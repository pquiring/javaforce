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
  private BuildTools tools;
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:GenDMG build.xml");
      System.exit(1);
    }
    try {
      new GenDMG().run(args[0]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void run(String buildfile) throws Exception {
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);
    String home = tools.getProperty("home");
    String app = tools.getProperty("app");
    String apptype = tools.getProperty("apptype");
    String version = tools.getProperty("version");
    String jre = tools.getProperty("jre");
    String ffmpeg = tools.getProperty("ffmpeg_home");

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
    if (!BuildTools.checkFiles("macfiles.lst")) {
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
}
