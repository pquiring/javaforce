package com.jphonelite;

/*
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import android.net.*;
import android.database.*;
import android.provider.*;
import android.provider.Contacts.*;
*/
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
  private String lastDialed = "";

  public int line = -1;
  public PhoneLine lines[];
  public Audio sound = new Audio();
  public static boolean active = false;

  private Engine() {}

  public synchronized static Engine getInstance(Main main, Context ctx) {
    if (instance == null) {
//JFLog.log("Engine.getInstance():create new");
      instance = new Engine();
      instance.main = main;
      instance.ctx = ctx;
      instance.init();
      return instance;
    }
//JFLog.log("Engine.getInstance():return old");
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
    JFLogAndroid.init(0, "JAVAFORCE", "/sdcard/.jphone.log");
//    JFLog.log("Engine.init()");
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
    PhoneLine pl = lines[line];
    if (!pl.incall) return;
    if (pl.xfr) {
      if (pl.dial.length() == 0) {
        //cancel xfer
        pl.status = "Connected";
        pl.xfr = false;
      } else {
        pl.sip.refer(pl.callid, pl.dial);
        pl.status = "XFER to " + pl.dial;
        pl.dial = "";
        pl.xfr = false;
        endLine(line);
      }
    } else {
      pl.dial = "";
      pl.status = "XFER : Enter dest and press XFER again";
      pl.xfr = true;
    }
  }

  public void do_hld() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (!pl.incall) return;
    pl.hld = !pl.hld;
    pl.sip.setHold(pl.callid, pl.hld);
    pl.sip.reinvite(pl.callid);
  }

  public void do_dnd() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.incall) return;
    if (pl.dnd)
      pl.dial = Settings.current.dndCodeOn;
    else
      pl.dial = Settings.current.dndCodeOff;
    pl.dnd = !pl.dnd;
  }

  public void do_cnf() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (!pl.incall) return;
    pl.cnf = !pl.cnf;
 }

  public void do_call() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.incall) end(); else call();
  }

  public void addDigit(char digit) {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.sip == null) return;
    if (!pl.sip.isRegistered()) return;
    if (pl.incoming) return;
    if (digit == 'x') {
      if ((pl.incall)&&(!pl.xfr)) return;
      //delete digit
      int len = pl.dial.length();
      if (len > 0) pl.dial = pl.dial.substring(0, len-1);
    } else {
      if ((pl.incall)&&(!pl.xfr)) {
        if (pl.dtmf == 'x') {
//          JFLog.log("DTMF:" + digit);
          pl.dtmfcnt = 7;  //7 * 20ms = 140ms total
          pl.dtmf = digit;
        }
        return;
      }
      pl.dial += digit;
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
//      JFLog.log("registering:" + host + ":" + port);
      try {
        int b=0;
        while (!lines[a].sip.init(host, port, localport++, this, SIP.Transport.UDP)) {b++; if (b==10) throw new Exception("err");}
        lines[a].sip.register(Settings.current.lines[a].name, Settings.current.lines[a].user, Settings.current.lines[a].auth, Settings.getPassword(Settings.current.lines[a].pass));
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
//JFLog.log("KeepAlive start:" + System.currentTimeMillis());
      for(int a=0;a<6;a++) {
        if (Settings.current.lines[a].same != -1) continue;
        if (lines[a].sip == null) continue;
        if (!lines[a].sip.isRegistered()) continue;
        lines[a].sip.keepalive();
      }
//JFLog.log("KeepAlive  stop:" + System.currentTimeMillis());
    }
  }

  /** TimerTask that reregisters all SIP connection after they expire (every 3600 seconds). */

  public class ReRegisterExpires extends java.util.TimerTask {
    public void run() {
//JFLog.log("Expires start:" + System.currentTimeMillis());
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
//JFLog.log("Expires  stop:" + System.currentTimeMillis());
    }
  }

  /** TimerTask that reregisters any SIP connections that have failed to register (checks every 1 second upto 5 attempts). */

  public class ReRegisterRetries extends java.util.TimerTask {
    public void run() {
//JFLog.log("ReRegister start:" + System.currentTimeMillis());
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
//JFLog.log("ReRegister  stop:" + System.currentTimeMillis());
    }
  }

  public void call() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.sip == null) return;
    if (!pl.sip.isRegistered()) return;
    if (pl.incall) return;  //already in call
    if (pl.dial.length() == 0) return;
    if (pl.incoming) {
      callAccept();
    } else {
      callInvite();
    }
    if (Settings.current.ac) {
      if (!pl.cnf) do_cnf();
    }
  }

  public void redial() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    pl.dial = lastDialed;
  }

  public void resetStatus(int forLine) {
    PhoneLine pl = lines[forLine];
    if ((pl.sip != null) && (!pl.unauth)) pl.status = "Line#" + (forLine+1) + " (" + pl.sip.getUser() + ")";
    if (main != null) main._updateScreen();
  }

  public void clearDial() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    pl.dial = "";
    if (!pl.incall) {
      resetStatus(line);
    }
    if (main != null) main.updateScreen();  //UI thread
  }

  /** Terminates a call. (may not be UI thread) */

  public void end() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.incoming) {
      pl.sip.deny(pl.callid, "IGNORE", 480);
      pl.incoming = false;
      pl.ringing = false;
      pl.dial = "";
      pl.status = "Hungup";
      if (main != null) main._updateScreen();
      return;
    }
    pl.dial = "";
    if (!pl.incall) {
      //no call (update status)
      resetStatus(line);
      if (main != null) main._updateScreen();
      return;
    }
    if (pl.talking)
      pl.sip.bye(pl.callid);
    else
      pl.sip.cancel(pl.callid);
    endLine(line);
    if (main != null) main._updateScreen();
  }

  /** Cleanup after a call is terminated (call terminated local or remote). (may not be UI thread) */

  public void endLine(int forLine) {
    PhoneLine pl = lines[forLine];
    pl.dial = "";
    pl.orgdial = "";
    pl.status = "Hungup";
    pl.trying = false;
    pl.ringing = false;
    pl.incoming = false;
    pl.cnf = false;
    pl.xfr = false;
    pl.incall = false;
    pl.talking = false;
    pl.audioRTP.stop();
    pl.audioRTP = null;
    pl.callid = "";
    pl.rtpStarted = false;
    if (Settings.current.usePublish) pl.sip.publish("open");
    if (main != null) main._updateScreen();
  }

  private byte[] genKey() {
    byte ret[] = new byte[16];
    new Random().nextBytes(ret);
    return ret;
  }

  private byte[] genSalt() {
    byte ret[] = new byte[14];
    new Random().nextBytes(ret);
    return ret;
  }

  private SDP getLocalSDPInvite(PhoneLine pl) {
    SDP sdp = new SDP();
    SDP.Stream stream = sdp.addStream(SDP.Type.audio);
    stream.content = "audio1";
    stream.port = pl.audioRTP.getlocalrtpport();
/*
    if (pl.srtp) {
      stream.profile = SDP.Profile.SAVP;
      if (!pl.dtls) {
        stream.keyExchange = SDP.KeyExchange.SDP;
        stream.addKey("AES_CM_128_HMAC_SHA1_80", genKey(), genSalt());
      } else {
        stream.keyExchange = SDP.KeyExchange.DTLS;
        stream.sdp.fingerprint = fingerprintSHA256;
        stream.sdp.iceufrag = RTP.genIceufrag();
        stream.sdp.icepwd = RTP.genIcepwd();
      }
    }
*/
    String enabledCodecs[] = Settings.current.getAudioCodecs();
    for(int a=0;a<enabledCodecs.length;a++) {
      if (enabledCodecs[a].equals(RTP.CODEC_G729a.name)) stream.addCodec(RTP.CODEC_G729a);
      if (enabledCodecs[a].equals(RTP.CODEC_G711u.name)) stream.addCodec(RTP.CODEC_G711u);
      if (enabledCodecs[a].equals(RTP.CODEC_G711a.name)) stream.addCodec(RTP.CODEC_G711a);
      if (enabledCodecs[a].equals(RTP.CODEC_G722.name)) stream.addCodec(RTP.CODEC_G722);
    }
/*
    if (!pl.disableVideo && Settings.current.nativeVideo) {
      stream = sdp.addStream(SDP.Type.video);
      stream.content = "video1";
      stream.port = pl.videoRTP.getlocalrtpport();
      if (pl.srtp) {
        stream.profile = SDP.Profile.SAVP;
        if (!pl.dtls) {
          stream.keyExchange = SDP.KeyExchange.SDP;
          stream.addKey("AES_CM_128_HMAC_SHA1_80", genKey(), genSalt());
        } else {
          stream.keyExchange = SDP.KeyExchange.DTLS;
          stream.sdp.fingerprint = fingerprintSHA256;
          stream.sdp.iceufrag = RTP.genIceufrag();
          stream.sdp.icepwd = RTP.genIcepwd();
        }
      }
      enabledCodecs = Settings.current.getVideoCodecs();
      for(int a=0;a<enabledCodecs.length;a++) {
        if (enabledCodecs[a].equals(RTP.CODEC_JPEG.name)) stream.addCodec(RTP.CODEC_JPEG);
        if (Settings.hasFFMPEG) {
          if (enabledCodecs[a].equals(RTP.CODEC_H263.name)) stream.addCodec(RTP.CODEC_H263);
          if (enabledCodecs[a].equals(RTP.CODEC_H263_1998.name)) stream.addCodec(RTP.CODEC_H263_1998);
          if (enabledCodecs[a].equals(RTP.CODEC_H263_2000.name)) stream.addCodec(RTP.CODEC_H263_2000);
          if (enabledCodecs[a].equals(RTP.CODEC_H264.name)) stream.addCodec(RTP.CODEC_H264);
          if (enabledCodecs[a].equals(RTP.CODEC_VP8.name)) stream.addCodec(RTP.CODEC_VP8);
        }
      }
    }
*/
    return sdp;
  }

  /** Returns the SDP Stream complementary mode (send <-> receive) */
  private SDP.Mode complementMode(SDP.Mode mode) {
    switch (mode) {
      case recvonly: return SDP.Mode.sendonly;
      case sendonly: return SDP.Mode.recvonly;
      case inactive:  //no break
      case sendrecv: return mode;
    }
    return null;
  }

/*
  private void addVideoStream(PhoneLine pl, SDP sdp, SDP.Stream vstream, Codec codec) {
    SDP.Stream newVstream = sdp.addStream(SDP.Type.video);
    newVstream.port = pl.videoRTP.getlocalrtpport();
    newVstream.mode = complementMode(vstream.mode);
    newVstream.addCodec(codec);
    if (pl.srtp) {
      newVstream.profile = SDP.Profile.SAVP;
      if (vstream.keyExchange == SDP.KeyExchange.SDP) {
        newVstream.keyExchange = SDP.KeyExchange.SDP;
        newVstream.addKey("AES_CM_128_HMAC_SHA1_80", genKey(), genSalt());
      } else {
        newVstream.keyExchange = SDP.KeyExchange.DTLS;
        newVstream.sdp.fingerprint = fingerprintSHA256;
        newVstream.sdp.iceufrag = RTP.genIceufrag();
        newVstream.sdp.icepwd = RTP.genIcepwd();
      }
    }
  }
*/

  /** Returns SDP that matches requested SDP. */

  private SDP getLocalSDPAccept(PhoneLine pl) {
    SDP sdp = new SDP();
    SDP.Stream astream = pl.sdp.getFirstAudioStream();
    SDP.Stream vstream = pl.sdp.getFirstVideoStream();
    SDP.Stream newAstream = sdp.addStream(SDP.Type.audio);
    newAstream.port = pl.audioRTP.getlocalrtpport();
    newAstream.mode = complementMode(astream.mode);
/*
    if (pl.srtp) {
      newAstream.profile = SDP.Profile.SAVP;
      if (astream.keyExchange == SDP.KeyExchange.SDP) {
        newAstream.keyExchange = SDP.KeyExchange.SDP;
        newAstream.addKey("AES_CM_128_HMAC_SHA1_80", genKey(), genSalt());
      } else {
        newAstream.keyExchange = SDP.KeyExchange.DTLS;
        newAstream.sdp.fingerprint = fingerprintSHA256;
        newAstream.sdp.iceufrag = RTP.genIceufrag();
        newAstream.sdp.icepwd = RTP.genIcepwd();
      }
    }
*/
    String enabledCodecs[] = Settings.current.getAudioCodecs();
    for(int a=0;a<enabledCodecs.length;a++) {
      if ((enabledCodecs[a].equals(RTP.CODEC_G729a.name)) && (astream.hasCodec(RTP.CODEC_G729a))) {
        newAstream.addCodec(RTP.CODEC_G729a);
        break;
      }
      if ((enabledCodecs[a].equals(RTP.CODEC_G711u.name)) && (astream.hasCodec(RTP.CODEC_G711u))) {
        newAstream.addCodec(RTP.CODEC_G711u);
        break;
      }
      if ((enabledCodecs[a].equals(RTP.CODEC_G711a.name)) && (astream.hasCodec(RTP.CODEC_G711a))) {
        newAstream.addCodec(RTP.CODEC_G711a);
        break;
      }
      if ((enabledCodecs[a].equals(RTP.CODEC_G722.name)) && (astream.hasCodec(RTP.CODEC_G722))) {
        newAstream.addCodec(RTP.CODEC_G722);
        break;
      }
    }
/*
    if (!pl.disableVideo && vstream != null) {
      enabledCodecs = Settings.current.getVideoCodecs();
      for(int a=0;a<enabledCodecs.length;a++) {
        if ((enabledCodecs[a].equals(RTP.CODEC_JPEG.name)) && (vstream.hasCodec(RTP.CODEC_JPEG))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_JPEG);
          break;
        }
        if ((enabledCodecs[a].equals(RTP.CODEC_H263.name)) && (vstream.hasCodec(RTP.CODEC_H263))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_H263);
          break;
        }
        if ((enabledCodecs[a].equals(RTP.CODEC_H263_1998.name)) && (vstream.hasCodec(RTP.CODEC_H263_1998))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_H263_1998);
          break;
        }
        if ((enabledCodecs[a].equals(RTP.CODEC_H263_2000.name)) && (vstream.hasCodec(RTP.CODEC_H263_2000))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_H263_2000);
          break;
        }
        if ((enabledCodecs[a].equals(RTP.CODEC_H264.name)) && (vstream.hasCodec(RTP.CODEC_H264))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_H264);
          break;
        }
        if ((enabledCodecs[a].equals(RTP.CODEC_VP8.name)) && (vstream.hasCodec(RTP.CODEC_VP8))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_VP8);
          break;
        }
      }
    }
*/
    return sdp;
  }

  /** Starts a outbound call. (may!UI thread) */

  public void callInvite() {
    PhoneLine pl = lines[line];
    lastDialed = pl.dial;
    pl.to = pl.dial;
    pl.audioRTP = new RTP();
    pl.audioRTP.init(this);
    pl.incall = true;
    pl.trying = false;
    pl.ringing = false;
    pl.talking = false;
    pl.incoming = false;
    pl.status = "Dialing";
    lastDial = pl.dial;
    pl.localsdp = getLocalSDPInvite(lines[line]);
    pl.callid = pl.sip.invite(pl.dial, pl.localsdp);
    pl.orgdial = pl.dial;
    if (Settings.current.usePublish) pl.sip.publish("busy");
    if (main != null) main._updateScreen();
  }

  /** Accepts an inbound call. (may!UI thread) */

  public void callAccept() {
    PhoneLine pl = lines[line];
    pl.to = pl.dial;
    pl.audioRTP = new RTP();
    pl.audioRTP.init(this);
    if (pl.sdp != null) {
      pl.localsdp = getLocalSDPAccept(pl);
      if (!startRTPinbound()) return;
    } else {
      //INVITE did not include SDP so start SDP negotiation on this side
      pl.localsdp = getLocalSDPInvite(pl);
    }
    pl.sip.accept(pl.callid, pl.localsdp);
    sound.flush();
    pl.incall = true;
    pl.ringing = false;
    pl.incoming = false;
    pl.talking = true;
    pl.status = "Connected";
    if (main != null) main._updateScreen();
  }

  /** Triggered when an outbound call (INVITE) was accepted. (!UI thread) */

  public void callInviteSuccess(int forLine, SDP sdp) {
    sound.flush();
    PhoneLine pl = lines[forLine];
    try {
      pl.sdp = sdp;
      if (!startRTPoutbound(forLine)) return;
      if (Settings.current.aa) selectLine(forLine);
    } catch (Exception e) {
      JFLog.log(e);
      pl.sip.bye(pl.callid);
      onCancel(pl.sip, pl.callid, 500);
    }
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

  /** Changes SDP/RTP details. */

  public void change(int xline, SDP sdp) {
    try {
      PhoneLine pl = lines[xline];
      boolean used[] = new boolean[sdp.streams.length];
      for(int c=0;c<pl.audioRTP.channels.size();) {
        RTPChannel channel = pl.audioRTP.channels.get(c);
        boolean ok = false;
        for(int s=0;s<sdp.streams.length;s++) {
          SDP.Stream stream = sdp.streams[s];
          if (stream.content.equals(channel.stream.content)) {
            channel.change(stream);
            ok = true;
            used[s] = true;
            break;
          }
        }
        if (!ok) {
          //RTPChannel no longer in use
          pl.audioRTP.removeChannel(channel);
        } else {
          c++;
        }
      }
/*
      if (pl.videoRTP != null) {
        for(int c=0;c<pl.videoRTP.channels.size();) {
          RTPChannel channel = pl.videoRTP.channels.get(c);
          boolean ok = false;
          for(int s=0;s<sdp.streams.length;s++) {
            SDP.Stream stream = sdp.streams[s];
            if (stream.content.equals(channel.stream.content)) {
              channel.change(stream);
              ok = true;
              used[s] = true;
              break;
            }
          }
          if (!ok) {
            //RTPChannel no longer in use
            JFLog.log("Video Channel no longer in use:" + channel.stream.content);
            pl.videoRTP.removeChannel(channel);
            RemoteCamera camera = findRemoteCamera(channel);
            if (camera != null) delRemoteCamera(pl, camera);
          } else {
            c++;
          }
        }
      }
      //check if new video stream's were added
      for(int s=0;s<sdp.streams.length;s++) {
        SDP.Stream stream = sdp.streams[s];
        if (used[s]) continue;
        if (stream.type == SDP.Type.audio) continue;  //ignore additional audio streams
        if (!stream.canRecv() || !stream.canSend()) continue;  //ignore inactive streams
        if (pl.videoRTP == null) {
          //video never started yet
          pl.videoRTP = new RTP();
          pl.videoRTP.start();
        }
        RTPChannel channel = pl.videoRTP.createChannel(stream);
        if (channel == null) {
          JFLog.log("RTP.createChannel() failed");
          continue;
        }
        if (stream.isSecure() && pl.sdp.getFirstVideoStream().keyExchange == SDP.KeyExchange.SDP) {
          //TODO : add crypto keys
          JFLog.log("Not implemented yet!");
          continue;
        }
        if (!channel.start()) {
          JFLog.log("RTP start failed");
          continue;
        }
        if (pl.videoWindow == null) continue;
        addRemoteCamera(pl, channel);
      }
*/
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Starts RTP after negotiation is complete (inbound call only). */

  public boolean startRTPinbound() {
    PhoneLine pl = lines[line];
    try {
      SDP.Stream astream = pl.sdp.getFirstAudioStream();
      SDP.Stream vstream = pl.sdp.getFirstVideoStream();
/*
      String list[] = Settings.current.getAudioCodecs();
      for(int a=0;a<list.length;a++) {
        JFLog.log("enabled[a]=" + list[a]);
      }
      Codec codecs[] = astream.codecs;
      for(int a=0;a<codecs.length;a++) {
        JFLog.log("offer[a]=" + codecs[a]);
      }
*/
      if ( (!astream.hasCodec(RTP.CODEC_G729a) || !Settings.current.hasAudioCodec(RTP.CODEC_G729a))
        && (!astream.hasCodec(RTP.CODEC_G711u) || !Settings.current.hasAudioCodec(RTP.CODEC_G711u))
        && (!astream.hasCodec(RTP.CODEC_G711a) || !Settings.current.hasAudioCodec(RTP.CODEC_G711a))
        && (!astream.hasCodec(RTP.CODEC_G722) || !Settings.current.hasAudioCodec(RTP.CODEC_G722)) )
      {
        JFLog.log("err:callAccept() : No compatible audio codec offered");
        pl.sip.deny(pl.callid, "NO_COMPATIBLE_CODEC", 415);
        onCancel(pl.sip, pl.callid, 415);
        return false;
      }
      astream.setCodec(pl.localsdp.getFirstAudioStream().codecs[0]);
      if (vstream != null) {
        if (pl.localsdp.hasVideo()) {
          vstream.setCodec(pl.localsdp.getFirstVideoStream().codecs[0]);
        } else {
          vstream = null;  //no video codecs match
        }
      }

      if (!pl.audioRTP.start()) {
        throw new Exception("RTP.start() failed");
      }
      if (pl.audioRTP.createChannel(astream) == null) {
        throw new Exception("RTP.createChannel() failed");
      }
      if (pl.sdp.getFirstAudioStream().isSecure()) {
        SRTPChannel channel = (SRTPChannel)pl.audioRTP.getDefaultChannel();
        if (pl.sdp.getFirstAudioStream().keyExchange == SDP.KeyExchange.SDP) {
          SDP.Stream local = pl.localsdp.getFirstAudioStream();
          SDP.Key localKey = local.getKey("AES_CM_128_HMAC_SHA1_80");
          if (localKey == null) throw new Exception("Local SRTP keys not found");
          channel.setLocalKeys(localKey.key, localKey.salt);
          SDP.Stream remote = pl.sdp.getFirstAudioStream();
          SDP.Key remoteKey = remote.getKey("AES_CM_128_HMAC_SHA1_80");
          if (remoteKey == null) throw new Exception("Remote SRTP keys not found");
          channel.setRemoteKeys(remoteKey.key, remoteKey.salt);
        } else {
          SDP.Stream local = pl.localsdp.getFirstAudioStream();
          channel.setDTLS(true, local.sdp.iceufrag, local.sdp.icepwd);
        }
      }
      if (!pl.audioRTP.getDefaultChannel().start()) {
        throw new Exception("RTPChannel.start() failed");
      }
/*
      if (!pl.videoRTP.start()) {
        throw new Exception("RTP.start() failed");
      }
      if (vstream != null) {
        if (pl.videoRTP.createChannel(vstream) == null) {
          throw new Exception("RTP.createChannel() failed");
        }
        if (pl.sdp.getFirstVideoStream().isSecure()) {
          SRTPChannel channel = (SRTPChannel)pl.videoRTP.getDefaultChannel();
          if (pl.sdp.getFirstVideoStream().keyExchange == SDP.KeyExchange.SDP) {
            SDP.Stream local = pl.localsdp.getFirstVideoStream();
            SDP.Key localKey = local.getKey("AES_CM_128_HMAC_SHA1_80");
            if (localKey == null) throw new Exception("Local SRTP keys not found");
            channel.setLocalKeys(localKey.key, localKey.salt);
            SDP.Stream remote = pl.sdp.getFirstVideoStream();
            SDP.Key remoteKey = remote.getKey("AES_CM_128_HMAC_SHA1_80");
            if (remoteKey == null) throw new Exception("Remote SRTP keys not found");
            channel.setRemoteKeys(remoteKey.key, remoteKey.salt);
          } else {
            SDP.Stream local = pl.localsdp.getFirstVideoStream();
            channel.setDTLS(true, local.sdp.iceufrag, local.sdp.icepwd);
          }
        }
        if (!pl.videoRTP.getDefaultChannel().start()) {
          throw new Exception("RTPChannel.start() failed");
        }
      }
*/
      pl.rtpStarted = true;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      pl.sip.deny(pl.callid, "RTP_START_FAILED", 500);
      onCancel(pl.sip, pl.callid, 500);
      return false;
    }
  }

  public boolean startRTPoutbound(int xline) {
    PhoneLine pl = lines[xline];
    try {
      SDP.Stream astream = pl.sdp.getFirstAudioStream();
      SDP.Stream vstream = pl.sdp.getFirstVideoStream();
      JFLog.log("note:callInviteSuccess():remotertpport=" + astream.port + ",remoteVrtport=" + (vstream != null ? vstream.port : -1));
      if ( (!astream.hasCodec(RTP.CODEC_G729a) || !Settings.current.hasAudioCodec(RTP.CODEC_G729a))
        && (!astream.hasCodec(RTP.CODEC_G711u) || !Settings.current.hasAudioCodec(RTP.CODEC_G711u))
        && (!astream.hasCodec(RTP.CODEC_G711a) || !Settings.current.hasAudioCodec(RTP.CODEC_G711a))
        && (!astream.hasCodec(RTP.CODEC_G722) || !Settings.current.hasAudioCodec(RTP.CODEC_G722)) )
      {
        JFLog.log("err:callInviteSuccess() : No compatible audio codec returned");
        pl.sip.bye(pl.callid);
        onCancel(pl.sip, pl.callid, 415);
        return false;
      }
      if (!pl.audioRTP.start()) {
        throw new Exception("RTP.start() failed");
      }
      if (pl.audioRTP.createChannel(astream) == null) {
        throw new Exception("RTP.createChannel() failed");
      }
      if (pl.sdp.getFirstAudioStream().isSecure()) {
        SRTPChannel channel = (SRTPChannel)pl.audioRTP.getDefaultChannel();
        if (pl.sdp.getFirstAudioStream().keyExchange == SDP.KeyExchange.SDP) {
          SDP.Stream local = pl.localsdp.getFirstAudioStream();
          SDP.Key localKey = local.getKey("AES_CM_128_HMAC_SHA1_80");
          if (localKey == null) throw new Exception("Local SRTP keys not found");
          channel.setLocalKeys(localKey.key, localKey.salt);
          SDP.Stream remote = pl.sdp.getFirstAudioStream();
          SDP.Key remoteKey = remote.getKey("AES_CM_128_HMAC_SHA1_80");
          if (remoteKey == null) throw new Exception("Remote SRTP keys not found");
          channel.setRemoteKeys(remoteKey.key, remoteKey.salt);
        } else {
          SDP.Stream local = pl.localsdp.getFirstAudioStream();
          channel.setDTLS(false, local.sdp.iceufrag, local.sdp.icepwd);
        }
      }
      if (!pl.audioRTP.getDefaultChannel().start()) {
        throw new Exception("RTPChannel.start() failed");
      }
/*
      if (!pl.videoRTP.start()) {
        throw new Exception("RTP.start() failed");
      }
      if (vstream != null) {
        if (pl.videoRTP.createChannel(vstream) == null) {
          throw new Exception("RTP.createChannel() failed");
        }
        if (pl.sdp.getFirstVideoStream().isSecure()) {
          SRTPChannel channel = (SRTPChannel)pl.videoRTP.getDefaultChannel();
          if (pl.sdp.getFirstVideoStream().keyExchange == SDP.KeyExchange.SDP) {
            SDP.Stream local = pl.localsdp.getFirstVideoStream();
            SDP.Key localKey = local.getKey("AES_CM_128_HMAC_SHA1_80");
            if (localKey == null) throw new Exception("Local SRTP keys not found");
            channel.setLocalKeys(localKey.key, localKey.salt);
            SDP.Stream remote = pl.sdp.getFirstVideoStream();
            SDP.Key remoteKey = remote.getKey("AES_CM_128_HMAC_SHA1_80");
            if (remoteKey == null) throw new Exception("Remote SRTP keys not found");
            channel.setRemoteKeys(remoteKey.key, remoteKey.salt);
          } else {
            SDP.Stream local = pl.localsdp.getFirstVideoStream();
            channel.setDTLS(false, local.sdp.iceufrag, local.sdp.icepwd);
          }
        }
        if (!pl.videoRTP.getDefaultChannel().start()) {
          throw new Exception("RTPChannel.start() failed");
        }
      }
*/
      pl.rtpStarted = true;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      pl.sip.deny(pl.callid, "RTP_START_FAILED", 500);
      onCancel(pl.sip, pl.callid, 500);
      return false;
    }
  }

  /** Send a reINVITE when the callee returns multiple codecs to select only most prefered codec (if reinvite is enabled). */

  public boolean reinvite(PhoneLine pl) {
    SDP.Stream astream = pl.sdp.getFirstAudioStream();
    SDP.Stream vstream = pl.sdp.getFirstVideoStream();
    int acnt = 0;
    int vcnt = 0;
    if (SIP.hasCodec(astream.codecs, RTP.CODEC_G729a)) acnt++;
    if (SIP.hasCodec(astream.codecs, RTP.CODEC_G711u)) acnt++;
    if (SIP.hasCodec(astream.codecs, RTP.CODEC_G711a)) acnt++;
    if (SIP.hasCodec(astream.codecs, RTP.CODEC_G722)) acnt++;

    if (vstream != null) {
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_JPEG)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H263)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H263_1998)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H263_2000)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H264)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_VP8)) vcnt++;
    }

    if ((acnt > 1 || vcnt > 1) && (Settings.current.reinvite)) {
      //returned more than one audio codec, reinvite with only one codec
      //do NOT reINVITE from a 183 - server will respond with 491 (request pending) and abort the call
      pl.localsdp = getLocalSDPAccept(pl);
      pl.sip.reinvite(pl.callid, pl.localsdp);
      return true;
    }
    return false;
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

  public void onSuccess(SIPClient sip, String callid, SDP sdp, boolean complete) {
    if (!complete) {
      //183 - could start RTP and listen to ringback tone
      onRinging(sip,callid);
      return;
    }
    //is a line trying to do an invite or hold
    for(int a=0;a<6;a++) {
      if (!lines[a].incall) continue;
      if (!lines[a].callid.equals(callid)) continue;
      if (!lines[a].talking) {
        lines[a].sdp = sdp;
        if (reinvite(lines[a])) {
          JFLog.log("reINVITE");
          return;
        }
        lines[a].status = "Connected";
        if (line == a) if (main != null) main._updateScreen();
        callInviteSuccess(a, sdp);
        lines[a].talking = true;
        lines[a].ringing = false;
        return;
      } else {
        //reINVITE accepted (200)
        lines[a].sdp = sdp;
        change(a, sdp);
        return;
      }
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

  public int onInvite(SIPClient sip, String callid, String fromid, String fromnumber, SDP sdp) {
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (lines[a].sip == sip) {
        if (lines[a].sip.getUser().equals(fromnumber)) {
          return 486;  //reply BUSY - attempt to call self (should goto voicemail now)
        }
        if (lines[a].callid.equals(callid)) {
          //reINVITEd (usually to change RTP host/port) (codec should not change since we only accept with 1 codec)
          if (sdp == null) {
            JFLog.log("onInvite: SDP null on reinvite");
            return -1;  //TODO : send an error and drop call???
          }
          pl.sdp = sdp;
          change(a, sdp);
          pl.localsdp = getLocalSDPAccept(pl);
          pl.sip.reaccept(callid, pl.localsdp);  //send 200 rely with new SDP
          return -1;  //do not send a reply
        }
        if (pl.incall) continue;
        if (pl.incoming) continue;
        lines[a].dial = fromnumber;
        lines[a].callerid = fromid;
        if ((lines[a].callerid == null) || (lines[a].callerid.trim().length() == 0)) lines[a].callerid = "Unknown";
        lines[a].status = fromid + " is calling";
        lines[a].incoming = true;
        pl.sdp = sdp;
        lines[a].callid = callid;
        lines[a].ringing = true;
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
        PhoneLine pl = lines[a];
        pl.incall = false;
        pl.trying = false;
        if (pl.audioRTP != null) {
          pl.audioRTP.uninit();
          pl.audioRTP = null;
        }
        pl.callid = "";
        pl.dial = "";
        pl.status = "Hungup (" + code + ")";
        pl.ringing = false;
        pl.incoming = false;
        pl.rtpStarted = false;
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

  public void onNotify(SIPClient sip, String callid, String event, String content) {
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

  public void onAck(SIPClient client, String callid, SDP sdp) {
    if (sdp == null) return;
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.sip == client) {
        if (!pl.rtpStarted) {
          //RFC 3665 section 3.6 - ACK provides SDP instead of INVITE
          pl.sdp = sdp;
          startRTPinbound();
        }
      }
    }
  }

//interface RTPInterface (not used in android release)
  public void rtpSamples(RTPChannel rtp) {}

  public void rtpDigit(RTPChannel rtp, char digit) {}

  public void rtpPacket(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtcpPacket(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtpH263(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtpH263_1998(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtpH263_2000(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtpH264(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtpVP8(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtpJPEG(RTPChannel rtp, byte[] data, int off, int len) {}

  public void rtpInactive(RTPChannel rtp) {}
}
