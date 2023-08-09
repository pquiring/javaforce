package javaforce.utils;

/** GenDEB
 *
 * Generates Debian .deb files.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenDEB {
  private static XML xml;
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("Usage:GenDEB build.xml");
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
    String arch = getArch();
    String archext = getArchExt();

    xml = loadXML(args[0]);
    String app = getProperty("app");
    String apptype = getProperty("apptype");
    String version = getProperty("version");
    String home = getProperty("home");

    if (version.equals("${javaforce-version}")) {
      xml = loadXML("versions.xml");
      version = getProperty("javaforce-version");
    }

    switch (apptype) {
      case "client":
      case "server":
        apptype = "-" + apptype;
        break;
      default:
        apptype = "";
        break;
    }

    String out = app + apptype + "-" + version + "_" + archext + ".deb";

    String control = "control.tar.gz";
    String data = "data.tar.bz2";
    String debian_binary = "debian-binary";

    Runtime rt = Runtime.getRuntime();
    int ret;
    try {
      GenPkgInfo.main(new String[] {"debian", arch, files});
      if (new File(control).exists()) {
        new File(control).delete();
      }
      ret = rt.exec(new String[] {"tar", "czf", control, "-C", "deb", "."}).waitFor();
      if (ret != 0) throw new Exception("Failed to build control.tar.gz : ret = " + ret);
      if (new File(data).exists()) {
        new File(data).delete();
      }
      ret = rt.exec(new String[] {"tar", "cjf", data, "-T", files}).waitFor();
      if (ret != 0) throw new Exception("Failed to build data.tar.bz2 : ret = " + ret);
      if (new File(out).exists()) {
        new File(out).delete();
      }
      FileOutputStream bin = new FileOutputStream(debian_binary);
      bin.write("2.0\n".getBytes());
      bin.close();
      //NOTE : debian-binary MUST be listed first
      ret = rt.exec(new String[] {"ar", "mc", out, debian_binary, control, data}).waitFor();
      if (ret != 0) throw new Exception("Failed to build " + out + " : ret = " + ret);

      new File(debian_binary).delete();
      new File(control).delete();
      new File(data).delete();
      JF.deletePathEx("deb");

      System.out.println(out + " created!");
      if (new File(home + "/repo/debian/readme.txt").exists()) {
        if (!JF.moveFile(out, home + "/repo/debian/" + arch + "/" + out)) throw new Exception("move failed");
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
    //debian uses custom names
    switch (arch) {
      case "x86_64": return "amd64";
      case "aarch64": return "arm64";
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
