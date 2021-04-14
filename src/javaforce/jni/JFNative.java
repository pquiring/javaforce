package javaforce.jni;

import javaforce.*;

/** Loads the native library.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class JFNative {

  /** Loads JavaForce native library. */
  public static void load() {
    if (loaded) return;
    try {
      test();
      JFLog.log("JFNative:Running on executable with native code embedded!");
      loaded = true;
    } catch (Throwable t) {
      JFLog.log("JFNative:Loading external native library!");
      loadNative();
    }
    if (!loaded) return;
    if (JF.isWindows()) {
      WinNative.load();
    }
    if (JF.isUnix() && !JF.isMac()) {
      LnxNative.load();
    }
    if (JF.isMac()) {
      MacNative.load();
    }
  }

  /** Indicates if native library is loaded. */
  public static boolean loaded;
  /** Specify if ffmpeg is needed? */
  public static boolean load_ffmpeg = true;

  private static void loadNative() {
    try {
      String path = System.getProperty("java.app.home");
      if (path == null) {
        path = ".";
      }
      String ext = "", bits = "";
      if (JF.is64Bit())
        bits = "64";
      else
        bits = "32";
      if (JF.isWindows()) {
        ext = ".dll";
      } else if (JF.isMac()) {
        ext = ".dylib";
      } else {
        ext = ".so";
        path = "/usr/lib";
      }
      Library lib = new Library("jfnative" + bits);
      if (!findLibraries(new File[] {new File(path)}, new Library[] {lib}, ext, 1)) {
        JFLog.log("Warning:Unable to find jfnative library");
        JFLog.log("Library Path=" + path);
      }
      if (lib.path != null) {
        System.load(lib.path);
        loaded = true;
      }
    } catch (Throwable t) {
      JFLog.log("Error:" + t);
    }
  }

  private static native void test();

  /** Find native libraries in folder (recursive). */
  public static boolean findLibraries(File folders[], Library libs[], String ext, int needed) {
    boolean once = false;
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
          int cnt = 0;
          for(int b=0;b<libs.length;b++) {
            if (fileName.startsWith(libs[b].name) || fileName.startsWith("lib" + libs[b].name)) {
              if (once && libs[b].once) continue;
              libs[b].path = file.getAbsolutePath();
              cnt++;
              if (libs[b].once) once = true;
            }
          }
          if (cnt == needed) return true;
        }
      }
    }
    return false;
  }
}
