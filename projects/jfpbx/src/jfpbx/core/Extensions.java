package jfpbx.core;

import jfpbx.db.RouteRow;
import jfpbx.db.Database;
import jfpbx.db.ExtensionRow;
import java.util.*;

import javaforce.*;


/** Low-level plugin for handling INVITEs to extensions. */

public class Extensions implements Plugin, DialChain {
  public final static int pid = 5;  //priority
  private PBXAPI api;
//interface Plugin
  public void init(PBXAPI api) {
    this.api = api;
    api.hookDialChain(this);
    JFLog.log("Extensions plugin init");
  }
  public void uninit(PBXAPI api) {
    api.unhookDialChain(this);
    JFLog.log("Extensions plugin uninit");
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
    JFLog.log("Extensions:onInvite:" + cd.dialed);
    if (cd.invited) {
      //reINVITE
      api.connect(cd);
      if (src) {
        cd.pbxdst.branch = cd.src.branch;
        cd.pbxsrc.branch = cd.src.branch;
      } else {
        cd.pbxdst.branch = cd.dst.branch;
        cd.pbxsrc.branch = cd.dst.branch;
      }
      cd.sip.buildsdp(cd, src ? cd.pbxdst : cd.pbxsrc);
      api.issue(cd, null, true, !src);
      return pid;
    }
    if (!src) return -1;
    if (!cd.authorized) {
      if (!apply_inbound_routes(cd)) return -1;
    }
    //dial inbound to an extension
    ExtensionRow ext = Database.getExtension(cd.dialed);
    if (ext == null) {
      JFLog.log("Extensions:dialed is not an extension");
      return -1;
    }  //an extension is not being dialed
    if (cd.user.equals(cd.dialed)) {  //did someone call themself?
      JFLog.log("Extensions:extension dialed self");
      return -1;  //voicemail will handle this call
    }
    if (!api.isRegistered(cd.dialed)) {
      JFLog.log("Extensions:extension not registered");
      return -1;  //phone is not online
    }
    Extension x = api.getExtension(cd.dialed);
    if (x == null) {
      JFLog.log("Extensions:exension not found");
      return -1;  //phone is not online
    }
    cd.pbxdst.contact = cd.src.contact;
    cd.pbxdst.branch = cd.src.branch;
    cd.pbxdst.to = cd.src.to.clone();
    cd.pbxdst.from = cd.src.from.clone();
    api.reply(cd, 100, "TRYING", null, false, true);
    cd.pbxdst.cseq = 1;
    cd.pbxdst.host = x.remoteip;
    cd.pbxdst.port = x.remoteport;
    cd.uri = "sip:" + cd.dialed + "@" + cd.pbxdst.host + ":" + cd.pbxdst.port;
    api.connect(cd);
    cd.sip.buildsdp(cd, cd.pbxdst);
    api.issue(cd, null, true, false);
    cd.invited = true;
    setTimer(cd, 15000);
    api.log(cd, "DEBUG:INVITE:" + cd.dialed + ":dst=" + cd.pbxdst.host + ":" + cd.pbxdst.port);
    return pid;
  }
  public void onRinging(CallDetailsPBX cd, boolean src) {
    if (src) return;
    api.reply(cd, 180, "RINGING", null, false, true);
  }
  public void onSuccess(CallDetailsPBX cd, boolean src) {
    if (!cd.cmd.equals("INVITE")) return;
    synchronized(cd.lock) {
      if (cd.cancelled) return;
      cd.connected = true;
    }
    clearTimer(cd);
    //send ack
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
    api.reply(cd, 200, "OK", null, true, !src);  //send 200 (NOTE : ACK is ignored)
  }
  public void onCancel(CallDetailsPBX cd, boolean src) {
    if (!src) return;
    synchronized(cd.lock) {
      if (cd.connected) return;  //TODO : should return error already connected
      cd.cancelled = true;
    }
    api.reply(cd, 200, "OK", null, false, true);
    api.issue(cd, null, false, false);
    api.disconnect(cd);
  }
  public void onError(CallDetailsPBX cd, int code, boolean src) {
    api.reply(cd, code, "RELAY", null, false, !src);
    cd.cmd = "ACK";
    api.issue(cd, null, false, src);
  }
  public void onTrying(CallDetailsPBX cd, boolean src) {
  }
  public void onBye(CallDetailsPBX cd, boolean src) {
    if (src) {
      //NOTE:to/from have been swapped
      cd.pbxdst.to = cd.src.to.clone();
      cd.pbxdst.from = cd.src.from.clone();
      cd.pbxdst.cseq++;
      api.issue(cd, null, false, false);
      cd.pbxsrc.to = cd.src.to.clone();
      cd.pbxsrc.from = cd.src.from.clone();
      cd.pbxsrc.cseq++;
      api.reply(cd, 200, "OK", null, false, true);
    } else {
      //NOTE:to/from have been swapped
      cd.pbxsrc.to = cd.dst.to.clone();
      cd.pbxsrc.from = cd.dst.from.clone();
      cd.pbxsrc.cseq++;
      api.issue(cd, null, false, true);
      cd.pbxdst.to = cd.dst.to.clone();
      cd.pbxdst.from = cd.dst.from.clone();
      cd.pbxdst.cseq++;
      api.reply(cd, 200, "OK", null, false, false);
    }
    api.disconnect(cd);
  }
  public void onFeature(CallDetailsPBX cd, String cmd, String cmddata, boolean src) {
  }
  private void setTimer(CallDetailsPBX cd, int timeout) {
    api.log(cd, "setting timer for call");
    cd.timer = new Timer();
    cd.timer.schedule(new TimerTask() {
      private CallDetailsPBX cd;
      public void run() {
        synchronized (cd.lock) {
          cd.timer = null;
          if (cd.connected) return;
      	  api.log(cd, "Trying to do voicemail for ext=" + cd.dialed);
          //send CANCEL to dst
          cd.cmd = "CANCEL";
          api.issue(cd, null, false, false);
          //check if ext has voicemail?
          ExtensionRow ext = Database.getExtension(cd.dialed);
          if ((ext != null) && (ext.voicemail)) {
            //transfer call to voicemail after timeout
            cd.invited = false;
            cd.pid = VoiceMail.pid;
            api.onInvite(cd, true, cd.pid);
          } else {
            //reply 486 (user is busy)
            cd.cmd = "INVITE";
            api.reply(cd, 486, "NO ONE HERE", null, false, true);
          }
        }
      }
      public TimerTask init(CallDetailsPBX cd) {
        this.cd = cd;
        return this;
      }
    }.init(cd), timeout);  //give ext timeout millisec to respond
  }
  private void clearTimer(CallDetailsPBX cd) {
    synchronized (cd.lock) {
      if (cd.timer == null) return;
      cd.timer.cancel();
      cd.timer = null;
    }
  }
  private boolean apply_inbound_routes(CallDetailsPBX cd) {
    RouteRow[] routes = Database.getInRoutes();
    //cd.user = cid
    //cd.dialed = did
    for(int route=0;route<routes.length;route++) {
      if (!routes[route].cid.equals(cd.user)) continue;  //no cid match
      if (!routes[route].did.equals(cd.dialed)) continue;  //no did match
      cd.dialed = routes[route].dest;  //set new destination
      return true;
    }
    String anon = Database.getConfig("anonymous");
    String route = Database.getConfig("route");
    cd.anon = anon.equals("true");
    cd.route = route.equals("true");
    return cd.anon;  //FALSE = no anonymous inbound calls
  }
}
