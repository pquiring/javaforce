package javaforce.utils;

/** GenRPM
 *
 * Generates RedHat .rpm files.
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.file.*;

import javaforce.*;

public class GenRPM {
  public static void main(String args[]) {
    if (args.length != 3) {
      System.out.println("Usage:GenRPM app version home");
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
    String arch = getArch();
    String archext = getArchExt();
    String out = args[0] + "-" + args[1] + "-1." + archext + ".rpm";
    String home = args[2];

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
      rt.exec(new String[] {"tar", "xjf", data, "-C", tmpdir, "."}).waitFor();
      new File(data).delete();
      //Warning : rpmbuild nukes the buildroot
      rt.exec(new String[] {"rpmbuild", "-bb", "rpm.spec", "--buildroot", tmpdir}).waitFor();
      JF.deletePathEx(tmpdir);
      new File("rpm.spec").delete();
      rt.exec(new String[] {"mv", "/root/rpmbuild/RPMS/" + archext + "/*.rpm", ".", tmpdir}).waitFor();
      System.out.println(out + " created!");
      Files.copy(new File(out).toPath(), new File(home + "/repo/fedora/" + out).toPath(), StandardCopyOption.REPLACE_EXISTING);
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static String getArch() {
    String arch = System.getenv("HOSTTYPE");
    switch (arch) {
      case "x86_64": return "amd64";
      case "aarch64": return "arm64";
    }
    return arch;
  }

  public static String getArchExt() {
    return getArch();
  }
}
