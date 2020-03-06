package javaforce.utils;

/** GenRPM
 *
 * Generates RedHat .rpm files.
 *
 * @author pquiring
 */

import java.io.*;
import javaforce.JF;

public class GenRPM {
  public static void main(String args[]) {
    if (args.length < 3) {
      System.out.println("Usage:GenRPM output.rpm arch");
      System.exit(1);
    }
    String files = "files.lst";
    if (new File("files-fedora.lst").exists()) {
      files = "files-fedora.lst";
    }
    if (!new File(files).exists()) {
      System.out.println("Error:files.lst not found");
      System.exit(1);
    }
    String out = args[0];
    String arch = args[1];
    String archext = null;
    switch (arch) {
      case "x32": archext = "i686"; break;
      case "x64": archext = "x86_64"; break;
      case "a32": archext = "armv7hl"; break;
      case "a64": archext = "aarch64"; break;
    }
    if (archext == null) {
      System.out.println("Error:Unknown arch type");
      System.exit(1);
    }

    String data = "data.tar.bz2";
    String tmpdir = "/tmp/jfrpm.tmp";

    Runtime rt = Runtime.getRuntime();
    try {
      GenPkgInfo.main(new String[] {"fedora", arch, files});
      JF.copyAllAppend(files, "rpm.spec");
      if (new File(data).exists()) {
        new File(data).delete();
      }
      rt.exec(new String[] {"tar", "cjf", data, "-T", files}).waitFor();
      if (new File(out).exists()) {
        new File(out).delete();
      }
      new File(tmpdir).mkdir();
      rt.exec(new String[] {"tar", "xjf", data, "-C", tmpdir}).waitFor();
      new File(data).delete();
      //Warning : rpmbuild nukes the buildroot
      rt.exec(new String[] {"rpmbuild", "-bb", "rpm.spec", "--buildroot", tmpdir}).waitFor();
      JF.deletePathEx(tmpdir);
      new File("rpm.spec").delete();
      rt.exec(new String[] {"mv", "/root/rpmbuild/RPMS/" + archext + "/*.rpm", ".", tmpdir}).waitFor();
      System.out.println(out + " created!");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
