package javaforce.utils;

/** GenMSI
 *
 * Generates Windows MSI file (wixtoolset)
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenMSI {
  private BuildTools tools;
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:GenMSI build.xml");
      System.exit(1);
    }
    try {
      new GenMSI().run(args[0]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void run(String buildfile) throws Exception {
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);
    String home = tools.getProperty("home");
    String app = tools.getProperty("app");
    String apptype = tools.getProperty("apptype");
    String version = tools.getProperty("version");
    String jre = tools.getProperty("jre");
    jre = jre.replaceAll("\\$\\{home\\}", home);
    String msi = tools.getProperty("msi");
    if (msi.length() == 0) msi = app;
    String heat_home = tools.getProperty("heat_home");
    if (heat_home.length() == 0) heat_home = "jre";
    String candle_extra = tools.getProperty("candle_extra");
    String light_extra = tools.getProperty("light_extra");
    String ffmpeg_version = tools.getVersion("ffmpeg-version");
    String ffmpeg_folder = home + "/ffmpeg-bin/" + ffmpeg_version;

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
        String[] cmd = new String[] {"wix","build","-arch","x64","-ext","WixToolset.UI.wixext","-ext","WixToolset.Firewall.wixext","-ext","WixToolset.Util.wixext","-o","app.wixlib","wix64.xml"};
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
        ShellProcess.Output output = ShellProcess.exec(new String[] {"wix","build","-arch","x64","-o","jre.wixlib","jre.xml"}, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //create ffmpeg object
        WixHeat.main(new String[] {ffmpeg_folder, "ffmpeg.xml", "FFMPEG", "."});
        ShellProcess.Output output = ShellProcess.exec(new String[] {"wix","build","-arch","x64","-o","ffmpeg.wixlib","ffmpeg.xml"}, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //create msvcrt object
        WixHeat.main(new String[] {home + "/jre_base/bin", "msvcrt.xml", "MSVCRT", ".", "**/api*.dll", "**/msvc*.dll", "**/ucrtbase.dll", "**/vcruntime*.dll"});
        ShellProcess.Output output = ShellProcess.exec(new String[] {"wix","build","-arch","x64","-o","msvcrt.wixlib","msvcrt.xml"}, true);
        if (output == null) throw new Exception("error");
        System.out.println(output.stdout);
        if (output.errorLevel > 0) throw new Exception("error");
      }

      {
        //create msi file
        //see https://wixtoolset.org/docs/fourthree/faqs/
        //see https://wixtoolset.org/docs/tools/wixexe/
        String[] cmd = new String[] {"wix","build","-arch","x64","-ext","WixToolset.UI.wixext","-ext","WixToolset.Firewall.wixext","-ext","WixToolset.Util.wixext","-culture","en-us"
          ,"-b",home,"-b",jre,"-b",jre + "/bin","-b",ffmpeg_folder,"-bv", "WixUILicenseRtf=" + home + "/license.rtf"
          ,"-o",out,"app.wixlib","jre.wixlib","ffmpeg.wixlib","msvcrt.wixlib"};
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
        new File("app.wixlib").delete();
        new File("jre.wixlib").delete();
        new File("jre.xml").delete();
        new File("ffmpeg.wixlib").delete();
        new File("ffmpeg.xml").delete();
        new File("msvcrt.wixlib").delete();
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
}
