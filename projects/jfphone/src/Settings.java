/*
 * Settings.java
 *
 * Created on Mar 22, 2010, 6:03 PM
 *
 * @author pquiring
 *
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.voip.*;

/** Keeps current settings and provides methods to load/save settings to an XML file. */

public class Settings {
  public static Settings current;
  public static class Line {
    public int same;  //0-5 (-1=disabled) (ignored for lines[0])
    public String name, user, auth, pass, host;
    public boolean disableVideo, srtp, dtls, siplog;
    public int transport;
    public Line() {
      same = 0;
      name = new String();
      user = new String();
      auth = new String();
      pass = new String();
      host = new String();
      disableVideo = false;
      srtp = false;
      dtls = false;
      transport = 0;  //0=UDP 1=TCP 2=TLS
    }
  }
  public int WindowXPos = 0;
  public int WindowYPos = 0;
  public Line lines[];
  public String sipcontacts[] = new String[0];
  public String callLog[] = new String[0];
  public String dndCodeOn = "*78", dndCodeOff = "*79";
  public boolean swVolForce = false;
  public boolean resample441k = false;
  public boolean checkVersion = true;
  public String audioInput = "<default>", audioOutput = "<default>";
  public boolean disableLogging = false;
  public boolean hideWhenMinimized = true;
  public boolean exitWhenClosed = false;
  public boolean alwaysOnTop = false;
  public boolean keepAudioOpen = false;
  public String downloadPath = JF.getUserPath() + "/Downloads";
  public boolean smallerFont = false;  //some JVMs have different fonts
  public String videoDevice = "<default>";
  public String videoResolution = "<default>";
  public String videoPosition = "<default>";
  public int videoFPS = 5;
  public boolean usePublish = false;
  public boolean speakerMode = false;
  public int speakerThreshold = 1000;  //0-32k
  public int speakerDelay = 250;  //ms
  public boolean disableEnhanced = false;
  public String audioCodecs = "G729,PCMU";
  public String videoCodecs = "VP8,H264,H263-1998,H263-2000,JPEG";  //BUG:H263 incomplete
  public boolean reinvite = true;  //reinvite when returned multiple codecs
  public boolean autohold = false;  //auto hold/unhold when switching between active lines
  public int volPlaySW = 75, volRecSW = 75;  //playback / recording vol levels (software)
  public int volPlayHW = 100, volRecHW = 100;  //playback / recording vol levels (hardware) (obsolete - removed in 1.1.0)
  public boolean nativeVideo = false;
  public int sipexpires = 3600;  //in seconds : min=300 (5mins) max=3600 (1hr)
  public boolean sipRange = false;  //specify SIP port range (min 32 ports)
  public int sipmin = 5061, sipmax = 5199;
  public boolean rtpRange = false;  //specify RTP port range (min 128 ports)
  public int rtpmin = 32768, rtpmax = 65535;
  public int nat = 0;  //None by default
  public boolean natPrivate = false;
  public String natHost = "", natUser = "", natPass = "";
  public boolean rport = true;
  public boolean received = true;
  public String inRingtone = "*RING", outRingtone = "*NA";
  public boolean welcome = true;

  //static = do not save these settings
  public static boolean aa;
  public static boolean ac;
  public static boolean isApplet = false;
  public static boolean isLinux = false;
  public static boolean isWindows = false;
  public static boolean isJavaScript = false;
  public static boolean hasFFMPEG = false;

  private void initLines() {
    lines = new Line[6];
    for(int a=0;a<6;a++) lines[a] = new Line();
  }

  public static void loadSettings() {
    String fn = JF.getUserPath() + "/.jfphone.xml";
    try {
      current = new Settings();
      XML xml = new XML();
      xml.setUseUniqueNames(false);
      xml.read(new FileInputStream(fn));
      xml.writeClass(current);

      if (current.lines == null || current.lines.length != 6) throw new Exception("invalid config");

      //force settings
      if (!current.hasAudioCodec(RTP.CODEC_G711a)
        && !current.hasAudioCodec(RTP.CODEC_G711u)
        && !current.hasAudioCodec(RTP.CODEC_GSM)
        && !current.hasAudioCodec(RTP.CODEC_G722)
        && !current.hasAudioCodec(RTP.CODEC_G729a)
         )
      {
        current.audioCodecs = "G729,PCMU";
      }
      if (!current.hasVideoCodec(RTP.CODEC_JPEG)
        && !current.hasVideoCodec(RTP.CODEC_H263)
        && !current.hasVideoCodec(RTP.CODEC_H263_1998)
        && !current.hasVideoCodec(RTP.CODEC_H263_2000)
        && !current.hasVideoCodec(RTP.CODEC_H264)
        && !current.hasVideoCodec(RTP.CODEC_VP8)
         )
      {
        current.videoCodecs = "VP8,H264,H263-1998,H263-2000,JPEG";  //TODO : add 'H263' when complete
      }
      if (current.sipexpires < 300) current.sipexpires = 300;
      if (current.sipexpires > 3600) current.sipexpires = 3600;
      if (current.nat == 1) current.nat = 0;  //beta value (dyndns dropped)

      if (current.welcome) {
        int cnt = 0;
        for(int a=0;a<6;a++) {
          if (current.lines[a].user.length() > 0) cnt++;
        }
        if (cnt > 0) current.welcome = false;
      }

      JFLog.log("loadSettings ok");
    } catch (FileNotFoundException e) {
      JFLog.log("Config file does not exist, using default values.");
      current = new Settings();
      current.initLines();
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
      current.initLines();
    }
  }
  public static void saveSettings() {
    String fn = JF.getUserPath() + "/.jfphone.xml";
    try {
      XML xml = new XML();
      xml.setUseUniqueNames(false);
      xml.readClass("settings", current);
      xml.write(new FileOutputStream(fn));
    } catch (Exception e) {
      JFAWT.showError("Error", "Save failed : " + e);
    }
  }
  private static int maxLog = 25;
  public static void addCallLog(String number) {
    int len = current.callLog.length;
    for(int a=0;a<len;a++) {
      if (current.callLog[a].equals(number)) {
        //move to top of list
        for(int b=a;b > 0;b--) current.callLog[b] = current.callLog[b-1];
        current.callLog[0] = number;
        saveSettings();
        return;
      }
    }
    if (len == maxLog) len = maxLog-1;
    String newcallLog[] = new String[len + 1];
    for(int a=0;a < len;a++) newcallLog[a+1] = current.callLog[a];
    newcallLog[0] = number;
    current.callLog = newcallLog;
    saveSettings();
  }
  public static void setContact(String name, String contact) {
    int len = current.sipcontacts.length;
    int idx;
    String fields[];
    for(int a=0;a<len;a++) {
      fields = SIP.split(current.sipcontacts[a]);
      if (fields[0].equals(name)) {
JFLog.log("setting contact : " + fields[0] + " to " + contact);
        current.sipcontacts[a] = contact;
        return;
      }
    }
JFLog.log("adding contact : " + contact);
    String newContacts[] = new String[len+1];
    for(int a=0;a<len;a++) {newContacts[a] = current.sipcontacts[a];}
    newContacts[len] = contact;
    current.sipcontacts = newContacts;
  }
  public static void delContact(String name) {
    int len = current.sipcontacts.length;
    if (len == 0) return;
    int idx;
    String fields[];
    for(int a=0;a<len;a++) {
      fields = SIP.split(current.sipcontacts[a]);
      if (fields[0].equalsIgnoreCase(name)) {
        int pos = 0;
        String newlist[] = new String[len-1];
        for(int b=0;b<len;b++) {
          if (b==a) continue;
          newlist[pos++] = current.sipcontacts[b];
        }
        current.sipcontacts = newlist;
        return;
      }
    }
  }
  /** Encodes a password with some simple steps. */
  public static String encodePassword(String password) {
    char ca[] = password.toCharArray();
    int sl = ca.length;
    if (sl == 0) return "";
    char tmp;
    for(int p=0;p<sl/2;p++) {
      tmp = ca[p];
      ca[p] = ca[sl-p-1];
      ca[sl-p-1] = tmp;
    }
    StringBuffer out = new StringBuffer();
    for(int p=0;p<sl;p++) {
      ca[p] ^= 0xaa;
      if (ca[p] < 0x10) out.append("0");
      out.append(Integer.toString(ca[p], 16));
    }
//System.out.println("e1=" + out.toString());
    Random r = new Random();
    char key = (char)(r.nextInt(0xef) + 0x10);
    char outkey = key;
    ca = out.toString().toCharArray();
    sl = ca.length;
    for(int p=0;p<sl;p++) {
      ca[p] ^= key;
      key ^= ca[p];
    }
    out = new StringBuffer();
    for(int a=0;a<4;a++) {
      out.append(Integer.toString(r.nextInt(0xef) + 0x10, 16));
    }
    out.append(Integer.toString(outkey, 16));
    for(int p=0;p<sl;p++) {
      if (ca[p] < 0x10) out.append("0");
      out.append(Integer.toString(ca[p], 16));
    }
    for(int a=0;a<4;a++) {
      out.append(Integer.toString(r.nextInt(0xef) + 0x10, 16));
    }
    return out.toString();
  }
  /** Encodes a password. */
  public static String encodePassword(char password[]) {
    return encodePassword(new String(password));
  }
  /** Decodes a password. */
  public static String decodePassword(String crypto) {
    int sl = crypto.length();
    if (sl < 10) return null;
    char key = (char)(int)Integer.valueOf(crypto.substring(8,10), 16);
    char newkey;
    crypto = crypto.substring(10, sl - 8);
    int cl = (sl - 18) / 2;
    char ca[] = new char[cl];
    for(int p=0;p<cl;p++) {
      ca[p] = (char)(int)Integer.valueOf(crypto.substring(p*2, p*2+2), 16);
      newkey = (char)(key ^ ca[p]);
      ca[p] ^= key;
      key = newkey;
    }
    crypto = new String(ca);
//System.out.println("d1=" + crypto);
    cl = crypto.length() / 2;
    ca = new char[cl];
    for(int p=0;p<cl;p++) {
      ca[p] = (char)(int)Integer.valueOf(crypto.substring(p*2, p*2+2), 16);
    }
    for(int p=0;p<cl;p++) {
      ca[p] ^= 0xaa;
    }
    char tmp;
    for(int p=0;p<cl/2;p++) {
      tmp = ca[p];
      ca[p] = ca[cl-p-1];
      ca[cl-p-1] = tmp;
    }
    return new String(ca);
  }
  public static void test() {
    String tst = "testPassword";
    String e = encodePassword(tst);
    System.out.println("e=" + e);
    String d = decodePassword(e);
    System.out.println("d=" + d);
  }
  public static String getPassword(String pass) {
    if (pass.startsWith("crypto(") && pass.endsWith(")")) {
      if (pass.charAt(8) != ',') return "";  //bad function
      if (pass.charAt(7) != '1') return "";  //unknown crypto type
      try {
        return decodePassword(pass.substring(9, pass.length() - 1));
      } catch (Exception e) {}
    } else {
      return pass;
    }
    return "";
  }
  public boolean hasAudioCodec(Codec codec) {
    String codecs[] = audioCodecs.split(",");
    if (codecs == null) return false;
    for(int a=0;a<codecs.length;a++) {
      if (codecs[a].equals(codec.name)) return true;
    }
    return false;
  }
  public String[] getAudioCodecs() {
    return audioCodecs.split(",");
  }
  public boolean hasVideoCodec(Codec codec) {
    String codecs[] = videoCodecs.split(",");
    if (codecs == null) return false;
    for(int a=0;a<codecs.length;a++) {
      if (codecs[a].equals(codec.name)) return true;
    }
    return false;
  }
  public String[] getVideoCodecs() {
    return videoCodecs.split(",");
  }
  private static final int CX = 320;
  private static final int CY = 240;
  public int[] getVideoResolution() {
    String res = videoResolution;
    if ((res == null) || (res.equals("<default>"))) return new int[] {CX,CY};
    int idx = res.indexOf("x");
    if (idx == -1) return new int[] {CX,CY};
    return new int[] {Integer.valueOf(res.substring(0, idx)), Integer.valueOf(res.substring(idx+1))};
  }
}
