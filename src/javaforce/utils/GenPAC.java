package javaforce.utils;

/** GenPAC
 *
 * Generates Arch Linux .pac files.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenPAC {
  private static XML xml;
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("Usage:GenPAC build.xml");
      System.exit(1);
    }
    String files = "files.lst";
    if (new File("files-pac.lst").exists()) {
      files = "files-pac.lst";
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

    String out = app + apptype + "-" + version + "-" + archext + ".pkg.tar.xz";

    String files_tmp = ".files.tmp";
    String data = ".MTREE";

    Runtime rt = Runtime.getRuntime();
    try {
      GenPkgInfo.main(new String[] {"arch", arch, files});
      new File(files_tmp).delete();
      JF.echoAppend(".PKGINFO\n", files_tmp);
      JF.echoAppend(".MTREE\n", files_tmp);
      JF.copyAllAppend(files, files_tmp);
      rt.exec(new String[] {
        "bsdtar",
        "-czf",
        data,
        "--format=mtree",
        "--options='!all,use-set,type,uid,gid,mode,time,size,md5,sha256,link'",
        "-T",
        files
      }).waitFor();
      rt.exec(new String[] {"tar", "cf", out, "-T", files_tmp}).waitFor();
      new File(data).delete();
      new File(".PKGINFO").delete();
      new File(data).delete();
      new File(files_tmp).delete();
      System.out.println(out + " created!");
      if (new File(home + "/repo/arch/readme.txt").exists()) {
        if (!JF.moveFile(out, home + "/repo/arch/" + archext + "/" + out)) throw new Exception("move failed");
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
    //arch uses GNU names
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
