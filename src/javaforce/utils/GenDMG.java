package javaforce.utils;

/** GenDMG
 *
 * Generates MacOS .dmg files.
 *
 * @author pquiring
 */

import java.io.*;

public class GenDMG {
  public static void main(String args[]) {
    if (args.length < 3) {
      System.out.println("Usage:GenDMG app output jre_folder [ffmpeg_folder]");
      System.out.println("  define 'genisoimage' to use genisoimage instead of hdiutil");
      System.exit(1);
    }
    String app = args[0];
    String out = args[1];
    String jre = args[2];
    String ffmpeg = null;
    String tmp_contents = "/tmp/" + app + "/" + app + ".app/Contents";
    String tmp_contents_resources = "/tmp/" + app + "/" + app + ".app/Contents/Resources";
    String tmp_contents_macos = "/tmp/" + app + "/" + app + ".app/Contents/MacOS";
    String icon = app + ".icns";
    if (args.length > 3) {
      ffmpeg = args[3];
      if (ffmpeg.length() == 0) ffmpeg = null;
    }
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
      if (ffmpeg == null) {
        rt.exec(new String[] {"tar", "cjfh", "data.tar.bz2", "-T", "macfiles.lst", "jre"}).waitFor();
      } else {
        rt.exec(new String[] {"tar", "cjfh", "data.tar.bz2", "-T", "macfiles.lst", "jre", "-C", ffmpeg, "."}).waitFor();
      }
      rt.exec(new String[] {"rm", "./jre"}).waitFor();
      rt.exec(new String[] {"mkdir", "-p", tmp_contents_resources}).waitFor();
      rt.exec(new String[] {"mkdir", "-p", tmp_contents_macos}).waitFor();
      rt.exec(new String[] {"tar", "xjf", "data.tar.bz2", "-C", tmp_contents_macos}).waitFor();
      rt.exec(new String[] {"rm", "data.tar.bz2"}).waitFor();
      rt.exec(new String[] {"cp", "Info.plist", tmp_contents}).waitFor();
      rt.exec(new String[] {"cp", icon, tmp_contents_resources}).waitFor();

      if (System.getProperty("genisoimage") == null) {
        rt.exec(new String[] {"hdiutil", "create", "-srcfolder", "/tmp/"+ app, out}).waitFor();
        rt.exec(new String[] {"hdiutil", "internet-enable", "-yes", out}).waitFor();
      } else {
        rt.exec(new String[] {"genisoimage", "-apple", "-r", "-o", out, "/tmp/" + app}).waitFor();
      }
      rt.exec(new String[] {"rm", "-rf", "/tmp/" + app}).waitFor();
      System.out.println(out + " created!");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
