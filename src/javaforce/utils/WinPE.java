package javaforce.utils;

/** Tool to add resources to Windows PE exe files.
 *
 * Based on WinRun4J source code (converted to Java).
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.jni.*;

public class WinPE {
  public static void usage() {
    System.out.println("WinPE : Add resources to Windows EXE PE files");
    System.out.println("Usage : WinPE exeFile file(s)");
    System.out.println("Supported : .ico .manifest .cfg");
    System.exit(0);
  }
  private static final int RT_ICON = 3;
  private static final int RT_GROUP_ICON = 11 + 3;
  private static final int RT_STRING = 6;  //too complex
  private static final int RT_RCDATA = 10;  //raw data (used to store .cfg file)
  private static final int RT_MANIFEST = 24;

  public static void main(String args[]) {
    if (!JF.isWindows()) {System.out.println("For windows only"); return;}
    if (args == null || args.length < 2) usage();
    String exeFile = args[0];
    for(int a=1;a<args.length;a++) {
      String file = args[a];
      if (file.endsWith(".ico")) {
        addIcon(exeFile, file);
      }
      else if (file.endsWith(".manifest")) {
        addManifest(exeFile, file);
      }
      else if (file.endsWith(".cfg")) {
        addCfg(exeFile, file);
      }
      else {
        System.out.println("Unsupported file:" + file);
      }
    }
  }

  public static void addIcon(String exeFile, String icoFile) {
    try {
      //LoadIcon
      FileInputStream fis = new FileInputStream(icoFile);
      byte ico[] = JF.readAll(fis);
      fis.close();

      //Begin
      long exe = WinNative.peBegin(exeFile);
      if (exe == 0) {
        System.out.println("Unable to open exe");
        return;
      }

      //add icon(s)
      WinNative.peAddIcon(exe, ico);

      //end
      WinNative.peEnd(exe);

      System.out.println("Added:" + icoFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void addManifest(String exeFile, String manifestFile) {
    try {
      FileInputStream fis = new FileInputStream(manifestFile);
      byte str[] = JF.readAll(fis);
      fis.close();

      //Begin
      long exe = WinNative.peBegin(exeFile);
      if (exe == 0) {
        System.out.println("Unable to open exe");
        return;
      }

      //add manifest
      WinNative.peAddString(exe, RT_MANIFEST, 1, str);

      //end
      WinNative.peEnd(exe);

      System.out.println("Added:" + manifestFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void addCfg(String exeFile, String cfgFile) {
    try {
      FileInputStream fis = new FileInputStream(cfgFile);
      byte str[] = JF.readAll(fis);
      fis.close();

      //Begin
      long exe = WinNative.peBegin(exeFile);
      if (exe == 0) {
        System.out.println("Unable to open exe");
        return;
      }

      //add cfg
      WinNative.peAddString(exe, RT_RCDATA, 1, str);

      //end
      WinNative.peEnd(exe);

      System.out.println("Added:" + cfgFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
