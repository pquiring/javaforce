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
  @SuppressWarnings("unchecked")
  public static void load() {
    if (loaded) return;
    try {
      test();
      loaded = true;
    } catch (Throwable t) {
    }
    if (!loaded) {
      JFLog.log("Error:JF native methods not found");
      return;
    }
    if (!inited) {
      inited = true;
      if (JF.isUnix() && !JF.isMac()) {
        LnxNative.load();
      }
      if (JF.isMac()) {
        MacNative.load();
      }
    }
  }

  /** Indicates if native library is loaded. */
  public static boolean loaded;
  /** Indicates if native library is inited. */
  public static boolean inited;
  /** Specify if ffmpeg is needed? */
  public static boolean load_ffmpeg = true;

  private static native void test();
  private static native void init();

  /** Find native libraries in folder (recursive). */
  public static boolean findLibraries(File[] folders, Library[] libs, String ext, int needed) {
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
