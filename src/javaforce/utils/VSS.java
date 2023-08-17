package javaforce.utils;

/** VSS - Volume Shadow Service command line API
  *
  * @author pquiring
  */

import javaforce.*;
import javaforce.jni.WinNative;

public class VSS {
  public static void usage() {
    System.out.println("Usage : VSS {command}");
    System.out.println(" listvols : list volumes");
    System.out.println(" listshadows : list shadows");
    System.out.println(" createshadow {drive} [path] : create shadow (optional mount to path)");
    System.out.println(" mountshadow {path} {shadow-vol} : mount shadow");
    System.out.println(" unmountshadow {path} : unmount shadow");
    System.out.println(" deleteshadow {shadow-id} : delete shadow (/all for all shadows)");
    System.exit(1);
  }

  public static void main(String[] args) {
    if (args.length == 0) usage();
    if (!WinNative.vssInit()) {
      JFLog.log("VSS Init Failed");
      return;
    }
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
    String[] vols = WinNative.vssListVols();
    for(String vol : vols) {
      System.out.println("Volume : " + vol);
    }
  }

  public static void listshadows(String[] args) {
    String[][] shadows = WinNative.vssListShadows();
    for(String[] shadow : shadows) {
      System.out.println("GUID : " + shadow[0]);
      System.out.println("Shadow Volume : " + shadow[1]);
      System.out.println("Volume : " + shadow[2]);
    }
  }

  public static void createshadow(String[] args) {
    if (args.length < 2) usage();
    if (args.length > 2) {
      if (WinNative.vssCreateShadow(args[1], args[2])) {
        System.out.println("Shadow creation and mount successful!");
      } else {
        System.out.println("Shadow creation failed!");
      }
    } else {
      if (WinNative.vssCreateShadow(args[1])) {
        System.out.println("Shadow creation successful!");
      } else {
        System.out.println("Shadow creation failed!");
      }
    }
  }

  public static void mountshadow(String[] args) {
    if (args.length != 3) usage();
    if (WinNative.vssMountShadow(args[1], args[2])) {
      System.out.println("Shadow mount successful!");
    } else {
      System.out.println("Shadow mount failed!");
    }
  }

  public static void unmountshadow(String[] args) {
    if (args.length != 2) usage();
    if (WinNative.vssUnmountShadow(args[1])) {
      System.out.println("Shadow unmount successful!");
    } else {
      System.out.println("Shadow unmount failed!");
    }
  }

  public static void deleteshadow(String[] args) {
    if (args.length != 2) usage();
    if (args[1].equals("/all")) {
      if (WinNative.vssDeleteShadowAll()) {
        System.out.println("Shadow deletion(s) successful!");
      } else {
        System.out.println("Shadow deletion(s) failed!");
      }
    } else {
      if (WinNative.vssDeleteShadow(args[1])) {
        System.out.println("Shadow deletion successful!");
      } else {
        System.out.println("Shadow deletion failed!");
      }
    }
  }
}
