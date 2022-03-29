package javaforce.utils;

/** VSS - Volume Shadow Service command line API
  *
  * Requires : Windows Server 2008 R2 x64 or higher
  *
  * vssadmin commands:
  *
  *   vssadmin list volumes
        Volume path: C:\
          Volume name: \\?\Volume{GUID}\

  *   vssadmin create shadow /For=C:
        Successfully created shadow copy for 'C:\'
          Shadow Copy ID: {GUID}
          Shadow Copy Volume Name: \\?\GLOBALROOT\Device\HarddiskVolumeShadowCopy1

  *   vssadmin list shadows
        Contents of shadow copy set ID: {GUID}
          Shadow Copy ID: {GUID}
            Original Volume: (C:)\\?\Volume{GUID}\
            Shadow Copy Volume: \\?\GLOBALROOT\Device\HarddiskVolumeShadowCopy1

  *   vssadmin delete shadows /For=C: /Quiet
  *
  * mount shadow:
  *
  *   mklink /d c:\vss \\?\GLOBALROOT\Device\HarddiskVolumeShadowCopy1\
  *     NOTE : Must add '\' to shadow path or mount doesn't work.
  *
  * unmount shadow:
  *
  *   rd c:\vss
  *
  * @author pquiring
  */

import java.util.*;
import java.io.*;

import javaforce.*;

public class VSS {
  public static String[] listVolumes() {
    ShellProcess sh = new ShellProcess();
    sh.keepOutput(true);
    String out = sh.run(new String[] {"vssadmin", "list", "volumes"}, false);
    //Volume path: C:\
    String lns[] = out.split("\r\n");
    ArrayList<String> vols = new ArrayList<String>();
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Volume path:") && lns[a].charAt(13) != '\\') {
        vols.add(lns[a].substring(13,15));
      }
    }
    return vols.toArray(new String[vols.size()]);
  }

  //output == C:=\\?\GLOBALROOT\Device\HarddiskVolumeShadowCopy1
  public static String[] listShadows() {
    ShellProcess sh = new ShellProcess();
    sh.keepOutput(true);
    String out = sh.run(new String[] {"vssadmin", "list", "shadows"}, false);
    //Original Volume: (C:)\\?\Volume{3b8f2351-58dd-11e6-80b4-806e6f6e6963}\
    //Shadow Copy Volume: \\?\GLOBALROOT\Device\HarddiskVolumeShadowCopy1
    String lns[] = out.split("\r\n");
    ArrayList<String> shadows = new ArrayList<String>();
    String org = "?:";
    for(int a=0;a<lns.length;a++) {
      lns[a] = lns[a].trim();
      if (lns[a].startsWith("Original Volume:")) {
        org = lns[a].substring(18, 20);  //C:
      }
      if (lns[a].startsWith("Shadow Copy Volume:")) {
        shadows.add(org + "=" + lns[a].substring(20));
        org = null;
      }
    }
    return shadows.toArray(new String[shadows.size()]);
  }

  public static boolean createShadow(String volume) {
    //Successfully created shadow copy for 'C:\'
    //Shadow Copy Volume Name: \\?\GLOBALROOT\Device\HarddiskVolumeShadowCopy1
    JFLog.log("VSS:createShadow:" + volume);
    ShellProcess sh = new ShellProcess();
    sh.keepOutput(true);
    String out = sh.run(new String[] {"vssadmin", "create", "shadow", "/For=" + volume}, false);
    String lns[] = out.split("\r\n");
    for(int a=0;a<lns.length;a++) {
      lns[a] = lns[a].trim();
      if (lns[a].startsWith("Successfully")) {
        return true;
      }
    }
    JFLog.log("Error:createShadow failed:" + volume);
    return false;
  }

  public static boolean deleteShadow(String volume) {
    JFLog.log("VSS:deleteShadow:" + volume);
    ShellProcess sh = new ShellProcess();
    sh.keepOutput(true);
    String out = sh.run(new String[] {"vssadmin", "delete", "shadows", "/For=" + volume, "/Quiet"}, false);
    //no output
    return sh.getErrorLevel() == 0;
  }

  public static boolean mountShadow(String mountPath, String shadowPath) {
    JFLog.log("VSS:mount:" + mountPath + " to " + shadowPath);
    ShellProcess sh = new ShellProcess();
    sh.keepOutput(true);
    String out = sh.run(new String[] {"cmd", "/c", "mklink", "/d", mountPath.replaceAll("[/]", "\\\\"), shadowPath + "\\"}, true);
    //symbolic link created for c:\vss <<===>> c:\windows
    JFLog.log("mklink output:" + out);
    return out.startsWith("symbolic link created for");
  }

  public static boolean unmountShadow(String mountPath) {
    JFLog.log("VSS:unmountShadow:" + mountPath);
    new File(mountPath).delete();
    return true;
  }

  public static void usage() {
    System.out.println("Usage : VSS {command}");
    System.out.println(" listvols : list volumes");
    System.out.println(" listshadows : list shadows");
    System.out.println(" createshadow {drive} : create shadow");
    System.out.println(" mountshadow {path} {shadow} : mount shadow");
    System.out.println(" unmountshadow {path} : unmount shadow");
    System.out.println(" deleteshadow {shadow} : delete shadow");
    System.exit(1);
  }

  public static void main(String[] args) {
    if (args.length == 0) usage();
    switch (args[0]) {
      case "listvols": listvols(args); break;
      case "listshadows": listshadows(args); break;
      case "createshadow": createshadow(args); break;
      case "mountshadow": mountshadow(args); break;
      case "unmountshadow": unmountshadow(args); break;
      case "deleteshadow": deleteshadow(args); break;
      default: usage();
    }
  }

  public static void listvols(String[] args) {
    String[] vols = listVolumes();
    for(String vol : vols) {
      System.out.println("Volume : " + vol);
    }
  }

  public static void listshadows(String[] args) {
    String[] shadows = listShadows();
    for(String shadow : shadows) {
      System.out.println("Volume : " + shadow);
    }
  }

  public static void createshadow(String[] args) {
    if (args.length != 2) usage();
    if (createShadow(args[1])) {
      System.out.println("Shadow creation successful!");
    } else {
      System.out.println("Shadow creation failed!");
    }
  }

  public static void mountshadow(String[] args) {
    if (args.length != 3) usage();
    if (mountShadow(args[1], args[2])) {
      System.out.println("Shadow mount successful!");
    } else {
      System.out.println("Shadow mount failed!");
    }
  }

  public static void unmountshadow(String[] args) {
    if (args.length != 2) usage();
    if (unmountShadow(args[1])) {
      System.out.println("Shadow unmount successful!");
    } else {
      System.out.println("Shadow unmount failed!");
    }
  }

  public static void deleteshadow(String[] args) {
    if (args.length != 2) usage();
    if (deleteShadow(args[1])) {
      System.out.println("Shadow deletion successful!");
    } else {
      System.out.println("Shadow deletion failed!");
    }
  }
}
