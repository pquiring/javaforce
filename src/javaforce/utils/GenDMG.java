package javaforce.utils;

/** GenDMG
 *
 * Generates MacOS .dmg files.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenDMG {
  private static XML xml;
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:GenDMG build.xml");
      System.exit(1);
    }
    xml = loadXML(args[0]);
    String home = getProperty("home");
    String app = getProperty("app");
    String apptype = getProperty("apptype");
    String version = getProperty("version");
    String jre = getProperty("jre");
    String ffmpeg = getProperty("ffmpeg_home");

    switch (apptype) {
      case "client":
      case "server":
        apptype = "-" + apptype;
        break;
      default:
        apptype = "";
        break;
    }

    String out = app + apptype + "-" + version + ".dmg";

    String tmp_contents = "/tmp/" + app + "/" + app + ".app/Contents";
    String tmp_contents_resources = "/tmp/" + app + "/" + app + ".app/Contents/Resources";
    String tmp_contents_macos = "/tmp/" + app + "/" + app + ".app/Contents/MacOS";

    String icon = app + ".icns";

    if (!new File("Info.plist").exists()) {
      System.out.println("Error:Info.plist not found");
      System.exit(1);
    }
    if (!new File("macfiles.lst").exists()) {
      System.out.println("Error:macfiles.lst not found");
      System.exit(1);
    }
    if (!new File(icon).exists()) {
      System.out.println("Error:" + app + ".icns not found");
      System.exit(1);
    }
    Runtime rt = Runtime.getRuntime();
    try {
      if (new File("jre").exists()) {
        rt.exec(new String[] {"rm", "./jre"}).waitFor();
      }
      rt.exec(new String[] {"ln", "-s", jre, "./jre"}).waitFor();
      rt.exec(new String[] {"tar", "cjfh", "data.tar.bz2", "-T", "macfiles.lst", "jre"}).waitFor();
      rt.exec(new String[] {"rm", "./jre"}).waitFor();
      rt.exec(new String[] {"mkdir", "-p", tmp_contents_resources}).waitFor();
      rt.exec(new String[] {"mkdir", "-p", tmp_contents_macos}).waitFor();
      rt.exec(new String[] {"tar", "xjf", "data.tar.bz2", "-C", tmp_contents_macos}).waitFor();
      rt.exec(new String[] {"rm", "data.tar.bz2"}).waitFor();
      rt.exec(new String[] {"cp", "Info.plist", tmp_contents}).waitFor();
      rt.exec(new String[] {"cp", icon, tmp_contents_resources}).waitFor();
      if (ffmpeg.length() > 0) {
        File[] files = new File(ffmpeg).listFiles();
        for(int a=0;a<files.length;a++) {
          if (files[a].isDirectory()) continue;
          rt.exec(new String[] {"cp", files[a].getAbsolutePath(), tmp_contents_macos}).waitFor();
        }
      }
      if (new File("jfnative64.dylib").exists()) {
        rt.exec(new String[] {"cp", "jfnative64.dylib", tmp_contents_macos}).waitFor();
      }

      if (System.getProperty("genisoimage") == null) {
        rt.exec(new String[] {"hdiutil", "create", "-srcfolder", "/tmp/"+ app, out}).waitFor();
        rt.exec(new String[] {"hdiutil", "internet-enable", "-yes", out}).waitFor();
      } else {
        rt.exec(new String[] {"genisoimage", "-apple", "-r", "-o", out, "/tmp/" + app}).waitFor();
      }
      rt.exec(new String[] {"rm", "-rf", "/tmp/" + app}).waitFor();

      if (new File(home + "/repo/mac/amd64/readme.txt").exists()) {
        if (!JF.moveFile(out, home + "/repo/mac/amd64/" + out)) throw new Exception("move failed");
      }

      System.out.println(out + " created!");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
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
