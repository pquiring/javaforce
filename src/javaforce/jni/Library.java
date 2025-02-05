package javaforce.jni;

/** Native Library
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

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
      return new File[] {new File("/usr/lib"), new File(LnxNative.getArchLibFolder())};
    }
  }
}
