package javaforce.utils;

/** Generates executable for platform.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenExecutable {
  private static XML xml;
  public static void chmod(String file) {
    new File(file).setExecutable(true);
  }
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:GenExecutable build.xml");
      System.exit(1);
    }
    xml = loadXML(args[0]);
    String home = getProperty("home");
    String app = getProperty("app");
    String apptype = getProperty("apptype");
    String ico = getProperty("ico");
    if (ico.length() == 0) {
      ico = app + ".ico";
    }
    String cfg = getProperty("cfg");
    if (cfg.length() == 0) {
      cfg = app + ".cfg";
    }
    try {
      if (JF.isWindows()) {
        //windows
        String ext = "";
        switch (apptype) {
          case "c":  //legacy
          case "console": ext = "c"; break;
          case "s":  //legacy
          case "service": ext = "s"; break;
        }
        if (!JF.copyFile(home + "/stubs/win64" + ext + ".exe", app + ".exe")) {
          throw new Exception("copy error");
        }
        WinPE.main(new String[] {app + ".exe", ico, cfg});
      } else if (JF.isMac()) {
        //mac
        JF.copyFile(home + "/stubs/mac64.bin", app);
        chmod(app);
      } else {
        //linux
        JF.copyFile(home + "/stubs/linux64.bin", app + ".bin");
        ResourceManager.main(new String[] {app + ".bin", cfg});
        chmod("/usr/bin/" + app);
      }
      System.out.println("Native Executable generated!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  private static XML loadXML(String buildfile) {
    XML xml = new XML();
    try {
      xml.read(new FileInputStream(buildfile));
      return xml;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  private static String getTag(String name) {
    XML.XMLTag tag = xml.getTag(new String[] {"project", name});
    if (tag == null) return "";
    return tag.content;
  }

  private static String getProperty(String name) {
    //<project> <property name="name" value="value">
    int cnt = xml.root.getChildCount();
    for(int a=0;a<cnt;a++) {
      XML.XMLTag tag = xml.root.getChildAt(a);
      if (!tag.name.equals("property")) continue;
      int attrs = tag.attrs.size();
      String attrName = null;
      String attrValue = null;
      for(int b=0;b<attrs;b++) {
        XML.XMLAttr attr = tag.attrs.get(b);
        if (attr.name.equals("name")) {
          attrName = attr.value;
        }
        if (attr.name.equals("value")) {
          attrValue = attr.value;
        }
        if (attr.name.equals("location")) {
          attrValue = attr.value;
        }
      }
      if (attrName != null && attrName.equals(name)) {
        return attrValue;
      }
    }
    return "";
  }
}
