/*
 * BasePhone.java
 *
 * Created on Oct 22, 2011, 10:26:03 AM
 *
 * @author pquiring@gmail.com
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.voip.*;
import javaforce.media.*;

/** Base Panel contains all phone logic code. */

public abstract class BasePhone extends javax.swing.JPanel implements SIPClientInterface, RTPInterface, ActionListener, KeyEventDispatcher {

  public static String version = "1.28";

  public static boolean debug = false;

  public void initBasePhone(GUI gui, WindowController wc) {
    JFLog.init(JF.getUserPath() + "/.jfphone.log", true);
    this.gui = gui;
    this.wc = wc;
    setLAF();  //must do this before any GUI elements are created
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    if (Settings.isJavaScript) initRPC();
    Settings.isWindows = JF.isWindows();
    Settings.isLinux = !Settings.isWindows;
    Settings.hasFFMPEG = true;
    initDTLS();
  }

  //global data
  public GUI gui;
  public int sipmin = 5061;
  public int sipmax = 5199;
  public int sipnext = 5061;
  public int line = -1;  //current selected line (0-5) (-1=none)
  public PhoneLine lines[];
  public JToggleButton lineButtons[];
  public JButton numButtons[];
  public Audio sound = new Audio();
  public WindowController wc;
  public String lastDial;
  public boolean showingContacts = false;
  public boolean showingChat = false;
  public java.util.Timer timerKeepAlive, timerRegisterExpires, timerRegisterRetries;
  public ImageIcon ii[];
  public String icons[] = {
    "blk.png", "grn.png", "red.png", "grey.png", "orange.png",
    "mic.png", "headset.png",
    "mute.png", "spk.png",
    "swscale.png", "hwscale.png",
    "icon_open.png", "icon_closed.png", "icon_busy.png", "icon_idle.png", "icon_dnd.png",
    "labels1.png", "labels2.png",
    "trayicon.png",
    "call.png", "end.png",
    "call2.png", "end2.png",
    "logo.png", "video.png", "record.png"
  };
  public final int PIC_BLACK = 0;
  public final int PIC_GREEN = 1;
  public final int PIC_RED = 2;
  public final int PIC_GREY = 3;
  public final int PIC_ORANGE = 4;
  public final int PIC_MIC = 5;
  public final int PIC_HEADSET = 6;
  public final int PIC_MUTE = 7;
  public final int PIC_SPK = 8;
  public final int PIC_SWSCALE = 9;
  public final int PIC_HWSCALE = 10;
  public final int PIC_ICON_OPEN = 11;
  public final int PIC_ICON_CLOSED = 12;
  public final int PIC_ICON_BUSY = 13;
  public final int PIC_ICON_IDLE = 14;
  public final int PIC_ICON_DND = 15;
  public final int PIC_LABELS1 = 16;
  public final int PIC_LABELS2 = 17;
  public final int PIC_TRAY = 18;
  public final int PIC_CALL = 19;
  public final int PIC_END = 20;
  public final int PIC_CALL2 = 21;
  public final int PIC_END2 = 22;
  public final int PIC_LOGO = 23;
  public final int PIC_VIDEO = 24;
  public final int PIC_RECORD = 25;
  public int registerRetries;
  public SystemTray tray;
  public TrayIcon icon;
  public MenuItem exit, show;
  public Vector<Contact> contactList = new Vector<Contact>();
  public boolean active = true;
  public boolean muted = false;

  public abstract void rtp_jpeg_receive(RTPChannel rtp, byte data[], int pos, int len);
  public abstract void rtp_h263_receive(RTPChannel rtp, byte data[], int pos, int len);
  public abstract void rtp_h263_1998_receive(RTPChannel rtp, byte data[], int pos, int len);
  public abstract void rtp_h263_2000_receive(RTPChannel rtp, byte data[], int pos, int len);
  public abstract void rtp_h264_receive(RTPChannel rtp, byte data[], int pos, int len);
  public abstract void rtp_h265_receive(RTPChannel rtp, byte data[], int pos, int len);
  public abstract void rtp_vp8_receive(RTPChannel rtp, byte data[], int pos, int len);
  public abstract void rtp_vp9_receive(RTPChannel rtp, byte data[], int pos, int len);
  public boolean registeringAll = false;
  public boolean doConfig = false;  //do config after register all

  /** Registers all SIP connections. */

  public void reRegisterAll() {
    registeringAll = true;
    int idx;
    String host;
    int port = -1;
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if ((a > 0) && (Settings.current.lines[a].same != -1)) continue;
      pl.disableVideo = Settings.current.lines[a].disableVideo;
      pl.srtp = Settings.current.lines[a].srtp;
      pl.dtls = Settings.current.lines[a].dtls;
      if (Settings.current.lines[a].host.length() == 0) continue;
      if (Settings.current.lines[a].user.length() == 0) continue;
      lines[a].sip = new SIPClient();
      idx = Settings.current.lines[a].host.indexOf(':');
      if (idx == -1) {
        host = Settings.current.lines[a].host;
        switch (Settings.current.lines[a].transport) {
          case 0:
          case 1:
            port = 5060;  //default UDP/TCP port
            break;
          case 2:
            port = 5061;  //default TLS port
            break;
        }
      } else {
        host = Settings.current.lines[a].host.substring(0,idx);
        port = JF.atoi(Settings.current.lines[a].host.substring(idx+1));
      }
      switch (Settings.current.lines[a].transport) {
        case 0: pl.transport = TransportType.UDP; break;
        case 1: pl.transport = TransportType.TCP; break;
        case 2: pl.transport = TransportType.TLS; break;
      }
      int attempt = 0;
      while (!pl.sip.init(host, port, getlocalport(), this, pl.transport)) {
        attempt++;
        if (attempt==10) {
          pl.sip = null;
          pl.status = "SIP init failed";
          if (a == line) gui.updateLine();
          break;
        }
      }
      if (pl.sip == null) continue;  //sip.init failed
      if (Settings.current.lines[a].siplog) {
        pl.sip.log(a+1, JF.getUserPath() + "/jfphone-sip-protocol-line" + (a+1) + ".log");
      }
      pl.user = Settings.current.lines[a].user;
//JFLog.log("lines[" + a + "].pass=" + Settings.current.lines[a].pass + "!");
      if ((Settings.current.lines[a].pass == null) || (Settings.current.lines[a].pass.length() == 0) || (Settings.current.lines[a].pass.equals("crypto(1,)"))) {
        pl.auth = true;
        pl.noregister = true;
        pl.status = "Ready (" + pl.user + ")";
      }
    }
    //setup "Same as" lines
    int same;
    for(int a=1;a<6;a++) {
      same = Settings.current.lines[a].same;
      if (same == -1) continue;
      PhoneLine pl = lines[a];
      pl.disableVideo = lines[same].disableVideo;
      pl.srtp = lines[same].srtp;
      pl.transport = lines[same].transport;
      pl.dtls = lines[same].dtls;
      pl.sip = lines[same].sip;
      pl.user = lines[same].user;
      pl.noregister = lines[same].noregister;
      if (pl.noregister) {
        pl.auth = true;
        pl.status = "Ready (" + pl.user + ")";
      }
    }
    //register lines
    for(int a=0;a<6;a++) {
      if ((a > 0) && (Settings.current.lines[a].same != -1)) continue;
      PhoneLine pl = lines[a];
      if (pl.sip == null) continue;
      try {
        pl.sip.register(Settings.current.lines[a].name, Settings.current.lines[a].user, Settings.current.lines[a].auth
          , Settings.getPassword(Settings.current.lines[a].pass), Settings.current.sipexpires);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    //setup reRegister timer (expires)
    int expires = Settings.current.sipexpires;
    expires -= 5;  //do a little early just in case
    expires *= 1000;  //convert to ms
    timerRegisterExpires = new java.util.Timer();
    timerRegisterExpires.scheduleAtFixedRate(new ReRegisterExpires(), expires, expires);
    registerRetries = 0;
    timerRegisterRetries = new java.util.Timer();
    timerRegisterRetries.schedule(new ReRegisterRetries(), 1000);
    registeringAll = false;
    if (doConfig) {
      doConfig = false;
      gui.doConfig();
    }
  }

  /** Expires registration with all SIP connections. */

  public void unRegisterAll() {
    if (timerRegisterExpires != null) {
      timerRegisterExpires.cancel();
      timerRegisterExpires = null;
    }
    if (timerRegisterRetries != null) {
      timerRegisterRetries.cancel();
      timerRegisterRetries = null;
    }
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.incall) {
        gui.selectLine(a);
        if (pl.talking)
          pl.sip.bye(pl.callid);
        else
          pl.sip.cancel(pl.callid);
        endLine(a);
      }
      pl.dial = "";
      pl.status = "";
      pl.unauth = false;
      pl.auth = false;
      pl.noregister = false;
      pl.user = "";
      if ((a > 0) && (Settings.current.lines[a].same != -1)) {
        pl.sip = null;
        continue;
      }
      if (pl.sip == null) continue;
      if (pl.sip.isRegistered()) {
        try {
          if (monitorUnSubscribe(pl.sip) > 0) {
            JF.sleep(100);
          }
          pl.sip.unregister();
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
    int maxwait;
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.sip == null) continue;
      maxwait = 1000;
      while (pl.sip.isRegistered()) { JF.sleep(10); maxwait -= 10; if (maxwait == 0) break; }
      pl.sip.uninit();
      pl.sip = null;
    }
  }

  /** Add a digit to be dialed. */

  public void addDigit(char digit) {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.sip == null) return;
    if (!pl.auth) return;
    if (pl.incoming) return;
    if (digit == KeyEvent.VK_BACK_SPACE) {
      if ((pl.incall)&&(!pl.xfer)) return;
      //delete digit
      int len = pl.dial.length();
      if (len > 0) pl.dial = pl.dial.substring(0, len-1);
    } else {
      if ((pl.incall)&&(!pl.xfer)) return;
      pl.dial += digit;
    }
    gui.updateLine();
  }

  /** KeyBinding action.  Causes DTMF generation if in a call. */

  public void pressDigit(char digit) {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.xfer) return;
    pl.dtmf = digit;
  }

  /** KeyBinding action.  Stops DTMF generation. */

  public void releaseDigit(char digit) {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.dtmf != 'x') pl.dtmfend = true;
  }

  /** Clears number to be dialed. */

  public void clear() {
    if (line == -1) return;
    lines[line].dial = "";
    gui.updateLine();
  }

  /** Sets the entire # to be dialed. */

  public void setDial(String number) {
    if (line == -1) return;
    lines[line].dial = number;
    gui.updateLine();
  }

  /** Starts a call or accepts an inbound call on selected line. */

  public void call() {
    gui.updateCallButton(false);
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.sip == null) return;
    if (!pl.auth) return;
    if (pl.incall) {gui.updateCallButton(true); return;}  //already in call
    if (pl.dial.length() == 0) {gui.updateCallButton(false); return;}
    gui.updateCallButton(true);
    if (pl.incoming) {
      callAccept();
    } else {
      callInvite();
    }
    if (Settings.current.ac) {
      if (!pl.cnf) doConference();
    }
  }

  /** Terminates a call. */

  public void end() {
    gui.updateEndButton(false);
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.incoming) {
      pl.sip.deny(pl.callid, "IGNORE", 486);
      pl.incoming = false;
      pl.ringing = false;
      pl.ringback = false;
      pl.dial = "";
      pl.status = "Hungup";
      gui.updateLine();
      return;
    }
    pl.dial = "";
    if (!pl.incall) {
      //no call (update status)
      if ((pl.sip != null) && (!pl.unauth)) pl.status = "Ready (" + pl.user + ")";
      gui.updateLine();
      return;
    }
    if (pl.talking)
      pl.sip.bye(pl.callid);
    else
      pl.sip.cancel(pl.callid);
    endLine(line);
  }

  /** Cleanup after a call is terminated (call terminated local or remote). */

  public void endLine(int xline) {
    gui.updateEndButton(false);
    gui.updateCallButton(false);
    PhoneLine pl = lines[xline];
    pl.dial = "";
    pl.status = "Hungup";
    pl.trying = false;
    pl.ringing = false;
    pl.ringback = false;
    pl.incoming = false;
    pl.cnf = false;
    pl.xfer = false;
    pl.incall = false;
    pl.talking = false;
    pl.hld = false;
    pl.rtpStarted = false;
    if (pl.audioRTP != null) {
      pl.audioRTP.stop();
      pl.audioRTP = null;
    }
    if (pl.videoRTP != null) {
      pl.videoRTP.stop();
      pl.videoRTP = null;
    }
    pl.callid = "";
    if (line == xline) gui.updateLine();
    gui.endLineUpdate(xline);
  }

  /** Starts a outbound call. */

  public void callInvite() {
    PhoneLine pl = lines[line];
    pl.to = pl.dial;
    pl.incall = true;
    pl.trying = false;
    pl.ringing = false;
    pl.ringback = false;
    pl.talking = false;
    pl.incoming = false;
    pl.status = "Dialing";
    lastDial = pl.dial;
    Settings.addCallLog(pl.dial);
    pl.audioRTP = new RTP();
    if (!pl.audioRTP.init(this)) {
      endLine(line);
      pl.dial = "";
      pl.status = "RTP init failed";
      gui.updateLine();
      return;
    }
    pl.videoRTP = new RTP();
    if (!pl.videoRTP.init(this)) {
      endLine(line);
      pl.dial = "";
      pl.status = "RTP init failed";
      gui.updateLine();
      return;
    }
    pl.localsdp = getLocalSDPInvite(pl);

    pl.callid = pl.sip.invite(pl.dial, pl.localsdp);
    gui.updateLine();
    gui.callInviteUpdate();
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

  /** Returns SDP that matches requested SDP. */

  private SDP getLocalSDPAccept(PhoneLine pl) {
    SDP sdp = new SDP();
    SDP.Stream astream = pl.sdp.getFirstAudioStream();
    SDP.Stream vstream = pl.sdp.getFirstVideoStream();
    SDP.Stream newAstream = sdp.addStream(SDP.Type.audio);
    newAstream.port = pl.audioRTP.getlocalrtpport();
    newAstream.mode = complementMode(astream.mode);
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
      if ((enabledCodecs[a].equals(RTP.CODEC_GSM.name)) && (astream.hasCodec(RTP.CODEC_GSM))) {
        newAstream.addCodec(RTP.CODEC_GSM);
        break;
      }
      if ((enabledCodecs[a].equals(RTP.CODEC_G722.name)) && (astream.hasCodec(RTP.CODEC_G722))) {
        newAstream.addCodec(RTP.CODEC_G722);
        break;
      }
    }

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
        if ((enabledCodecs[a].equals(RTP.CODEC_H265.name)) && (vstream.hasCodec(RTP.CODEC_H265))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_H265);
          break;
        }
        if ((enabledCodecs[a].equals(RTP.CODEC_VP8.name)) && (vstream.hasCodec(RTP.CODEC_VP8))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_VP8);
          break;
        }
        if ((enabledCodecs[a].equals(RTP.CODEC_VP9.name)) && (vstream.hasCodec(RTP.CODEC_VP9))) {
          addVideoStream(pl, sdp, vstream, RTP.CODEC_VP9);
          break;
        }
      }
    }
    return sdp;
  }

  /** Returns SDP packet for all enabled codecs (audio and video). */

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
    if (pl.srtp) {
      stream.profile = SDP.Profile.SAVP;
      if (!pl.dtls) {
        stream.keyExchange = SDP.KeyExchange.SDP;
        stream.addKey("AES_CM_128_HMAC_SHA1_80", genKey(), genSalt());
      } else {
        JFLog.log("Using DTLS fingerprint=" + fingerprintSHA256);
        stream.keyExchange = SDP.KeyExchange.DTLS;
        stream.sdp.fingerprint = fingerprintSHA256;
        stream.sdp.iceufrag = RTP.genIceufrag();
        stream.sdp.icepwd = RTP.genIcepwd();
      }
    }
    String enabledCodecs[] = Settings.current.getAudioCodecs();
    for(int a=0;a<enabledCodecs.length;a++) {
      if (enabledCodecs[a].equals(RTP.CODEC_G729a.name)) stream.addCodec(RTP.CODEC_G729a);
      if (enabledCodecs[a].equals(RTP.CODEC_G711u.name)) stream.addCodec(RTP.CODEC_G711u);
      if (enabledCodecs[a].equals(RTP.CODEC_G711a.name)) stream.addCodec(RTP.CODEC_G711a);
      if (enabledCodecs[a].equals(RTP.CODEC_GSM.name)) stream.addCodec(RTP.CODEC_GSM);
      if (enabledCodecs[a].equals(RTP.CODEC_G722.name)) stream.addCodec(RTP.CODEC_G722);
    }
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
          if (enabledCodecs[a].equals(RTP.CODEC_H265.name)) stream.addCodec(RTP.CODEC_H265);
          if (enabledCodecs[a].equals(RTP.CODEC_VP8.name)) stream.addCodec(RTP.CODEC_VP8);
          if (enabledCodecs[a].equals(RTP.CODEC_VP9.name)) stream.addCodec(RTP.CODEC_VP9);
        }
      }
    }
    return sdp;
  }

  /** Starts RTP after negotiation is complete (inbound call only). */

  public boolean startRTPinbound() {
    PhoneLine pl = lines[line];
    try {
      SDP.Stream astream = pl.sdp.getFirstAudioStream();
      SDP.Stream vstream = pl.sdp.getFirstVideoStream();
      if ( (!astream.hasCodec(RTP.CODEC_G729a) || !Settings.current.hasAudioCodec(RTP.CODEC_G729a))
        && (!astream.hasCodec(RTP.CODEC_G711u) || !Settings.current.hasAudioCodec(RTP.CODEC_G711u))
        && (!astream.hasCodec(RTP.CODEC_G711a) || !Settings.current.hasAudioCodec(RTP.CODEC_G711a))
        && (!astream.hasCodec(RTP.CODEC_GSM) || !Settings.current.hasAudioCodec(RTP.CODEC_GSM))
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
      JFLog.log("callInviteSuccess():remote Audio port=" + astream.port + ",remote Video port=" + (vstream != null ? vstream.port : -1));
      if ( (!astream.hasCodec(RTP.CODEC_G729a) || !Settings.current.hasAudioCodec(RTP.CODEC_G729a))
        && (!astream.hasCodec(RTP.CODEC_G711u) || !Settings.current.hasAudioCodec(RTP.CODEC_G711u))
        && (!astream.hasCodec(RTP.CODEC_G711a) || !Settings.current.hasAudioCodec(RTP.CODEC_G711a))
        && (!astream.hasCodec(RTP.CODEC_GSM) || !Settings.current.hasAudioCodec(RTP.CODEC_GSM))
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
      pl.rtpStarted = true;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      pl.sip.deny(pl.callid, "RTP_START_FAILED", 500);
      onCancel(pl.sip, pl.callid, 500);
      return false;
    }
  }


  /** Accepts an inbound call on selected line. */

  public void callAccept() {
    PhoneLine pl = lines[line];
    try {
      pl.to = pl.dial;
      pl.audioRTP = new RTP();
      if (!pl.audioRTP.init(this)) {
        throw new Exception("RTP.init() failed");
      }
      pl.videoRTP = new RTP();
      if (!pl.videoRTP.init(this)) {
        throw new Exception("RTP.init() failed");
      }
      pl.videoRTP.setMTU(1500);
      if (pl.sdp != null) {
        pl.localsdp = getLocalSDPAccept(pl);
        if (!startRTPinbound()) return;
      } else {
        //INVITE did not include SDP so start SDP negotiation on this side
        pl.localsdp = getLocalSDPInvite(pl);
      }

      pl.sip.accept(pl.callid, pl.localsdp);
      pl.incall = true;
      pl.ringing = false;
      pl.ringback = false;
      pl.incoming = false;
      pl.talking = true;
      pl.status = "Connected";
      gui.updateLine();
      updateIconTray();
    } catch (Exception e) {
      JFLog.log(e);
      pl.sip.deny(pl.callid, "RTP_START_FAILED", 500);
      onCancel(pl.sip, pl.callid, 500);
    }
  }

  /** Triggered when an outbound call (INVITE) was accepted. */

  public boolean callInviteSuccess(int xline, SDP sdp) {
    PhoneLine pl = lines[xline];
    try {
      pl.sdp = sdp;
      if (!startRTPoutbound(xline)) return false;
      if (Settings.current.aa) gui.selectLine(xline);
    } catch (Exception e) {
      JFLog.log(e);
      pl.sip.bye(pl.callid);
      onCancel(pl.sip, pl.callid, 500);
      return false;
    }
    return true;
  }

  /** Triggered when an outbound call (INVITE) was refused. */

  public void callInviteFail(int xline) {
    PhoneLine pl = lines[xline];
    pl.incall = false;
    pl.trying = false;
    if (pl.audioRTP != null) {
      pl.audioRTP.uninit();
    }
    if (pl.videoRTP != null) {
      pl.videoRTP.uninit();
    }
    pl.callid = "";
  }

  /** Start or finish a call transfer. */

  public void doXfer() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (!pl.talking) return;
    if (pl.xfer) {
      if (pl.dial.length() == 0) {
        //cancel xfer
        pl.status = "Connected";
      } else {
        pl.sip.refer(pl.callid, pl.dial);
        pl.status = "XFER Requested";
        pl.dial = "";
      }
      pl.xfer = false;
    } else {
      pl.dial = "";
      pl.status = "Enter dest and then XFER again";
      pl.xfer = true;
    }
    gui.updateLine();
  }

  /** Put a call into or out of hold. */

  public void doHold() {
    gui.hld_setIcon(ii[PIC_GREY]);
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (!pl.incall) return;
    pl.hld = !pl.hld;
    pl.sip.setHold(pl.callid, pl.hld);
    pl.sip.reinvite(pl.callid);
    gui.hld_setIcon(ii[pl.hld ? PIC_RED : PIC_GREY]);
  }

  /** Redial last number dialed. */

  public void doRedial() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.incall) return;
    if (lastDial == null) return;
    if (lastDial.length() == 0) return;
    pl.dial = lastDial;
    gui.updateLine();
    call();
  }

  /** Toggles AA (Auto Answer). */

  public void toggleAA() {
    Settings.current.aa = !Settings.current.aa;
    gui.aa_setIcon(ii[Settings.current.aa ? PIC_GREEN : PIC_GREY]);
  }

  /** Toggle AC (Auto Conference). */

  public void toggleAC() {
    Settings.current.ac = !Settings.current.ac;
    gui.ac_setIcon(ii[Settings.current.ac ? PIC_GREEN : PIC_GREY]);
  }

  /** Toggle DND (Do-Not-Disturb). */

  public void toggleDND() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (pl.incall) return;
    pl.dnd = !pl.dnd;
    gui.dnd_setIcon(ii[pl.dnd ? PIC_RED : PIC_GREY]);
    if (pl.dnd)
      pl.dial = Settings.current.dndCodeOn;
    else
      pl.dial = Settings.current.dndCodeOff;
    call();
  }

  /** Toggles the conference state of a line. */

  public void doConference() {
    if (line == -1) return;
    PhoneLine pl = lines[line];
    if (!pl.incall) return;
    pl.cnf = !pl.cnf;
    gui.cnf_setIcon(ii[pl.cnf ? PIC_GREEN : PIC_GREY]);
  }

  /** Toggles mute. */

  public void toggleMute() {
    muted = !muted;
    sound.setMute(muted);
    gui.mute_setIcon(ii[muted ? PIC_RED : PIC_GREY]);
  }

  /** Creates a timer to send keep-alives on all SIP connections.  Keep alive are done every 30 seconds (many routers have a 60 second timeout). */

  public void keepAliveinit() {
    timerKeepAlive = new java.util.Timer();
    timerKeepAlive.scheduleAtFixedRate(new KeepAlive(),0,30*1000);
  }

  /** TimerTask to perform keep-alives. on all SIP connections. */

  public class KeepAlive extends java.util.TimerTask {
    public void run() {
      for(int a=0;a<6;a++) {
        if (Settings.current.lines[a].same != -1) continue;
        PhoneLine pl = lines[a];
        if (pl.sip == null) continue;
        if (!pl.sip.isRegistered()) continue;
        pl.sip.keepalive();
        if (pl.talking) {
          if (pl.audioRTP != null) pl.audioRTP.keepalive();
          if (pl.videoRTP != null) pl.videoRTP.keepalive();
        }
      }
    }
  }

  /** TimerTask that reregisters all SIP connection after they expire (every 3600 seconds). */

  public class ReRegisterExpires extends java.util.TimerTask {
    public void run() {
      for(int a=0;a<6;a++) {
        PhoneLine pl = lines[a];
        if (Settings.current.lines[a].same != -1) continue;
        if (pl.sip == null) continue;
        if (pl.noregister) continue;
        pl.sip.reregister();
      }
      registerRetries = 0;
      if (timerRegisterRetries != null) {
        timerRegisterRetries = new java.util.Timer();
        timerRegisterRetries.schedule(new ReRegisterRetries(), 1000);
      }
    }
  }

  /** TimerTask that reregisters any SIP connections that have failed to register (checks every 1 second upto 5 attempts). */

  public class ReRegisterRetries extends java.util.TimerTask {
    public void run() {
      boolean again = false;
      if (registerRetries < 5) {
        for(int a=0;a<6;a++) {
          if (Settings.current.lines[a].same != -1) continue;
          PhoneLine pl = lines[a];
          if (pl.sip == null) continue;
          if (pl.unauth) continue;
          if (pl.noregister) continue;
          if (!pl.sip.isRegistered()) {
            JFLog.log("warn:retry register on line:" + (a+1));
            pl.sip.reregister();
            again = true;
          }
        }
        registerRetries++;
        if (again) {
          timerRegisterRetries = new java.util.Timer();
          timerRegisterRetries.schedule(new ReRegisterRetries(), 1000);
          return;
        }
      }
      for(int a=0;a<6;a++) {
        PhoneLine pl = lines[a];
        if (pl.sip == null) continue;
        if (pl.unauth) continue;
        if (pl.noregister) continue;
        if (!pl.sip.isRegistered()) {
          pl.unauth = true;  //server not responding after 5 attempts to register
          pl.status = "Server not responding";
          if (a == line) gui.updateLine();
        }
      }
      timerRegisterRetries = null;
    }
  }

  /** Loads icons during startup. */

  public void loadIcons() {
    ii = new ImageIcon[icons.length];
    for(int a=0;a<icons.length;a++) {
      try {
        InputStream is = getClass().getClassLoader().getResourceAsStream(icons[a]);
        int len = is.available();
        byte data[] = new byte[len];
        is.read(data);
        is.close();
        ii[a] = new ImageIcon(data);
      } catch (Exception e) {
        JFLog.log("err:loadIcons() Failed:" + e);
        BasePhone.unlockFile();
        System.exit(0);
      }
    }
  }

  /** Checks for an online update on startup. */

  public void checkVersion() {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
        new URI("https://jfphone.sourceforge.io/version" + (version.indexOf("beta") == -1 ? "" : "beta") + ".php").toURL().openStream()));
      final String line = reader.readLine();
      if (line.equals(version)) {JFLog.log("version is up-to-date"); return;}
      JFLog.log("newer version is available : " + line);
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          JFAWT.showMessage("Upgrade available", "A newer version of jfphone is available! (v" + line + ")\r\nPlease goto http://jfphone.sourceforge.net to download it");
        }
      });
    } catch (Exception e) {
      JFLog.log("err:unable to check for version update");
      JFLog.log(e);
    }
  }

  /** Updates notification area test. */

  public void updateIconTray() {
    if (icon == null) return;
    if (active) return;
    StringBuffer buf = new StringBuffer();
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.incoming == true) {
        if (buf.length() > 0) buf.append("\r\n");
        buf.append("\"" + pl.callerid + "\" " + pl.dial + " is on Line " + (a+1));
      }
    }
    if (buf.length() > 0) {
      icon.displayMessage("Incoming Call(s)", buf.toString(), TrayIcon.MessageType.INFO);
    } else {
      //BUG? How do you hide a message if visible???
    }
  }

  /** Toggles speaker phone mode. */

  public void toggleSpeaker() {
    Settings.current.speakerMode = !Settings.current.speakerMode;
    gui.spk_setIcon(ii[Settings.current.speakerMode ? PIC_GREEN : PIC_GREY]);
  }

  /** Returns status of a line. */

  public String getStatus(int a) {
    return lines[a].status;
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
    if (SIP.hasCodec(astream.codecs, RTP.CODEC_GSM)) acnt++;
    if (SIP.hasCodec(astream.codecs, RTP.CODEC_G722)) acnt++;

    if (vstream != null) {
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_JPEG)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H263)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H263_1998)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H263_2000)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H264)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_H265)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_VP8)) vcnt++;
      if (SIP.hasCodec(vstream.codecs, RTP.CODEC_VP9)) vcnt++;
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

  public int monitorSubscribe(SIPClient sip) {
    //subscribe for any that belong to sip
    int count = 0;
    String server = sip.getRemoteHost();
    for(int a=0;a<contactList.size();a++) {
      Contact contact = contactList.get(a);
      if (!contact.monitor()) continue;
      String fields[] = SIP.split(contact.sip_user);
      if (fields[2].equalsIgnoreCase(server)) {
        contact.callid = sip.subscribe(fields[1], "presence", 3600);
        count++;
      }
    }
    return count;
  }

  public int monitorUnSubscribe(SIPClient sip) {
    //unsubscribe for any that belong to sip
    int count = 0;
    String server = sip.getRemoteHost();
    for(int a=0;a<contactList.size();a++) {
      Contact contact = contactList.get(a);
      if (!contact.monitor()) continue;
      if (contact.callid == null) continue;
      String fields[] = SIP.split(contact.sip_user);
      if (fields[2].equalsIgnoreCase(server)) {
        sip.unsubscribe(contact.callid, fields[1], "presence");
        contact.callid = null;
        count++;
      }
    }
    return count;
  }

//SIPClientInterface interface

  /** SIPClientInterface.onRegister() : triggered when a SIPClient has confirmation of a registration with server. */

  public void onRegister(SIPClient sip, boolean status) {
    if (status) {
      //success
      for(int a=0;a<6;a++) {
        PhoneLine pl = lines[a];
        if (pl.sip != sip) continue;
        if (pl.status.length() == 0) pl.status = "Ready (" + pl.user + ")";
        pl.auth = true;
        if (line == -1) {
          gui.selectLine(a);
        } else {
          if (line == a) gui.updateLine();
        }
      }
      sip.subscribe(sip.getUser(), "message-summary", Settings.current.sipexpires);  //SUBSCRIBE to self for message-summary event (not needed with Asterisk but X-Lite does it)
      gui.onRegister(sip);
      monitorSubscribe(sip);
    } else {
      //failed
      for(int a=0;a<6;a++) {
        PhoneLine pl = lines[a];
        if (pl.sip == sip) {
          pl.status = "Unauthorized";
          pl.unauth = true;
          if (line == a) gui.selectLine(-1);
        }
      }
    }
  }

  /** SIPClientInterface.onTrying() : triggered when an INVITE returns status code 100 (TRYING). */

  public void onTrying(SIPClient sip, String callid) {
    //is a line trying to do an invite
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if ((pl.incall)&&(!pl.trying)) {
        if (pl.callid.equals(callid)) {
          pl.trying = true;
          pl.status = "Trying";
          if (line == a) gui.updateLine();
        }
      }
    }
  }

  /** SIPClientInterface.onRinging() : triggered when an INVITE returns status code 180 (RINGING). */

  public void onRinging(SIPClient sip, String callid) {
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if ((pl.incall)&&(!pl.ringing)) {
        if (pl.callid.equals(callid)) {
          pl.ringing = true;
          pl.status = "Ringing";
          if (line == a) gui.updateLine();
        }
      }
    }
  }

  /** SIPClientInterface.onSuccess() : triggered when an INVITE returns status code 200 (OK) or 183 (ringback). */

  public void onSuccess(SIPClient sip, String callid, SDP sdp, boolean complete) {
    JFLog.log("onSuccess : streams=" + sdp.streams.length);
    for(int s=0;s<sdp.streams.length;s++) {
      JFLog.log("onSuccess : stream=" + sdp.streams[s].getType() + "," + sdp.streams[s].getMode() + "," + sdp.streams[s].content);
      SDP.Stream stream = sdp.streams[s];
      for(int c=0;c<stream.codecs.length;c++) {
        JFLog.log("onSuccess : codecs[] = " + stream.codecs[c].name + ":" + stream.codecs[c].id);
      }
    }
    //is a line trying to do an invite or reinvite?
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (!pl.incall) continue;
      if (!pl.callid.equals(callid)) continue;
      if (!pl.talking) {
        pl.sdp = sdp;
        if (complete && reinvite(pl)) return;
        if (!callInviteSuccess(a, sdp)) return;
        if (complete) {
          //200 call complete
          pl.status = "Connected";
          if (line == a) gui.updateLine();
          pl.talking = true;
          pl.ringing = false;
          pl.ringback = false;
        } else {
          //183 call progress (ie: ringback tones)
          pl.status = "Ringing";
          if (line == a) gui.updateLine();
          pl.talking = true;  //NOTE:Must send silent data via RTP or firewall will BLOCK inbound anyways
          pl.ringing = true;
          pl.ringback = true;
        }
        return;
      } else {
        if (pl.ringback && complete) {
          pl.status = "Connected";
          if (line == a) gui.updateLine();
          pl.ringback = false;
          pl.ringing = false;
          pl.sdp = sdp;
          if (reinvite(pl)) return;  //??? could this cause inf loop ???
        }
      }
      //update RTP data in case reINVITE changes them (or when making progress from 183 to 200)
      pl.sdp = sdp;
      change(a, sdp);
      return;
    }
    JFLog.log("err:onSuccess() for unknown call:" + callid);
  }

  /** SIPClientInterface.onBye() : triggered when server terminates a call. */

  public void onBye(SIPClient sip, String callid) {
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.incall) {
        if (pl.callid.equals(callid)) {
          endLine(a);
          updateIconTray();
        }
      }
    }
  }

  /** SIPClientInterface.onInvite() : triggered when server send an INVITE to jfphone. */

  public int onInvite(SIPClient sip, String callid, String fromid, String fromnumber, SDP sdp) {
    //NOTE : onInvite() can not change codecs (use SIP.reaccept() to do that)
    JFLog.log("onInvite:fromid=" + fromid + ":fromnumber=" + fromnumber);
    if (sdp != null) {
      for(int s=0;s<sdp.streams.length;s++) {
        JFLog.log("onInvite : stream=" + sdp.streams[s].getType() + "," + sdp.streams[s].getMode() + "," + sdp.streams[s].content);
        SDP.Stream stream = sdp.streams[s];
        for(int c=0;c<stream.codecs.length;c++) {
          JFLog.log("onInvite : codecs[] = " + stream.codecs[c].name + ":" + stream.codecs[c].id);
        }
      }
    }
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.sip == sip) {
        if (!pl.auth) {
          pl.auth = true;  //received a call on a line that failed to register (can happen if server was down briefly)
        }
        if (pl.callid.equals(callid)) {
          if (pl.talking) {
            //reINVITE
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
          return 180;  //reply RINGING
        }
        if (pl.incall) continue;
        if (pl.incoming) continue;
        pl.dial = fromnumber;
        if (pl.dial == null || pl.dial.trim().length() == 0) pl.dial = "Unknown";
        pl.callerid = fromid;
        if ((pl.callerid == null) || (pl.callerid.trim().length() == 0)) pl.callerid = "Unknown";
        Settings.addCallLog(pl.dial);
        gui.updateRecentList();
        pl.status = pl.callerid + " is calling";
        pl.incoming = true;
        pl.sdp = sdp;
        pl.callid = callid;
        pl.ringing = true;
        if (Settings.current.aa) {
          gui.selectLine(a);
          gui.updateLine();
          call();  //this will send a reply
          return -1;  //do NOT send a reply
        } else {
          if (line == a) gui.updateLine();
          updateIconTray();
          return 180;  //reply RINGING
        }
      }
    }
    return 486;  //reply BUSY
  }

  /** SIPClientInterface.onCancel() : triggered when server send a CANCEL request after an INVITE, or an error occured. */

  public void onCancel(SIPClient sip, String callid, int code) {
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.callid.equals(callid)) {
        endLine(a);
        pl.status = "Hungup (" + code + ")";
        if (line == a) gui.updateLine();
        updateIconTray();
      }
    }
  }

  /** SIPClientInterface.onRefer() : triggered when the server accepts a transfer for processing (SIP code 202). */

  public void onRefer(SIPClient sip, String callid) {
    //NOTE:SIP code 202 doesn't really tell you the transfer was successful, it just tells you the transfer is in progress.
    //     see onNotify() event="refer" to determine if transfer was successful
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.callid.equals(callid)) {
        pl.status = "XFER Accepted";
        if (line == a) gui.updateLine();
      }
    }
  }

  /** SIPClientInterface.onNotify() : processes SIP:NOTIFY messages. */

  public void onNotify(SIPClient sip, String callid, String event, String[] msg) {
    JFLog.log("onNotify:event=" + event);
    event = event.toLowerCase();
    if (event.equals("message-summary")) {
      String msgwait = HTTP.getParameter(msg, "Messages-Waiting");
      if (msgwait != null) {
        for(int a=0;a<6;a++) {
          PhoneLine pl = lines[a];
          if (pl.sip == sip) {
//            JFLog.log("notify() line=" + a + ", msgwaiting = " + msgwaiting);
            pl.msgwaiting = msgwait.equalsIgnoreCase("yes");
          }
        }
      }
      return;
    }
    if (event.equals("presence")) {
      String content = String.join("", msg);
      JFLog.log("note:Presence:" + content);
      if (!content.startsWith("<?xml")) {JFLog.log("Not valid presence data (1)"); return;}
      XML xml = new XML();
      ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes());
      if (!xml.read(bais)) {JFLog.log("Not valid presence data (2)"); return;}
      XML.XMLTag contact = xml.getTag(new String[] { "presence", "tuple", "contact" });
      if (contact == null) {JFLog.log("Not valid presence data (3)"); return;}
      String fields[] = SIP.split("Unknown<" + contact.getContent() + ">");
      if (fields == null) {JFLog.log("Not valid presence data (4)"); return;}
      XML.XMLTag status = xml.getTag(new String[] { "presence", "tuple", "status", "basic" });
      if (status == null) {JFLog.log("Not valid presence data (5)"); return;}
      gui.setStatus(fields[1], fields[2], status.getContent().trim());
      return;
    }
    String parts[] = event.split(";");
    if (parts[0].equals("refer")) {
      int notifyLine = -1;
      for(int a=0;a<6;a++) {
        if (lines[a].callid.equals(callid)) {
          notifyLine = a;
          break;
        }
      }
      if (notifyLine == -1) {JFLog.log("Received NOTIFY:REFER that doesn't match any lines"); return;}
      parts = msg[0].split(" ");  //SIP/2.0 code desc
      int code = JF.atoi(parts[1]);
      switch (code) {
        case 100:  //trying (not used by Asterisk)
          lines[notifyLine].status = "XFER Trying";
          break;
        case 180:  //ringing
        case 183:  //ringing (not used by Asterisk)
          lines[notifyLine].status = "XFER Ringing";
          break;
        case 200:  //refer successful
        case 202:  //accepted (not used by Asterisk)
          lines[notifyLine].status = "XFER Success";
          lines[notifyLine].incall = false;
          break;
        case 404:  //refer failed
        default:
          lines[notifyLine].status = "XFER Failed (" + code + ")";
          break;
      }
      if (line == notifyLine) gui.updateLine();
      return;
    }
    JFLog.log("Warning : unknown NOTIFY type : " + event);
  }

  /** SIPClientInterface.onAck() : triggered when server send an ACK to jfphone. */

  public void onAck(SIPClient sip, String callid, SDP sdp) {
    if (sdp == null) return;
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (pl.sip == sip) {
        if (!pl.rtpStarted) {
          //RFC 3665 section 3.6 - ACK provides SDP instead of INVITE
          pl.sdp = sdp;
          startRTPinbound();
        }
      }
    }
  }

  /** SIPClientInterface.onMessage() : triggered when server sends a message. */

  public void onMessage(SIPClient client, String callid, String fromid, String fromnumber, String[] msg) {
    gui.chatAdd(fromnumber, msg);
  }

  /** Processes keyboard input. */

  public boolean dispatchKeyEvent(KeyEvent e) {
    if (line == -1) return false;
    Object awtsrc = e.getSource();
    if (!(awtsrc instanceof JComponent)) return false;
    JComponent src = (JComponent)awtsrc;
    if (src.getParent() != (Object)this) return false;
//    JFLog.log("KeyEvent : KeyCode=" + e.getKeyCode() + " KeyChar=" + e.getKeyChar() + " Mods=" + (e.getModifiersEx() & JFAWT.KEY_MASKS) + " ID=" + e.getID());
    int id = e.getID();
    char ch = e.getKeyChar();
    int cc = e.getKeyCode();
    PhoneLine pl = lines[line];
    switch (id) {
      case KeyEvent.KEY_TYPED:
        if ((ch >= '0') && (ch <= '9')) addDigit(ch);
        if (ch == '*') addDigit(ch);
        if (ch == '#') addDigit(ch);
        if (ch == '/') addDigit('#');  //for keypad usage
        break;
      case KeyEvent.KEY_PRESSED:
        if (pl.xfer) {
          if (cc == KeyEvent.VK_ESCAPE) {pl.dial = ""; doXfer();}
          if (ch == KeyEvent.VK_ENTER) doXfer();
          break;
        }
        if ((ch >= '0') && (ch <= '9')) pressDigit(ch);
        switch (ch) {
          case '*': pressDigit(ch); break;
          case '#': pressDigit(ch); break;
          case '/': pressDigit('#'); break;  //for keypad usage
          case KeyEvent.VK_ENTER: {
            if (!pl.incall) call(); else pressDigit('#');
            break;
          }
        }
        switch (cc) {
          case KeyEvent.VK_BACK_SPACE: addDigit((char)cc); break;
          case KeyEvent.VK_ESCAPE: {
            if (!pl.incall) {pl.dial = ""; gui.updateLine();} else pressDigit('*');
            break;
          }
          case KeyEvent.VK_F1: gui.selectLine(0); break;
          case KeyEvent.VK_F2: gui.selectLine(1); break;
          case KeyEvent.VK_F3: gui.selectLine(2); break;
          case KeyEvent.VK_F4: gui.selectLine(3); break;
          case KeyEvent.VK_F5: gui.selectLine(4); break;
          case KeyEvent.VK_F6: gui.selectLine(5); break;
        }
        break;
      case KeyEvent.KEY_RELEASED:
        if (pl.xfer) break;
        if ((ch >= '0') && (ch <= '9')) releaseDigit(ch);
        if (ch == '*') releaseDigit(ch);
        if (ch == '#') releaseDigit(ch);
        if (ch == '/') releaseDigit('#');  //for keypad usage
        if (ch == KeyEvent.VK_ENTER) {
          if (pl.incall) releaseDigit('#');
        }
        if (cc == KeyEvent.VK_ESCAPE) releaseDigit('*');
        break;
    }
    return false;  //pass on as normal
  }

//interface RTPInterface

  /** RTPInterface.rtpDigit() */
  public void rtpDigit(RTPChannel rtp, char digit) {}
  /** RTPInterface.rtpSamples() */
  public void rtpSamples(RTPChannel rtp) {}
  /** RTPInterface.rtpPacket() */
  public void rtpPacket(RTPChannel rtp, int codec, byte data[], int off, int len) {
    switch (codec) {
      case CodecType.H263: rtpH263(rtp, data, off, len); break;
      case CodecType.H263_1998: rtpH263_1998(rtp, data, off, len); break;
      case CodecType.H263_2000: rtpH263_2000(rtp, data, off, len); break;
      case CodecType.H264: rtpH264(rtp, data, off, len); break;
      case CodecType.H265: rtpH265(rtp, data, off, len); break;
      case CodecType.VP8: rtpVP8(rtp, data, off, len); break;
      case CodecType.VP9: rtpVP9(rtp, data, off, len); break;
      case CodecType.JPEG: rtpJPEG(rtp, data, off, len); break;
    }
  }
  /** rtpH263() */
  public void rtpH263(RTPChannel rtp, byte data[], int off, int len) {
    rtp_h263_receive(rtp, data, off, len);
  }
  /** rtpH263_1998() */
  public void rtpH263_1998(RTPChannel rtp, byte data[], int off, int len) {
    rtp_h263_1998_receive(rtp, data, off, len);
  }
  /** rtpH263() */
  public void rtpH263_2000(RTPChannel rtp, byte data[], int off, int len) {
    rtp_h263_2000_receive(rtp, data, off, len);
  }
  /** rtpH264() */
  public void rtpH264(RTPChannel rtp, byte data[], int off, int len) {
    rtp_h264_receive(rtp, data, off, len);
  }
  /** rtpH265() */
  public void rtpH265(RTPChannel rtp, byte data[], int off, int len) {
    rtp_h265_receive(rtp, data, off, len);
  }
  /** rtpVP8() */
  public void rtpVP8(RTPChannel rtp, byte data[], int off, int len) {
    rtp_vp8_receive(rtp, data, off, len);
  }
  /** rtpVP9() */
  public void rtpVP9(RTPChannel rtp, byte data[], int off, int len) {
    rtp_vp9_receive(rtp, data, off, len);
  }
  /** rtpJPEG() */
  public void rtpJPEG(RTPChannel rtp, byte data[], int off, int len) {
    rtp_jpeg_receive(rtp, data, off, len);
  }
  /** RTPInterface.rtpInactive() */
  public void rtpInactive(RTPChannel rtp) {
    //find line this belongs to
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      if (!pl.talking) continue;
      for(int r=0;r<pl.audioRTP.channels.size();r++) {
        if (pl.audioRTP.channels.get(a) == rtp) {
          pl.sip.bye(pl.callid);
          endLine(a);
          pl.status = "Hangup (audio inactive)";
          if (line == a) gui.updateLine();
          return;
        }
      }
    }
  }

  /** ActionListener : actionPerformed() - for the SystemTray Icon actions. */

  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == exit) {
      unRegisterAll();
      BasePhone.unlockFile();
      System.exit(0);
    }
    if (o == show) {
      wc.setPanelVisible();
    }
  }

  public void setLAF() {
//    LookAndFeel laf = UIManager.getLookAndFeel();
//    JFLog.log("current laf=" + laf);
    try { UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); } catch (Exception e) { JFLog.log(e); }  //only acceptable LAF
//    try { UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception e) { JFLog.log(e); }  //crud
//    try { UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); } catch (Exception e) { JFLog.log(e); }  //crud
//    try { UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); } catch (Exception e) { JFLog.log(e); }  //crud
/*
    UIManager.LookAndFeelInfo infos[] = UIManager.getInstalledLookAndFeels();
    for(int a=0;a<infos.length;a++) {
      JFLog.log("infos[] = " + infos[a]);
    }
    UIManager.setLookAndFeel();
*/
  }

  public void selectLine(String ln) {
    gui.selectLine(JF.atoi(ln));
  }

  /*
   * These functions are for calling functions from outside the EDT. (ie: javascript)
   * and also preserve the applet's elevated security permissions (was not easy)
   * It seems like when an applet is called from javascript it's called within a lowered
   * security context, so I had to create a sort of RPC system to get around that.
   */

  private static Method method;
  private static Object object;
  private static String param;
  private static Object lock = new Object();
  private static Object lockReturn = new Object();
  private static String retValue;
  private static Vector<String> funcs = new Vector<String>();

  public void initRPC() {
    object = this;
    new RPCServer().start();
  }

  public static class RPCServer extends Thread {
    public void run() {
      synchronized(lock) {
        while (true) {
          try {lock.wait();} catch (Exception e) {JFLog.log(e);}
          if (funcs.size() == 0) continue;
          String func = funcs.remove(0);
          if (func.startsWith("=")) {
            try {
              method = object.getClass().getMethod(func.substring(1));
              EventQueue.invokeLater(new Runnable() {
                public void run() {
                  try {
                    retValue = (String)method.invoke(object);
                  } catch (InvocationTargetException e) {
                    JFLog.log(e.getCause());
                  } catch (Exception e) {
                    JFLog.log(e);
                  }
                  try {
                    synchronized(lockReturn) {
                      lockReturn.notifyAll();
                    }
                  } catch (Exception e) {
                    JFLog.log(e);
                  }
                }
              });
            } catch (Exception e) {
              JFLog.log(e);
            }
            continue;
          }
          int idx = func.indexOf(",");
          if (idx == -1) {
            try {
              method = object.getClass().getMethod(func);
              EventQueue.invokeLater(new Runnable() {
                public void run() {
                  try {
                    method.invoke(object);
                  } catch (InvocationTargetException e) {
                    JFLog.log(e.getCause());
                  } catch (Exception e) {
                    JFLog.log(e);
                  }
                }
              });
            } catch (Exception e) {
              JFLog.log(e);
            }
          } else {
            param = func.substring(idx+1);
            func = func.substring(0, idx);
            try {
              method = object.getClass().getMethod(func, String.class);
              EventQueue.invokeLater(new Runnable() {
                public void run() {
                  try {
                    method.invoke(object, param);
                  } catch (InvocationTargetException e) {
                    JFLog.log(e.getCause());
                  } catch (Exception e) {
                    JFLog.log(e);
                  }
                }
              });
            } catch (Exception e) {
              JFLog.log(e);
            }
          }
        }
      }
    }
  }

  public void callEDT(String func) {
    synchronized(lock) {
      funcs.add(func);
      lock.notifyAll();
    }
  }

  public void callEDT(String func, String param) {
    this.param = param;
    synchronized(lock) {
      funcs.add(func + "," + param);
      lock.notifyAll();
    }
  }

  public String callEDTreturn(String func) {
    synchronized(lockReturn) {
      retValue = "";
      synchronized(lock) {
        funcs.add("=" + func);
        lock.notifyAll();
      }
      try { lockReturn.wait(); } catch (Exception e) {}
      return retValue;
    }
  }

  private synchronized int getlocalport() {
    int port = sipnext++;
    if (sipnext > sipmax) sipnext = sipmin;
    return port;
  }

  public void setSIPPortRange(int min, int max) {
    sipmin = min;
    sipmax = max;
    sipnext = min;
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
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public RemoteCamera addRemoteCamera(PhoneLine pl, RTPChannel channel) {
    RemoteCamera camera = new RemoteCamera(channel, pl.videoWindow);
    pl.remoteCameras.add(camera);
    camera.start();
    return camera;
  }
  public void delRemoteCamera(PhoneLine pl, RemoteCamera camera) {
    pl.remoteCameras.remove(camera);
    camera.cancel();
  }
  public RemoteCamera findRemoteCamera(RTPChannel channel) {
    for(int a=0;a<6;a++) {
      PhoneLine pl = lines[a];
      synchronized(pl.remoteCamerasLock) {
        for(int b=0;b<pl.remoteCameras.size();b++) {
          if (pl.remoteCameras.get(b).channel == channel) {
            return pl.remoteCameras.get(b);
          }
        }
      }
    }
    return null;
  }
  private static byte crt[], privateKey[];
  private static String fingerprintSHA256;
  protected void initDTLS() {
    String password = "password";
    try {
      FileInputStream fis = new FileInputStream(JF.getUserPath() + "/.jfphone.key");
      KeyMgmt key = new KeyMgmt();
      key.open(fis, password);
      fis.close();
      crt = key.getCRT("jfphone").getEncoded();
      fingerprintSHA256 = KeyMgmt.fingerprintSHA256(crt);
      ArrayList<byte[]> chain = new ArrayList<byte[]>();
      chain.add(crt);
      java.security.cert.Certificate root = key.getCRT("root");
      if (root != null) {
        chain.add(root.getEncoded());
      }
      privateKey = key.getKEY("jfphone", password).getEncoded();
      SRTPChannel.initDTLS(chain, privateKey, false);
    } catch(FileNotFoundException e) {
      //do nothing
    } catch(Exception e) {
      JFLog.log(e);
    }
  }
  //see ticket # 23
  public void pressDigit(String digit) {
    pressDigit(digit.charAt(0));
  }
  //see ticket # 23
  public void releaseDigit(String digit) {
    releaseDigit(digit.charAt(0));
  }

  private static JFLockFile lockFile = new JFLockFile();

  public static boolean lockFile() {
    return lockFile.lock(JF.getUserPath() + "/.jfphone.lck");
  }

  public static void unlockFile() {
    lockFile.unlock();
  }
}
