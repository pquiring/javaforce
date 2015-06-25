package com.jphonelite;

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

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import android.util.*;

import javaforce.*;
import javaforce.voip.*;

/** Keeps current settings and provides methods to load/save settings to an XML file. */

public class Settings implements Cloneable {
  public static Settings current;
  public static class Line implements Cloneable {
    public int same;  //0-5 (-1=disabled) (ignored for lines[0])
    public String user, auth, pass, host;
    public Line() {
      same = 0;
      user = new String();
      auth = new String();
      pass = new String();
      host = new String();
    }
  }
  public Line lines[];
  public String dndCodeOn;
  public String dndCodeOff;
  public boolean usePublish = false;
  public static boolean aa, ac;  //not saved
  public boolean use_g711u = true;  //G.711u (North America)
  public boolean use_g711a = false;  //G.711a (Europe)
  public boolean use_g729a = false;  //requires super FAST cpu (samsung moment 800Mhz too slow - it uses 20ms just to encode one frame)
  public boolean use_g722 = false;  //G.722 (16k) HD
  public boolean speakerMode = false;
  public int speakerThreshold = 1000;  //0-32k
  public int speakerDelay = 1250;  //ms (1.25sec = ouch)
  public boolean reinvite = true;

  //for some reason implementing Cloneable didn't work
  public Settings clone() {
    Settings c = new Settings();
    c.lines = new Line[6];
    for(int a=0;a<6;a++) {
      c.lines[a] = new Line();
      c.lines[a].same = lines[a].same;
      c.lines[a].user = "" + lines[a].user;
      c.lines[a].auth = "" + lines[a].auth;
      c.lines[a].pass = "" + lines[a].pass;
      c.lines[a].host = "" + lines[a].host;
    }
    c.dndCodeOn = "" + dndCodeOn;
    c.dndCodeOff = "" + dndCodeOff;
    c.usePublish = usePublish;
    c.use_g711u = use_g711u;
    c.use_g711a = use_g711a;
    c.use_g729a = use_g729a;
    c.use_g722 = use_g722;
    c.speakerMode = speakerMode;
    c.speakerThreshold = speakerThreshold;
    c.speakerDelay = speakerDelay;
    return c;
  }

  public Settings() {
    super();
    lines = new Line[6];
    for(int a=0;a<6;a++) lines[a] = new Line();
    lines[0].same = -1;
    dndCodeOn = "*78";  //asterisk feature code to activate DND
    dndCodeOff = "*79";  //asterisk feature code to deactivate DND
  }

  public static void loadSettings() {
    String fn = "/sdcard/.jphone.xml";
    current = new Settings();
    try {
      FileReader r = new FileReader(fn);
      SettingsSAX sax = new SettingsSAX();
      Xml.parse(r, sax);
      r.close();
      JFLog.log("loadSettings successful!");
    } catch (Exception e) {
      JFLog.log("loadSettings Exception:", e);
      current = new Settings();
    }
    //validate config
    for(int a=0;a<6;a++) {
      if ((current.lines[a].same > 5) || (current.lines[a].same < -1)) current.lines[a].same = -1;
    }
  }

 private static void write(OutputStream os, String str) throws Exception {
    os.write(str.getBytes());
  }

  public static void saveSettings() {
    String fn = "/sdcard/.jphone.xml";
    try {
      FileOutputStream fos = new FileOutputStream(fn);
      write(fos, "<?xml version=\"1.0\"?>");
      write(fos, "<settings>");
      for(int a=0;a<6;a++) {
        write(fos, "<line>");
        write(fos, "<same>" + current.lines[a].same + "</same>");
        write(fos, "<user>" + current.lines[a].user + "</user>");
        write(fos, "<auth>" + current.lines[a].auth + "</auth>");
        write(fos, "<pass>" + current.lines[a].pass + "</pass>");
        write(fos, "<host>" + current.lines[a].host + "</host>");
        write(fos, "</line>");
      }
      write(fos, "<dndCodeOn>" + current.dndCodeOn + "</dndCodeOn>");
      write(fos, "<dndCodeOff>" + current.dndCodeOff + "</dndCodeOff>");
      write(fos, "<use_g729a>" + current.use_g729a + "</use_g729a>");
      write(fos, "<use_g711u>" + current.use_g711u + "</use_g711u>");
      write(fos, "<use_g711a>" + current.use_g711a + "</use_g711a>");
      write(fos, "<use_g722>" + current.use_g722 + "</use_g722>");
      write(fos, "<speakerMode>" + current.speakerMode + "</speakerMode>");
      write(fos, "<speakerThreshold>" + current.speakerThreshold + "</speakerThreshold>");
      write(fos, "<speakerDelay>" + current.speakerDelay + "</speakerDelay>");
      write(fos, "</settings>");
      fos.close();
    } catch (Exception e) {
      JFLog.log("saveSettings:", e);
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
  public static String getPassword(String pass) {
    if (pass.startsWith("crypto(") && pass.endsWith(")")) {
      if (pass.charAt(8) != ',') return "";  //bad function
      if (pass.charAt(7) != '1') return "";  //unknown crypto type
      try {
        String decoded = decodePassword(pass.substring(9, pass.length() - 1));
        if (decoded == null) decoded = "";
        return decoded;
      } catch (Exception e) {}
    } else {
      return pass;
    }
    return "";
  }

  public String[] getAudioCodecs() {
    ArrayList<String> list = new ArrayList<String>();
    if (use_g729a) list.add(RTP.CODEC_G729a.name);
    if (use_g711u) list.add(RTP.CODEC_G711u.name);
    if (use_g711a) list.add(RTP.CODEC_G711a.name);
    if (use_g722) list.add(RTP.CODEC_G722.name);
    return list.toArray(new String[list.size()]);
  }

  public boolean hasAudioCodec(Codec codec) {
    String codecs[] = getAudioCodecs();
    if (codecs == null) return false;
    for(int a=0;a<codecs.length;a++) {
      if (codecs[a].equals(codec.name)) return true;
    }
    return false;
  }

//DefaultHandler members
  private static class SettingsSAX extends DefaultHandler {
    private int line;
    private enum Tag {
      none, line, dndon, dndoff, same, user, auth, pass, host, speakerMode, speakerThreshold, speakerDelay,
      g729a, g711u, g711a, g722
    }
    private Tag tag;
    public void startDocument () {
      line = -1;
      tag = Tag.none;
    }
    public void endDocument () {}
    public void startElement (String uri, String name, String qName, Attributes atts) {
      if (name.equals("line")) {line++; tag = Tag.line; return;}
      if (name.equals("same")) {tag = Tag.same; return;}
      if (name.equals("user")) {tag = Tag.user; return;}
      if (name.equals("auth")) {tag = Tag.auth; return;}
      if (name.equals("host")) {tag = Tag.host; return;}
      if (name.equals("pass")) {tag = Tag.pass; return;}
      if (name.equals("dndCodeOn")) {tag = Tag.dndon; return;}
      if (name.equals("dndCodeOff")) {tag = Tag.dndoff; return;}
      if (name.equals("use_g729a")) {tag = Tag.g729a; return;}
      if (name.equals("use_g711a")) {tag = Tag.g711a; return;}
      if (name.equals("use_g711u")) {tag = Tag.g711u; return;}
      if (name.equals("use_g722")) {tag = Tag.g722; return;}
      if (name.equals("speakerMode")) {tag = Tag.speakerMode; return;}
      if (name.equals("speakerThreshold")) {tag = Tag.speakerThreshold; return;}
      if (name.equals("speakerDelay")) {tag = Tag.speakerDelay; return;}
    }
    public void endElement (String uri, String name, String qName) {
      tag = Tag.none;
    }
    public void characters (char ch[], int start, int length) {
      String str = new String(ch,start,length);
      switch (tag) {
        case same:
          current.lines[line].same = Integer.valueOf(str);
          break;
        case user:
          current.lines[line].user = str;
          break;
        case auth:
          current.lines[line].auth = str;
          break;
        case host:
          current.lines[line].host = str;
          break;
        case pass:
          current.lines[line].pass = str;
          break;
        case dndon:
          current.dndCodeOn = str;
          break;
        case dndoff:
          current.dndCodeOff = str;
          break;
        case g729a:
          current.use_g729a = str.equals("true");
          break;
        case g711a:
          current.use_g711a = str.equals("true");
          break;
        case g711u:
          current.use_g711u = str.equals("true");
          break;
        case g722:
          current.use_g722 = str.equals("true");
          break;
        case speakerMode:
          current.speakerMode = str.equals("true");
          break;
        case speakerThreshold:
          try {
            current.speakerThreshold = Integer.valueOf(str);
          } catch (Exception e1) {
            current.speakerThreshold = 1000;
          }
          break;
        case speakerDelay:
          try {
            current.speakerDelay = Integer.valueOf(str);
          } catch (Exception e1) {
            current.speakerDelay = 1250;
          }
          break;
      }
    }
  }
}
