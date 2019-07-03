package javaforce.jni;

import javaforce.*;

/** Loads the native library.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class JFNative {
  static {
    try {
      String path = null;
      String ext = "", bits = "";
      if (JF.is64Bit())
        bits = "64";
      else
        bits = "32";
      if (JF.isWindows()) {
        ext = ".dll";
        path = System.getProperty("java.app.home");
        if (path == null) {
          path = System.getenv("windir");
        }
      } else if (JF.isMac()) {
        ext = ".dylib";
        path = "/usr/lib";
      } else {
        ext = ".so";
        path = "/usr/lib";
      }
      Library lib = new Library("jfnative" + bits);
      if (!findLibraries(new File[] {new File("."), new File(path)}, new Library[] {lib}, ext, 1, false)) {
        JFLog.log("Warning:Unable to find jfnative library");
      }
      if (lib.path != null) {
        System.load(lib.path);
        loaded = true;
      }
    } catch (Throwable t) {
      JFLog.log("Error:" + t);
    }
  }
  public static void load() {}  //ensure native library is loaded
  public static boolean loaded;
  /** Specify if ffmpeg is needed? */
  public static boolean load_ffmpeg = true;

  /** Find native libraries in folder (recursive). */
  public static boolean findLibraries(File folders[], Library libs[], String ext, int needed, boolean recursive) {
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
          if (!recursive) continue;
          if (JF.isUnix()) {
            //Ubuntu has i386 folders on 64bit systems
            String path = file.getName();
            if (JF.is64Bit()) {
              if (path.contains("i386")) continue;
              if (path.contains("i486")) continue;
              if (path.contains("i586")) continue;
              if (path.contains("i686")) continue;
            } else {
              if (path.contains("x86_64")) continue;
            }
          }
          if (findLibraries(new File[] {file}, libs, ext, needed, recursive)) return true;
        } else if (fileName.contains(ext)) {
          int cnt = 0;
          boolean once = false;
          for(int b=0;b<libs.length;b++) {
            if (libs[b].path != null) {
              if (libs[b].once) {
                if (!once) {
                  cnt++;
                  once = true;
                }
              } else {
                cnt++;
              }
            }
            else if (fileName.startsWith(libs[b].name) || fileName.startsWith("lib" + libs[b].name)) {
              JFLog.log("Found Library:" + file.toString());
              libs[b].path = file.getAbsolutePath();
              cnt++;
            }
          }
          if (cnt == needed) return true;
        }
      }
    }
    return false;
  }
}
