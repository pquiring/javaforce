/*
 * Settings.java
 *
 * Created on August 2, 2007, 7:56 PM
 *
 * @author pquiring
 */

import java.awt.Font;
import java.awt.Color;

import javaforce.*;
import javaforce.jni.*;
import javaforce.jni.win.*;
import javaforce.jni.lnx.*;

public class Settings {
  public static Settings settings = new Settings();
  public static class Site {
    public String name, host, protocol, port, username, password, sshkey;
    public int sx, sy;
    public boolean x11, autoSize, localEcho, utf8;
  }
  public static class Folder {
    public Folder() {
      name = "My Sites";
      site = new Site[0];
      folder = new Folder[0];
    }
    public String name;
    public Site site[];
    public Folder folder[];
  }

  public Settings() {
    //try some font metrics adjustments
    if (JF.isWindows()) {
      fontHeight = -1;
    } else {
      fontDescent = 1;
    }
  }

  public static Font fnt = JFAWT.getMonospacedFont(0, 12);
  public int cols = 80;
  public int rows = 24;
  public int scrollBack = 1000;
  public Color foreColor = new Color(0x000000);  //black
  public Color backColor = new Color(0xffffff);  //white
  public Color cursorColor = new Color(0x0000ff);  //black
  public Color selectColor = new Color(0x777777);  //grey
  public int fontSize = 12;
  public String termType = "xterm";
  public int tabStops = 8;
  public int WindowXSize = 800;
  public int WindowYSize = 600;
  public int WindowXPos = 0;
  public int WindowYPos = 0;
  public boolean bWindowMax = false;
  public String termApp = "bash";  //local
  public int fontWidth = 0, fontHeight = 0, fontDescent = 0;  //adjustments
  public Folder sites = new Folder();

  public static boolean hasComm = false;  //support com ports

  public static void loadSettings() {
    String fn = JF.getUserPath() + "/.jfterm.xml";
    XML xml = new XML();
    xml.read(fn);
    XML.XMLTag tag = xml.getTag(new Object[] {"jfterm", "settings"});
    if (tag == null) return;  //no settings found
    xml.writeClass(tag, settings);
    fnt = JFAWT.getMonospacedFont(0, settings.fontSize);
  }

  public static void saveSettings() {
    String fn = JF.getUserPath() + "/.jfterm.xml";
    XML xml = new XML();
    xml.root.setName("jfterm");
    XML.XMLTag tag = xml.addTag(xml.root, "settings", "", "");
    xml.readClass(tag, settings);
    xml.write(fn);
    fnt = JFAWT.getMonospacedFont(0, settings.fontSize);
  }

  static {
    if (JF.isWindows()) {
      hasComm = WinCom.init();
    } else {
      hasComm = LnxCom.init();
      LnxPty.init();
    }
  }
}
