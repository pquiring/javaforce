package javaforce.utils;

/** Generates executable for platform.
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.file.*;

import javaforce.*;

public class GenExecutable {
  public static void copy(String src, String dest) throws Exception {
    Files.copy(
      Paths.get(src),
      Paths.get(dest),
      StandardCopyOption.REPLACE_EXISTING
    );
  }
  public static void chmod(String file) {
    new File(file).setExecutable(true);
  }
  public static void main(String[] args) {
    if (args.length != 5) {
      System.out.println("Usage:GenExecutable home app apptype ico cfg");
      System.exit(1);
    }
    String home = args[0];
    String app = args[1];
    String type = args[2];
    String ico = args[3];
    String cfg = args[4];
    if (type.equals("w")) {
      type = "";
    }
    try {
      if (JF.isWindows()) {
        //windows
        copy(home + "/stubs/win64" + type + ".exe", app + ".exe");
        WinPE.main(new String[] {app + ".exe", ico, cfg});
      } else if (JF.isMac()) {
        //mac
        copy(home + "/stubs/mac64.bin", app);
        chmod(app);
      } else {
        //linux
        copy(home + "/stubs/linux64.bin", app + ".bin");
        ResourceManager.main(new String[] {app + ".bin", cfg});
        chmod("/usr/bin/" + app);
      }
      System.out.println("Native Executable generated!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
