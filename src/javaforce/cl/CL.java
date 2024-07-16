package javaforce.cl;

/** CL
 *
 * @author peter.quiring
 */

import java.io.*;

import javaforce.*;
import javaforce.jni.*;

public class CL {
  private native static boolean ninit(String opencl);
  public static boolean init() {
    File[] sysFolders;
    String ext = "";
    String apphome = System.getProperty("java.app.home");
    String name = "OpenCL";
    if (apphome == null) apphome = ".";
    if (JF.isWindows()) {
      sysFolders = new File[] {new File(System.getenv("windir") + "\\system32"), new File(apphome), new File(".")};
      ext = ".dll";
      name = name.toLowerCase();
    } else if (JF.isMac()) {
      sysFolders = new File[] {new File("/System/Library/Frameworks/OpenCL.framework/Versions/Current/OpenCL"), new File(apphome), new File(".")};
      ext = ".dylib";
    } else {
      sysFolders = new File[] {new File("/usr/lib"), new File(LnxNative.getArchLibFolder())};
      ext = ".so";
    }
    Library[] libs = {new Library(name)};
    JFNative.findLibraries(sysFolders, libs, ext, libs.length);
    if (libs[0].path == null) {
      JFLog.log("Error:Unable to find OpenCL library");
      return false;
    }
    return ninit(libs[0].path);
  }

  public static void main(String[] args) {
    if (!init()) {
      JFLog.log("OpenCL init failed");
      System.exit(1);
    }

  }
}
