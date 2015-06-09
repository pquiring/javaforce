package com.jphonelite;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import android.net.*;
import android.database.*;
import android.provider.*;
import android.provider.Contacts.*;
import android.content.*;

import java.util.*;

import javaforce.*;
import javaforce.voip.*;

/**
 * jPhoneLite for Android : Engine
 *
 * author : Peter Quiring (pquiring at gmail dot com)
 *
 * website : jphonelite.sourceforge.net
 */

public class Engine implements SIPClientInterface, RTPInterface {
  private static Engine instance = null;
  private java.util.Timer timerKeepAlive, timerRegisterExpires, timerRegisterRetries;
  private int registerRetries;
  private int localport = 5061;
  private String lastDial;
  private Main main;
  private Context ctx;
  private static final String TAG = "JPLENGINE";
  private String lastDialed = "";

  public int line = -1;
  public PhoneLine lines[];
  public Sound sound = new Sound();
  public static boolean active = false;

  private Engine() {}

  public synchronized static Engine getInstance(Main main, Context ctx) {
    if (instance == null) {
//Log.i(TAG, "Engine.getInstance():create new");
      instance = new Engine();
      instance.main = main;
      instance.ctx = ctx;
      instance.init();
      return instance;
    }
//Log.i(TAG, "Engine.getInstance():return old");
    instance.main = main;
    instance.ctx = ctx;
    if (!active) instance.reinit();
    for(int a=0;a<6;a++) instance.lines[a].clr = -1;  //force color update
    return instance;
  }

  public void release() {
    main = null;
    instance = null;
  }

  private void init() {
    active = true;
//    Log.i(TAG, "Engine.init()");
    JFLog.init("/sdcard/.jphone.log", true);
    lines = new PhoneLine[6];
    for(int a=0;a<6;a++) lines[a] = new PhoneLine();
    Settings.loadSettings();
    sound.init(lines, ctx);
    reRegisterAll();
    keepAliveinit();
  }

  public void uninit() {
    active = false;
    unRegisterAll();
    sound.uninit();
    PhoneLine newLines[] = new PhoneLine[6];
    for(int a=0;a<6;a++) newLines[a] = new PhoneLine();
    lines = newLines;
  }

  public void reinit() {
    active = true;
    reRegisterAll();
    sound.init(lines, ctx);
  }

  public void do_xfr() {
    if (line == -1) return;
    if (!lines[line].incall) return;
    if (lines[line].xfr) {
      if (lines[line].dial.length() == 0) {
        //cancel xfer
        lines[line].status = "Connected";
        lines[line].xfr = false;
      } else {
        lines[line].sip.refer(lines[line].callid, lines[line].dial);
        lines[line].status = "XFER to " + lines[line].dial;
        lines[line].dial = "";
        lines[line].xfr = false;
        endLine(line);
      }
    } else {
      lines[line].dial = "";
      lines[line].status = "XFER : Enter dest and press XFER again";
      lines[line].xfr = true;
    }
  }

  public void do_hld() {
    if (line == -1) return;
    if (!lines[line].incall) return;
    if (lines[line].sip.isHold(lines[line].callid)) return;  //can't put on hold if you are on hold from other side
    if (lines[line].hld) {
      lines[line].sip.reinvite(lines[line].callid, lines[line].rtp.getlocalrtpport(), lines[line].codecs);
      lines[line].hld = false;
    } else {
      lines[line].sip.hold(lines[line].callid, lines[line].rtp.getlocalrtpport());
      lines[line].hld = true;
    }
  }

  public void do_dnd() {
    if (line == -1) return;
    if (lines[line].incall) return;
    if (lines[line].dnd)
      lines[line].dial = Settings.current.dndCodeOn;
    else
      lines[line].dial = Settings.current.dndCodeOff;
    lines[line].dnd = !lines[line].dnd;
  }

  public void do_cnf() {
    if (line == -1) return;
    if (!lines[line].incall) return;
    lines[line].cnf = !lines[line].cnf;
 }

  public void do_call() {
    if (line == -1) return;
    if (lines[line].incall) end(); else call();
  }

  public void addDigit(char digit) {
    if (line == -1) return;
    if (lines[line].sip == null) return;
    if (!lines[line].sip.isRegistered()) return;
    if (lines[line].incoming) return;
    if (digit == 'x') {
      if ((lines[line].incall)&&(!lines[line].xfr)) return;
      //delete digit
      int len = lines[line].dial.length();
      if (len > 0) lines[line].dial = lines[line].dial.substring(0, len-1);
    } else {
      if ((lines[line].incall)&&(!lines[line].xfr)) {
        if (lines[line].dtmf == 'x') {
//          Log.i(TAG, "DTMF:" + digit);
          lines[line].dtmfcnt = 7;  //7 * 20ms = 140ms total
          lines[line].dtmf = digit;
        }
        return;
      }
      lines[line].dial += digit;
    }
  }

  public void selectLine(int newline) {
    //make sure line is valid
    if ((line != -1) && (lines[line].dtmf != 'x')) lines[line].dtmfend = true;  //finish dtmf on current line
    line = newline;
    if (line == -1) return;
    sound.selectLine(line);
    if (main != null) main._updateScreen();
  }

  /** Registers all SIP connections. */

  public void reRegisterAll() {
    int idx;
    String host;
    int port;
    for(int a=0;a<6;a++) {
      lines[a].dial = "";
      lines[a].status = "";
      if ((a > 0) && (Settings.current.lines[a].same != -1)) continue;
      if (Settings.current.lines[a].host.length() == 0) continue;
      if (Settings.current.lines[a].user.length() == 0) continue;
      if (Settings.current.lines[a].pass.length() == 0) continue;
      lines[a].sip = new SIPClient();
      idx = Settings.current.lines[a].host.indexOf(':');
      if (idx == -1) {
        host = Settings.current.lines[a].host;
        port = 5060;
      } else {
        host = Settings.current.lines[a].host.substring(0,idx);
        port = JF.atoi(Settings.current.lines[a].host.substring(idx+1));
      }
//      Log.i(TAG, "registering:" + host + ":" + port);
      try {
        int b=0;
        while (!lines[a].sip.init(host, port, localport++, this)) {b++; if (b==10) throw new Exception("err");}
        lines[a].sip.register(Settings.current.lines[a].user, Settings.current.lines[a].auth, Settings.getPassword(Settings.current.lines[a].pass));
      } catch (Exception e) {
        lines[a].status = "SIP init failed (Exception)";
        lines[a].sip = null;
        if (a == line) if (main != null) main._updateScreen();
      }
    }
    for(int a=1;a<6;a++) {
      if (Settings.current.lines[a].same != -1) {
        if (Settings.current.lines[Settings.current.lines[a].same].same != -1) {
          lines[a].status = "Line#" + (a+1) + " (Invalid)";
        } else {
          lines[a].sip = lines[Settings.current.lines[a].same].sip;
        }
      }
    }
    //setup reRegister timer (expires)
    timerRegisterExpires = new java.util.Timer();
    timerRegisterExpires.scheduleAtFixedRate(new ReRegisterExpires(), 3595*1000, 3595*1000);  //do it 5 seconds early just to be sure
    registerRetries = 0;
    timerRegisterRetries = new java.util.Timer();
    timerRegisterRetries.schedule(new ReRegisterRetries(), 1000);
    if (main != null) main._updateScreen();
  }

  /** Expires registration with all SIP connections. */

  public void unRegisterAll() {
    if (timerRegisterExpires != null) {
      timerRegisterExpires.cancel();
      timerRegisterExpires = null;
    }
    for(int a=0;a<6;a++) {
      if (lines[a].incall) {
        selectLine(a);
        end();
      }
      lines[a].dial = "";
      lines[a].status = "";
      lines[a].unauth = false;
      if ((a > 0) && (Settings.current.lines[a].same != -1)) {
        lines[a].sip = null;
        continue;
      }
      if (lines[a].sip == null) continue;
      if (lines[a].sip.isRegistered()) {
        try {
          lines[a].sip.unregister();
        } catch (Exception e) {
        }
      }
    }
    int maxwait;
    for(int a=0;a<6;a++) {
      if (lines[a].sip == null) continue;
      maxwait = 1000;
      while (lines[a].sip.isRegistered()) { JF.sleep(10); maxwait -= 10; if (maxwait == 0) break; }
      lines[a].sip.uninit();
      lines[a].sip = null;
    }
  }

  /** Creates a timer to send keep-alives on all SIP connections.  Keep alive are done every 30 seconds (many routers have a 60 second timeout). */

  public void keepAliveinit() {
    timerKeepAlive = new java.util.Timer();
    timerKeepAlive.scheduleAtFixedRate(new KeepAlive(),0,30*1000);
  }

  /** TimerTask to perform keep-alives. on all SIP connections. */

  public class KeepAlive extends java.util.TimerTask {
    public void run() {
//Log.i(TAG,"KeepAlive start:" + System.currentTimeMillis());
      for(int a=0;a<6;a++) {
        if (Settings.current.lines[a].same != -1) continue;
        if (lines[a].sip == null) continue;
        if (!lines[a].sip.isRegistered()) continue;
        lines[a].sip.keepalive();
      }
//Log.i(TAG,"KeepAlive  stop:" + System.currentTimeMillis());
    }
  }

  /** TimerTask that reregisters all SIP connection after they expire (every 3600 seconds). */

  public class ReRegisterExpires extends java.util.TimerTask {
    public void run() {
//Log.i(TAG,"Expires start:" + System.currentTimeMillis());
      for(int a=0;a<6;a++) {
        if (Settings.current.lines[a].same != -1) continue;
        if (lines[a].sip == null) continue;
        lines[a].sip.reregister();
      }
      registerRetries = 0;
      if (timerRegisterRetries != null) {
        timerRegisterRetries = new java.util.Timer();
        timerRegisterRetries.schedule(new ReRegisterRetries(), 1000);
      }
//Log.i(TAG,"Expires  stop:" + System.currentTimeMillis());
    }
  }

  /** TimerTask that reregisters any SIP connections that have failed to register (checks every 1 second upto 5 attempts). */

  public class ReRegisterRetries extends java.util.TimerTask {
    public void run() {
//Log.i(TAG,"ReRegister start:" + System.currentTimeMillis());
      boolean again = false;
      for(int a=0;a<6;a++) {
        if (Settings.current.lines[a].same != -1) continue;
        if (lines[a].sip == null) continue;
        if (lines[a].unauth) continue;
        if (!lines[a].sip.isRegistered()) {
          lines[a].sip.reregister();
          again = true;
        }
      }
      registerRetries++;
      if ((again) && (registerRetries < 5)) {
        timerRegisterRetries = new java.util.Timer();
        timerRegisterRetries.schedule(new ReRegisterRetries(), 1000);
      } else {
        for(int a=0;a<6;a++) {
          if (Settings.current.lines[a].same != -1) continue;
          if (lines[a].sip == null) continue;
          if (lines[a].unauth) continue;
          if (!lines[a].sip.isRegistered()) {
            lines[a].unauth = true;  //server not responding after 5 attempts to register
          }
        }
        timerRegisterRetries = null;
      }
//Log.i(TAG,"ReRegister  stop:" + System.currentTimeMillis());
    }
  }

  public void call() {
    if (line == -1) return;
    if (lines[line].sip == null) return;
    if (!lines[line].sip.isRegistered()) return;
    if (lines[line].incall) return;  //already in call
    if (lines[line].dial.length() == 0) return;
    if (lines[line].incoming) {
      callAccept();
    } else {
      callInvite();
    }
    if (Settings.current.ac) {
      if (!lines[line].cnf) do_cnf();
    }
  }

  public void redial() {
    if (line == -1) return;
    lines[line].dial = lastDialed;
  }

  public void resetStatus(int forLine) {
    if ((lines[forLine].sip != null) && (!lines[forLine].unauth)) lines[forLine].status = "Line#" + (forLine+1) + " (" + lines[forLine].sip.getUser() + ")";
    if (main != null) main._updateScreen();
  }

  public void clearDial() {
    if (line == -1) return;
    lines[line].dial = "";
    if (!lines[line].incall) {
      resetStatus(line);
    }
    if (main != null) main.updateScreen();  //UI thread
  }

  /** Terminates a call. (may not be UI thread) */

  public void end() {
    if (line == -1) return;
    if (lines[line].incoming) {
      lines[line].sip.deny(lines[line].callid, "IGNORE", 480);
      lines[line].incoming = false;
      lines[line].ringing = false;
      lines[line].dial = "";
      lines[line].status = "Hungup";
      if (main != null) main._updateScreen();
      return;
    }
    lines[line].dial = "";
    if (!lines[line].incall) {
      //no call (update status)
      resetStatus(line);
      if (main != null) main._updateScreen();
      return;
    }
    if (lines[line].talking)
      lines[line].sip.bye(lines[line].callid);
    else
      lines[line].sip.cancel(lines[line].callid);
    endLine(line);
    if (main != null) main._updateScreen();
  }

  /** Cleanup after a call is terminated (call terminated local or remote). (may not be UI thread) */

  public void endLine(int forLine) {
    lines[forLine].dial = "";
    lines[forLine].orgdial = "";
    lines[forLine].status = "Hungup";
    lines[forLine].trying = false;
    lines[forLine].ringing = false;
    lines[forLine].incoming = false;
    lines[forLine].cnf = false;
    lines[forLine].xfr = false;
    lines[forLine].incall = false;
    lines[forLine].talking = false;
    lines[forLine].rtp.stop();
    lines[forLine].rtp = null;
    lines[forLine].callid = "";
    if (Settings.current.usePublish) lines[forLine].sip.publish("open");
    if (main != null) main._updateScreen();
  }

  /** Starts a outbound call. (may!UI thread) */

  public void callInvite() {
    lastDialed = lines[line].dial;
    lines[line].to = lines[line].dial;
    lines[line].rtp = new RTP();
    lines[line].rtp.init(this);
    lines[line].incall = true;
    lines[line].trying = false;
    lines[line].ringing = false;
    lines[line].talking = false;
    lines[line].incoming = false;
    lines[line].status = "Dialing";
    lastDial = lines[line].dial;
    if (Settings.current.use_g729a) {
      lines[line].callid = lines[line].sip.invite(lines[line].dial, lines[line].rtp.getlocalrtpport(), -1, new Codec[] {RTP.CODEC_G729a, RTP.CODEC_G711u});
    } else {
      lines[line].callid = lines[line].sip.invite(lines[line].dial, lines[line].rtp.getlocalrtpport(), -1, new Codec[] {RTP.CODEC_G711u});
    }
    lines[line].orgdial = lines[line].dial;
    if (Settings.current.usePublish) lines[line].sip.publish("busy");
    if (main != null) main._updateScreen();
  }

  /** Accepts an inbound call. (may!UI thread) */

  public void callAccept() {
    if (!Settings.current.use_g729a) {
      if (SIP.hasCodec(lines[line].codecs, RTP.CODEC_G729a)) lines[line].codecs = SIP.delCodec(lines[line].codecs, RTP.CODEC_G729a);
    }
    lines[line].to = lines[line].dial;
    lines[line].rtp = new RTP();
    lines[line].rtp.init(this);
    lines[line].sip.accept(lines[line].callid, lines[line].rtp.getlocalrtpport(), -1, lines[line].codecs);
    sound.flush();
    lines[line].rtp.start(lines[line].remotertphost, lines[line].remotertpport, lines[line].codecs, false);
    lines[line].incall = true;
    lines[line].ringing = false;
    lines[line].incoming = false;
    lines[line].talking = true;
    lines[line].status = "Connected";
    if (main != null) main._updateScreen();
  }

  /** Triggered when an outbound call (INVITE) was accepted. (!UI thread) */

  public void callInviteSuccess(int forLine, String remotertphost, int remotertpport, Codec codecs[]) {
    int codec;
    sound.flush();
    lines[forLine].codecs = codecs;
    lines[forLine].rtp.start(remotertphost, remotertpport, codecs, false);
    if (Settings.current.aa) selectLine(forLine);
  }

  public void registered(SIPClient sip) {
    for(int a=0;a<6;a++) {
      if (lines[a].sip != sip) continue;
      if (lines[a].status.length() == 0) resetStatus(a);
      if (line == -1) {
        selectLine(a);
      }
    }
    sip.subscribe(sip.getUser(), "message-summary", 3600);  //SUBSCRIBE to self for message-summary event (not needed with Asterisk but X-Lite does it)
    if (Settings.current.usePublish) lines[line].sip.publish("open");
  }

  public void unauthorized(SIPClient sip) {
    for(int a=0;a<6;a++) {
      if (lines[a].sip == sip) {
        lines[a].status = "Unauthorized";
        lines[a].unauth = true;
        if (line == a) selectLine(-1);
      }
    }
  }

//SIPClientInterface interface

  /** SIPClientInterface : onRegister() : triggered when a SIPClient has successfully or failed to register with server. */

  public void onRegister(SIPClient sip, boolean status) {
    if (status) registered(sip); else unauthorized(sip);
  }

  /** SIPClientInterface : onTrying() : triggered when an INVITE returns status code 100 (TRYING). */

  public void onTrying(SIPClient sip, String callid) {
    //is a line trying to do an invite
    for(int a=0;a<6;a++) {
      if ((lines[a].incall)&&(!lines[a].trying)) {
        if (lines[a].callid.equals(callid)) {
          lines[a].trying = true;
          lines[a].status = "Trying";
          if (line == a) if (main != null) main._updateScreen();
        }
      }
    }
  }

  /** SIPClientInterface : onRinging() : triggered when an INVITE returns status code 180/183 (RINGING). */

  public void onRinging(SIPClient sip, String callid) {
    //is a line trying to do an invite
    for(int a=0;a<6;a++) {
      if ((lines[a].incall)&&(!lines[a].ringing)) {
        if (lines[a].callid.equals(callid)) {
          lines[a].ringing = true;
          lines[a].status = "Ringing";
          if (line == a) if (main != null) main._updateScreen();
        }
      }
    }
  }

  /** SIPClientInterface : onSuccess() : triggered when an INVITE returns status code 200 (OK). */

  public void onSuccess(SIPClient sip, String callid, String remotertphost, int remotertpport, int remoteVrtpport, Codec codecs[]) {
    if (remotertphost == null) return;
    //is a line trying to do an invite or hold
    for(int a=0;a<6;a++) {
      if (!lines[a].incall) continue;
      if (!lines[a].callid.equals(callid)) continue;
      if (!lines[a].talking) {
        if (SIP.hasCodec(codecs, RTP.CODEC_G711u) && SIP.hasCodec(codecs, RTP.CODEC_G729a)) {
          //try to reinvite with one codec
          lines[a].sip.reinvite(callid, lines[a].rtp.getlocalrtpport(), SIP.delCodec(codecs, RTP.CODEC_G729a));
          return;
        }
        lines[a].status = "Connected";
        if (line == a) if (main != null) main._updateScreen();
        callInviteSuccess(a, remotertphost, remotertpport, codecs);
        lines[a].talking = true;
        lines[a].ringing = false;
        lines[a].codecs = codecs;
        return;
      }
      if (lines[a].hld) {
        lines[a].rtp.hold(true);
      } else {
        lines[a].rtp.hold(false);
      }
      return;
    }
  }

  /** SIPClientInterface : onBye() : triggered when server terminates a call. */

  public void onBye(SIPClient sip, String callid) {
    for(int a=0;a<6;a++) {
      if (lines[a].incall) {
        if (lines[a].callid.equals(callid)) {
          if (main != null) {
            endLine(a);
            if ((main != null) && (!main.active)) main.notify("Idle", false);
          }
        }
      }
    }
  }

  /** SIPClientInterface : onInvite() : triggered when server send an INVITE to jphonelite. */

  public int onInvite(SIPClient sip, String callid, String fromid, String fromnumber, String remotertphost, int remotertpport, int remoteVrtpprt, Codec codecs[]) {
    for(int a=0;a<6;a++) {
      if (lines[a].sip == sip) {
        if (lines[a].sip.getUser().equals(fromnumber)) {
          return 486;  //reply BUSY - attempt to call self (should goto voicemail now)
        }
        if (lines[a].incall) {
          if (lines[a].callid.equals(callid)) {
            //reINVITEd (usually to change RTP host/port) (codec should not change since we only accept with 1 codec)
            lines[a].remotertphost = remotertphost;
            lines[a].remotertpport = remotertpport;
            lines[a].rtp.change(remotertphost, remotertpport);
            return 200;
          }
          continue;
        }
        lines[a].dial = fromnumber;
        lines[a].callerid = fromid;
        if ((lines[a].callerid == null) || (lines[a].callerid.trim().length() == 0)) lines[a].callerid = "Unknown";
        lines[a].status = fromid + " is calling";
        lines[a].incoming = true;
        lines[a].remotertphost = remotertphost;
        lines[a].remotertpport = remotertpport;
        lines[a].callid = callid;
        lines[a].ringing = true;
        lines[a].codecs = codecs;
        if (Settings.current.aa) {
          selectLine(a);
          if (main != null) main._updateScreen();
          call();  //this will send a reply
          return -1;  //do NOT send a reply
        } else {
          if (line == a) if (main != null) main._updateScreen();
        }
        if (main != null) {
          if ((main != null) && (!main.active)) main.notify("" + fromnumber + " is calling.", true);
        }
        return 180;  //reply RINGING
      }
    }
    return 486;  //reply BUSY
  }

  /** SIPClientInterface : onCancel() : triggered when server send a CANCEL request after an INVITE. */

  public void onCancel(SIPClient sip, String callid, int code) {
    for(int a=0;a<6;a++) {
      if (lines[a].callid.equals(callid)) {
        lines[line].incall = false;
        lines[line].trying = false;
        if (lines[line].rtp != null) {
          lines[line].rtp.uninit();
          lines[line].rtp = null;
        }
        lines[line].callid = "";
        lines[a].dial = "";
        lines[a].status = "Hungup (" + code + ")";
        lines[a].ringing = false;
        lines[a].incoming = false;
        if (line == a) if (main != null) main._updateScreen();
      }
    }
  }


  /** SIPClientInterface : onRefer() : triggered when the server signals a successful transfer (REFER). */

  public void onRefer(SIPClient sip, String callid) {
    for(int a=0;a<6;a++) {
      if (lines[a].callid.equals(callid)) {
        if (line != a) selectLine(a);
        end();
      }
    }
  }

  /** SIPClientInterface : onNotify() : processes SIP:NOTIFY messages. */

  public void onNotify(SIPClient sip, String event, String content) {
    String contentLines[] = content.split("\r\n");
    if (event.equals("message-summary")) {
      String msgwait = SIP.getHeader("Messages-Waiting:", contentLines);
      if (msgwait != null) {
        for(int a=0;a<6;a++) {
          if (lines[a].sip == sip) {
            lines[a].msgwaiting = msgwait.equalsIgnoreCase("yes");
          }
        }
      }
      return;
    }
/*  //not supported in android release
    if (event.equals("presence")) {
      if (!content.startsWith("<?xml")) return;
      content = content.replaceAll("\r", "").replaceAll("\n", "");
      XML xml = new XML();  //against my better judgement I'm going to use my own XML (un)marshaller to process the data
      ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes());
      if (!xml.read(bais)) return;
      XML.XMLTag contact = xml.getTag(new String[] { "presence", "tuple", "contact" });
      if (contact == null) return;
      String fields[] = SIP.split("Unknown<" + contact.getContent() + ">");
      if (fields == null) return;
      XML.XMLTag status = xml.getTag(new String[] { "presence", "tuple", "status", "basic" });
      if (status == null) return;
      setStatus(fields[1], fields[2], status.getContent().trim());
      return;
    }
*/
  }

  /** SIPClientInterface : getResponse() : not used in jPhoneLite */

  public String getResponse(SIPClient client, String realm, String cmd, String uri, String nonce, String qop, String nc, String cnonce) {
    return null;  //not used
  }

//interface RTPInterface (not used in android release)
  public void rtpDigit(RTP rtp, char digit) {}
  public void rtpSamples(RTP rtp) {}
  public void rtpPacket(RTP rtp, boolean rtcp, byte data[],int off,int len) {}
  public void rtpH263(RTP rtp, byte data[],int off,int len) {}
  public void rtpH264(RTP rtp, byte data[],int off,int len) {}
  public void rtpFLV(RTP rtp,byte data[],int off,int len) {}
  public void rtpJPEG(RTP rtp,byte data[],int off,int len) {}
}
