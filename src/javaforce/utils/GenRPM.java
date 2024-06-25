package javaforce.utils;

/** GenRPM
 *
 * Generates RedHat .rpm files.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenRPM {
  private BuildTools tools;
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage:GenRPM build.xml deps");
      System.exit(1);
    }
    try {
      new GenRPM().run(args[0], args[1]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void run(String buildfile, String deps) throws Exception {
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);
    String files = "files.lst";
    if (new File("files-fedora.lst").exists()) {
      files = "files-fedora.lst";
    }
    if (!BuildTools.checkFiles(files)) {
      System.exit(1);
    }
    String arch = getArch();
    String archext = getArchExt();

    String app = tools.getProperty("app");
    String apptype = tools.getProperty("apptype");
    String version = tools.getProperty("version");
    String home = tools.getProperty("home");

    switch (apptype) {
      case "client":
      case "server":
        apptype = "-" + apptype;
        break;
      default:
        apptype = "";
        break;
    }

    String out = app + apptype + "-" + version + "-1." + archext + ".rpm";

    String data = "data.tar.bz2";
    String tmpdir = "/tmp/jfrpm.tmp";

    Runtime rt = Runtime.getRuntime();
    Process p;
    boolean debug = System.getenv("DEBUG") != null;
    try {
      GenPkgInfo.main(new String[] {"fedora", arch, files, deps});
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
      p = rt.exec(new String[] {"rpmbuild", "-bb", "rpm.spec", "--buildroot", tmpdir});
      p.waitFor();
      if (debug) {
        System.out.println(new String(JF.readAll(p.getInputStream())));
      }
      if (!debug) {
        JF.deletePathEx(tmpdir);
        new File("rpm.spec").delete();
      }
      rt.exec(new String[] {"mv", JF.getUserPath() + "/rpmbuild/RPMS/" + archext + "/" + out, "."}).waitFor();
      System.out.println(out + " created!");
      if (new File(home + "/repo/fedora/readme.txt").exists()) {
        if (!JF.moveFile(out, home + "/repo/fedora/" + archext + "/" + out)) throw new Exception("move failed");
      }
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static String getArch() {
    String arch = System.getenv("HOSTTYPE");
    if (arch == null) {
      arch = System.getProperty("os.arch");
      if (arch == null) {
        JFLog.log("Error:Unable to detect CPU from env:HOSTTYPE or property:os.arch");
      }
    }
    //fedora uses GNU names
    switch (arch) {
      case "amd64": return "x86_64";
      case "arm64": return "aarch64";
    }
    return arch;
  }

  public static String getArchExt() {
    return getArch();
  }
}
