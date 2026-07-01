package javaforce.ffm;

import java.io.*;
import java.util.*;

import javaforce.*;

/** Native Library
 *
 * @author pquiring
 */

public class Library {
  public String name;
  public String path;
  public String match;

  public Library(String name) {
    this.name = name;
  }

  /** Get shared library extension. */
  public static String getExt() {
    if (JF.isWindows()) {
      return ".dll";
    } else if (JF.isMac()) {
      return ".dylib";
    } else {
      return ".so";
    }
  }

  /** Get list of folders where system libraries are stored. */
  public static File[] getSysFolders() {
    String apphome = System.getProperty("java.app.home");
    if (apphome == null) apphome = ".";
    if (JF.isWindows()) {
      return new File[] {new File(apphome), new File(System.getenv("appdata") + "/ffmpeg"), new File(".")};
    } else if (JF.isMac()) {
      return new File[] {new File(apphome), new File(".")};
    } else {
      return new File[] {new File("/usr/lib"), new File(getArchLibFolder())};
    }
  }

  /** Find native libraries in folder (recursive). */
  public static boolean findLibraries(File[] folders, Library[] libs, String ext) {
    for(int i=0;i<libs.length;i++) {
      if (JF.isUnix()) {
        libs[i].name = "lib" + libs[i].name;
      }
      libs[i].match = libs[i].name + "([-][0-9]*)?" + ext + "([.][0-9]*)*";
    }
    for(int fn=0;fn<folders.length;fn++) {
      File[] files = folders[fn].listFiles();
      if (files == null || files.length == 0) {
        continue;
      }
      //sort folders last
      Arrays.sort(files, new Comparator<File>() {
        public int compare(File f1, File f2) {
          boolean d1 = f1.isDirectory();
          boolean d2 = f2.isDirectory();
          if (d1 && d2) return 0;
          if (d1) return 1;
          if (d2) return -1;
          return 0;
        }
      });
      for (int a = 0; a < files.length; a++) {
        File file = files[a];
        String fileName = files[a].getName();
        if (file.isDirectory()) {
          continue;
        } else if (fileName.contains(ext)) {
          for(int b=0;b<libs.length;b++) {
            if (fileName.matches(libs[b].match)) {
              //always use longer paths (shorter ones are often ld scripts that dlopen does not understand)
              String path = file.getAbsolutePath();
              if ((libs[b].path == null) || (path.length() > libs[b].path.length())) {
                libs[b].path = path;
                break;
              }
            }
          }
        }
      }
    }
    boolean ok = true;
    for(int i=0;i<libs.length;i++) {
      if (libs[i].path == null) {
        JFLog.log("Unable to find library:" + libs[i].name);
        ok = false;
      }
    }
    return ok;
  }

  /** Returns CPU arch lib folder. */
  public static String getArchLibFolder() {
    if (new File("/usr/lib/x86_64-linux-gnu").exists()) {
      return "/usr/lib/x86_64-linux-gnu";
    }
    if (new File("/usr/lib/aarch64-linux-gnu").exists()) {
      return "/usr/lib/aarch64-linux-gnu";
    }
    if (new File("/usr/lib64").exists()) {
      //Fedora
      return "/usr/lib64";
    }
    JFLog.log("Warning:Arch Lib folder not found!");
    return "/usr/lib";
  }
}
