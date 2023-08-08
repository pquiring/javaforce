package javaforce.utils;

/** GenMSI
 *
 * Generates Windows MSI file (wixtoolset)
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class GenMSI {
  private static XML xml;
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("Usage:GenMSI build.xml");
      System.exit(1);
    }

    xml = loadXML(args[0]);
    String home = getProperty("home");
    String app = getProperty("app");
    String apptype = getProperty("apptype");
    String version = getProperty("version");
    String jre = getProperty("jre");
    jre = jre.replaceAll("\\$\\{home\\}", home);
    String msi = getProperty("msi");
    if (msi.length() == 0) msi = app;
    String heat_home = getProperty("heat_home");
    if (heat_home.length() == 0) heat_home = "jre";
    String candle_extra = getProperty("candle_extra");
    String light_extra = getProperty("light_extra");

    switch (apptype) {
      case "client":
      case "server":
        apptype = "-" + apptype;
        break;
      default:
        apptype = "";
        break;
    }

    String out = msi + apptype + "-" + version + "-win64.msi";
    String pdb = msi + apptype + "-" + version + "-win64.wixpdb";

    try {
      {
        //create wix object
        String[] cmd = new String[] {"candle","-ext","WixFirewallExtension","-ext","WixUtilExtension","-o","wix.obj","wix64.xml"};
        if (candle_extra.length() > 0) {
          String[] extra = candle_extra.split(" ");
          for(String x : extra) {
            cmd = JF.copyOfInsert(cmd, cmd.length, x);
          }
        }
        ShellProcess.Output output = ShellProcess.exec(cmd, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //create jre object
        WixHeat.main(new String[] {jre, "jre.xml", "JRE", heat_home});
        ShellProcess.Output output = ShellProcess.exec(new String[] {"candle","-o","jre.obj","jre.xml"}, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //create ffmpeg object
        WixHeat.main(new String[] {home + "/ffmpeg", "ffmpeg.xml", "FFMPEG", "."});
        ShellProcess.Output output = ShellProcess.exec(new String[] {"candle","-o","ffmpeg.obj","ffmpeg.xml"}, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //create msvcrt object
        WixHeat.main(new String[] {home + "/jre_base/bin", "msvcrt.xml", "MSVCRT", ".", "**/api*.dll", "**/msvc*.dll", "**/ucrtbase.dll", "**/vcruntime*.dll"});
        ShellProcess.Output output = ShellProcess.exec(new String[] {"candle","-o","msvcrt.obj","msvcrt.xml"}, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //create msi file
        String[] cmd = new String[] {"light","-ext","WixUIExtension","-ext","WixFirewallExtension","-ext","WixUtilExtension","-cultures:en-us"
          ,"-b",home,"-b",jre,"-b",jre + "/bin","-b",home + "/ffmpeg","-dWixUILicenseRtf=" + home + "/license.rtf"
          ,"-o",out,"wix.obj","jre.obj","ffmpeg.obj","msvcrt.obj"};
        if (light_extra.length() > 0) {
          String[] extra = light_extra.split(" ");
          for(String x : extra) {
            cmd = JF.copyOfInsert(cmd, cmd.length, x);
          }
        }
        ShellProcess.Output output = ShellProcess.exec(cmd, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //cleanup
        new File("wix.obj").delete();
        new File("jre.obj").delete();
        new File("jre.xml").delete();
        new File("ffmpeg.obj").delete();
        new File("ffmpeg.xml").delete();
        new File("msvcrt.obj").delete();
        new File("msvcrt.xml").delete();
        new File(pdb).delete();
        if (new File(home + "/repo/windows/amd64/readme.txt").exists()) {
          if (!JF.moveFile(out, home + "/repo/windows/amd64/" + out)) throw new Exception("move failed");
        }
      }

      System.out.println(out + " created!");
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
