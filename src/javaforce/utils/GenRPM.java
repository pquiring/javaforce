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
  private static XML xml;
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("Usage:GenRPM build.xml");
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

    xml = loadXML(args[0]);
    String app = getProperty("app");
    String apptype = getProperty("apptype");
    String version = getProperty("version");
    String home = getProperty("home");

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
      p = rt.exec(new String[] {"rpmbuild", "-bb", "rpm.spec", "--buildroot", tmpdir});
      p.waitFor();
      if (debug) {
        System.out.println(new String(JF.readAll(p.getInputStream())));
      }
      JF.deletePathEx(tmpdir);
      new File("rpm.spec").delete();
      rt.exec(new String[] {"mv", "/root/rpmbuild/RPMS/" + archext + "/" + out, "."}).waitFor();
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

  private static XML loadXML(String buildfile) {
    XML xml = new XML();
    try {
      xml.read(new FileInputStream(buildfile));
      return xml;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  private static String getTag(String name) {
    XML.XMLTag tag = xml.getTag(new String[] {"project", name});
    if (tag == null) return "";
    return tag.content;
  }

  private static String getProperty(String name) {
    //<project> <property name="name" value="value">
    int cnt = xml.root.getChildCount();
    for(int a=0;a<cnt;a++) {
      XML.XMLTag tag = xml.root.getChildAt(a);
      if (!tag.name.equals("property")) continue;
      int attrs = tag.attrs.size();
      String attrName = null;
      String attrValue = null;
      for(int b=0;b<attrs;b++) {
        XML.XMLAttr attr = tag.attrs.get(b);
        if (attr.name.equals("name")) {
          attrName = attr.value;
        }
        if (attr.name.equals("value")) {
          attrValue = attr.value;
        }
        if (attr.name.equals("location")) {
          attrValue = attr.value;
        }
      }
      if (attrName != null && attrName.equals(name)) {
        return attrValue;
      }
    }
    return "";
  }
}
