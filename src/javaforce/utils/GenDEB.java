package javaforce.utils;

/** GenDEB
 *
 * Generates Debian .deb files.
 *
 * @author pquiring
 */

import java.io.*;
import javaforce.JF;

public class GenDEB {
  public static void main(String args[]) {
    if (args.length < 3) {
      System.out.println("Usage:GenDEB output.deb arch");
      System.exit(1);
    }
    String files = "files.lst";
    if (new File("files-debian.lst").exists()) {
      files = "files-debian.lst";
    }
    if (!new File(files).exists()) {
      System.out.println("Error:files.lst not found");
      System.exit(1);
    }
    String out = args[0];
    String arch = args[1];

    String control = "control.tar.gz";
    String data = "data.tar.bz2";
    String debian_binary = "debian-binary";

    Runtime rt = Runtime.getRuntime();
    try {
      GenPkgInfo.main(new String[] {"debian", arch, files});
      if (new File(control).exists()) {
        new File(control).delete();
      }
      rt.exec(new String[] {"tar", "czf", control, "-C", "deb"}).waitFor();
      if (new File(data).exists()) {
        new File(data).delete();
      }
      rt.exec(new String[] {"tar", "cjf", data, "-T", files}).waitFor();
      if (new File(out).exists()) {
        new File(out).delete();
      }
      FileOutputStream bin = new FileOutputStream(debian_binary);
      bin.write("2.0\n".getBytes());
      bin.close();
      //NOTE : debian-binary MUST be listed first
      rt.exec(new String[] {"ar", "mc", out, debian_binary, control, data}).waitFor();

      new File(debian_binary).delete();
      new File(control).delete();
      new File(data).delete();
      JF.deletePathEx("deb");

      System.out.println(out + " created!");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
