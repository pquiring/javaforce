package jpbx.plugins.core;

import java.util.*;
import javaforce.*;
import javaforce.voip.*;
import jpbx.core.*;

/** Low-level plugin for handling INVITEs from extensions to trunks. */

public class Trunks implements Plugin, DialChain {
  public final static int pid = 20;  //priority
  private PBXAPI api;
//interface Plugin
  public void init(PBXAPI api) {
    this.api = api;
    api.hookDialChain(this);
    JFLog.log("Trunks plugin init");
  }
  public void uninit(PBXAPI api) {
    api.unhookDialChain(this);
    JFLog.log("Trunks plugin uninit");
  }
  public void install(PBXAPI api) {
    //nothing to do
  }
  public void uninstall(PBXAPI api) {
    //nothing to do
  }
//interface DialChain
  public int getPriority() {return pid;}
  public int onInvite(CallDetailsPBX cd, SQL sql, boolean src) {
    if (cd.invited) {
      //reINVITE
      api.connect(cd);
      if (src) {
        cd.pbxdst.branch = cd.src.branch;
        cd.pbxsrc.branch = cd.src.branch;
      } else {
        cd.pbxsrc.branch = cd.dst.branch;
        cd.pbxsrc.branch = cd.dst.branch;
      }
      cd.sip.buildsdp(cd, src ? cd.pbxdst : cd.pbxsrc);
      api.issue(cd, null, true, !src);
      return pid;
    }
    //dial outbound from extension
    if (!cd.authorized) {
      if (cd.route) route_call(cd, sql);  //TODO : route call from one trunk to another ???
      return -1;  //??? pid ???
    }
    if ((cd.user == null) || (cd.user.length() == 0)) return -1;
    String ext = sql.select1value("SELECT ext FROM exts WHERE ext=" + sql.quote(cd.user));
    if (ext == null) return -1;  //an extension is not dialing
    if (!api.isRegistered(cd.user)) return -1;
    if (cd.user.equals(cd.dialed)) return -1;  //voicemail may intercept next
    Extension x = api.getExtension(cd.user);
    if (x == null) return -1;  //unauth trunk access
    cd.pbxdst.to = cd.src.to.clone();
    cd.pbxdst.from = cd.src.from.clone();
    cd.pbxdst.contact = cd.src.contact;  //BUG : should this be changed to DID from trunk register string ???
    cd.pbxdst.branch = cd.src.branch;
    api.connect(cd);
    cd.trunks = api.getTrunks(cd.dialed, cd.user, sql);
    if ((cd.trunks == null) || (cd.trunks.length < 2)) return -1;  //no routes on this trunk
    api.reply(cd, 100, "TRYING", null, false, true);
    cd.dialed = cd.trunks[0];  //apply new dialed after outroute pattern is applied
    cd.trunkidx = 1;
    cd.lastcode = -1;
    cd.invited = true;
    tryTrunk(cd, sql);
    return pid;
  }
  public void onRinging(CallDetailsPBX cd, SQL sql, boolean src) {
    if (src) return;
    cd.trunkok = true;
    api.reply(cd, 180, "RINGING", null, false, true);
  }
  public void onSuccess(CallDetailsPBX cd, SQL sql, boolean src) {
    if (!cd.cmd.equals("INVITE")) return;
    synchronized(cd.lock) {
      if (cd.cancelled) return;
      cd.trunkok = true;
      cd.connected = true;
    }
    clearTimer(cd);
    cd.cmd = "ACK";
    api.issue(cd, null, false, src);  //send ACK
    if (src) {
      cd.pbxdst.to = cd.src.to.clone();
      cd.pbxdst.from = cd.src.from.clone();
    } else {
      cd.pbxsrc.to = cd.dst.to.clone();
      cd.pbxsrc.from = cd.dst.from.clone();
    }
    api.connect(cd);
    cd.sip.buildsdp(cd, src ? cd.pbxdst : cd.pbxsrc);
    cd.cmd = "INVITE";
    api.reply(cd, 200, "OK", null, true, !src);  //send 200 (NOTE : ACK is ignored (already sent))
  }
  public void onCancel(CallDetailsPBX cd, SQL sql, boolean src) {
    //extension "CANCEL"ed call
    if (!src) return;
    synchronized(cd.lock) {
      if (cd.connected) return;  //TODO : should return error already connected
      cd.trunkok = true;  //stop tryTrunk()
      cd.cancelled = true;
    }
    api.reply(cd, 200, "OK", null, false, true);
    api.issue(cd, null, false, false);
    api.disconnect(cd);
  }
  public void onError(CallDetailsPBX cd, SQL sql, int code, boolean src) {
    if (!cd.connected) {
      cd.lastcode = code;
    }
    api.reply(cd, code, "RELAY", null, false, !src);
    cd.cmd = "ACK";
    api.issue(cd, null, false, src);
  }
  public void onTrying(CallDetailsPBX cd, SQL sql, boolean src) {
    if (!src) {
      cd.trunkdelay = 2;  //give trunk 10 more secs
    }
  }
  public void onBye(CallDetailsPBX cd, SQL sql, boolean src) {
    if (src) {
      api.log(cd, "TRUNK : src terminated call with BYE");
      cd.pbxdst.cseq++;
      api.issue(cd, null, false, false);
      api.reply(cd, 200, "OK", null, false, true);
    } else {
      api.log(cd, "TRUNK : dst terminated call with BYE");
      //NOTE:to/from have been swapped
      cd.pbxsrc.to = cd.dst.to.clone();
      cd.pbxsrc.from = cd.dst.from.clone();
      api.issue(cd, null, false, true);
      cd.pbxdst.cseq++;
      cd.pbxdst.to = cd.dst.to.clone();
      cd.pbxdst.from = cd.dst.from.clone();
      api.reply(cd, 200, "OK", null, false, false);
    }
    api.disconnect(cd);
  }
  public void onFeature(CallDetailsPBX cd, SQL sql, String cmd, String cmddata, boolean src) {
  }
  private void tryTrunk(CallDetailsPBX cd, SQL sql) {
    //try INVITE to cd.trunks[cd.trunkidx]
    String trunk = cd.trunks[cd.trunkidx];
    api.log(cd, "TRUNK : Trying trunk " + trunk);
    String host_rules[] = sql.select1row("SELECT host,outrules FROM trunks WHERE trunk=" + sql.quote(trunk));
    if ((host_rules == null) || (host_rules[0] == null)) return;  //ohoh
    String host = host_rules[0];
    int idx = host.indexOf(':');
    if (idx == -1) {
      cd.pbxdst.host = host;
      cd.pbxdst.port = 5060;
    } else {
      cd.pbxdst.host = host.substring(0, idx);
      cd.pbxdst.port = Integer.valueOf(host.substring(idx+1));
    }
    String rulesStr = host_rules[1];
    if (rulesStr != null) {
      cd.pbxdst.to[1] = null;
      String rules[] = rulesStr.split(":");
      for(int a=0;a<rules.length;a++) {
        cd.pbxdst.to[1] = api.patternMatches(rules[a], cd.dialed);
        if (cd.pbxdst.to[1] != null) break;
      }
      if (cd.pbxdst.to[1] == null) cd.pbxdst.to[1] = cd.dialed;
    } else {
      cd.pbxdst.to[1] = cd.dialed;
    }
    cd.pbxdst.to[0] = cd.pbxdst.to[1];
    cd.sip.buildsdp(cd, cd.pbxdst);
    cd.pbxdst.cseq = cd.src.cseq;
    cd.uri = "sip:" + cd.dialed + "@" + cd.pbxdst.host;
    cd.pbxdst.to[2] = cd.pbxdst.host;
    cd.cmd = "INVITE";
    cd.pbxdst.to = SIP.removetag(cd.pbxdst.to.clone());
    cd.pbxdst.from = cd.pbxdst.from.clone();
    cd.trunkok = false;
    cd.authsent = false;
    api.issue(cd, null, true, false);
    setTimer(cd);
  }
  private void setTimer(CallDetailsPBX cd) {
    cd.timer = new Timer();
    cd.timer.schedule(new TimerTask() {
      private CallDetailsPBX cd;
      public void run() {
      	synchronized (cd.lock) {
          if (cd.trunkok) return;
          if (cd.trunkdelay > 0) {
            cd.trunkdelay--;
            setTimer(cd);
          } else {
            //send CANCEL to last trunk
            if (cd.lastcode == -1) {
              cd.cmd = "CANCEL";
              api.issue(cd, null, false, false);
            }
            cd.trunkidx++;
            if ((cd.trunks.length == cd.trunkidx) || (cd.lastcode == 486)) {
              //reply last error (if any) to extension
              if (cd.lastcode == -1) cd.lastcode = 487;
              api.log(cd, "TRUNK : call failed : error = " + cd.lastcode);
              api.reply(cd, cd.lastcode, "ERROR", null, false, true);
              return;
            }
            SQL sql = new SQL();
            if (!sql.init()) return;  //ohoh
            tryTrunk(cd, sql);
            sql.uninit();
          }
        }
      }
      public TimerTask init(CallDetailsPBX cd) {
        this.cd = cd;
        return this;
      }
    }.init(cd), 5000);  //give trunk 5 sec to respond
  }
  private void clearTimer(CallDetailsPBX cd) {
    synchronized (cd.lock) {
      if (cd.timer == null) return;
      cd.timer.cancel();
      cd.timer = null;
    }
  }
  private void route_call(CallDetailsPBX cd, SQL sql) {
    //TODO : route call from one trunk to another
  }
}
