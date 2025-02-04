package javaforce.jni;

/** JFNative
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.*;
import java.util.*;

import javaforce.*;

public class JFNative {

  static {
    load();
  }

  private static boolean loaded = false;

  /** Loads the JavaForce native library (optional).
   * This library is not needed if the JavaForce loaders are used.
   */
  public static void load() {
    if (loaded) return;
    String loader = System.getProperty("javaforce.loader");
    if (loader != null) return;
    System.out.println("Loading javaforce native library...");
    String path = JF.getCurrentPath();
    String file = "/jfnative64";
    String ext = "";
    if (JF.isWindows()) {
      ext = ".dll";
    } else if (JF.isMac()) {
      ext = ".dylib";
    } else {
      ext = ".so";
    }
    try {
      System.load(path + file + ext);
    } catch(Throwable t) {
      JFLog.log(t);
    }
    loaded = true;
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
  /** Allocate a Direct ByteBuffer. */
  public native static ByteBuffer allocate(int size);
  /** Free memory backing a Direct ByteBuffer.
   * Failing to call free() will result in memory leak.
   */
  public native static void free(ByteBuffer buffer);
}
