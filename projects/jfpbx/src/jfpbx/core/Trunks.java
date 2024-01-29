package jfpbx.core;

import jfpbx.db.TrunkRow;
import jfpbx.db.ExtensionRow;
import jfpbx.db.Database;
import java.util.*;

import javaforce.*;
import javaforce.voip.*;


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
  public int onInvite(CallDetailsPBX cd, boolean src) {
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
      JFLog.log("Trunks:call from unregistered source");
      if (cd.route) route_call(cd);  //TODO : route call from one trunk to another ???
      return -1;  //??? pid ???
    }
    if ((cd.user == null) || (cd.user.length() == 0)) {
      JFLog.log("Trunks:invalid user");
      return -1;
    }
    ExtensionRow ext = Database.getExtension(cd.user);
    if (ext == null) {
      //an extension is not dialing
      JFLog.log("Trunks:Call not from extension");
      return -1;
    }
    if (!api.isRegistered(cd.user)) {
      JFLog.log("Trunks:call from unregistered extension");
      return -1;
    }
    if (cd.user.equals(cd.dialed)) {
      //voicemail may intercept next
      JFLog.log("Trunks:extension called self");
      return -1;
    }
    Extension x = api.getExtension(cd.user);
    if (x == null) {
      //unauth trunk access
      JFLog.log("Trunks:extension not found");
      return -1;
    }
    cd.pbxdst.to = cd.src.to.clone();
    cd.pbxdst.from = cd.src.from.clone();
    cd.pbxdst.contact = cd.src.contact;  //BUG : should this be changed to DID from trunk register string ???
    cd.pbxdst.branch = cd.src.branch;
    api.connect(cd);
    Dial dial = new Dial();
    dial.number = cd.dialed;
    cd.trunks = api.getTrunks(dial, cd.user);
    if ((cd.trunks == null) || (cd.trunks.length == 0)) {
      //no routes on this trunk
      JFLog.log("Trunks:no routes apply to call dialed");
      return -1;
    }
    api.reply(cd, 100, "TRYING", null, false, true);
    cd.dialed = dial.number;  //apply new dialed after outroute pattern is applied
    cd.trunkidx = 0;
    cd.lastcode = -1;
    cd.invited = true;
    tryTrunk(cd);
    return pid;
  }
  public void onRinging(CallDetailsPBX cd, boolean src) {
    if (src) return;
    cd.trunkok = true;
    api.reply(cd, 180, "RINGING", null, false, true);
  }
  public void onSuccess(CallDetailsPBX cd, boolean src) {
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
  public void onCancel(CallDetailsPBX cd, boolean src) {
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
  public void onError(CallDetailsPBX cd, int code, boolean src) {
    if (!cd.connected) {
      cd.lastcode = code;
    }
    api.reply(cd, code, "RELAY", null, false, !src);
    cd.cmd = "ACK";
    api.issue(cd, null, false, src);
  }
  public void onTrying(CallDetailsPBX cd, boolean src) {
    if (!src) {
      cd.trunkdelay = 2;  //give trunk 10 more secs
    }
  }
  public void onBye(CallDetailsPBX cd, boolean src) {
    if (src) {
      api.log(cd, "Trunks:src terminated call with BYE");
      cd.pbxdst.cseq++;
      api.issue(cd, null, false, false);
      api.reply(cd, 200, "OK", null, false, true);
    } else {
      api.log(cd, "Trunks:dst terminated call with BYE");
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
  public void onFeature(CallDetailsPBX cd, String cmd, String cmddata, boolean src) {
  }
  public boolean onMessage(CallDetailsPBX cd, String from, String to, String[] msg, boolean src) {
    return false;
  }

  private void tryTrunk(CallDetailsPBX cd) {
    //try INVITE to cd.trunks[cd.trunkidx]
    TrunkRow trunk = cd.trunks[cd.trunkidx];
    api.log(cd, "Trunks:Trying trunk " + trunk);
    String host = trunk.host;
    int idx = host.indexOf(':');
    if (idx == -1) {
      cd.pbxdst.host = host;
      cd.pbxdst.port = 5060;
    } else {
      cd.pbxdst.host = host.substring(0, idx);
      cd.pbxdst.port = Integer.valueOf(host.substring(idx+1));
    }
    String rulesStr = trunk.outrules;
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
    String reg = trunk.register;
    idx = reg.indexOf('@');
    if (idx != -1) {
      reg = reg.substring(0, idx);
    }
    String user, pass;
    idx = reg.indexOf(':');
    if (idx != -1) {
      user = reg.substring(0, idx);
      pass = reg.substring(idx+1);
      cd.pbxdst.from[0] = "";
      cd.pbxdst.from[1] = user;
    }
    if (trunk.cid.length() > 0) {
      cd.pbxdst.from[0] = "";
      cd.pbxdst.from[1] = trunk.cid;
    }
    if (trunk.xip.length() > 0) {
      cd.pbxdst.from[2] = trunk.xip;
    }
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
              api.log(cd, "Trunks:call failed:error = " + cd.lastcode);
              api.reply(cd, cd.lastcode, "ERROR", null, false, true);
              return;
            }
            tryTrunk(cd);
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
  private void route_call(CallDetailsPBX cd) {
    //TODO : route call from one trunk to another
  }
}
