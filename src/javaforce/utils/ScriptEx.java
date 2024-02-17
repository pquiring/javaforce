package javaforce.utils;

/** Scripting Utils
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.ansi.server.*;
import javaforce.jni.*;

public class ScriptEx {
  public static String[] args;

  public static void main(String[] args) {
    ScriptEx.args = args;
    ANSI.enableConsoleMode();
    ConsoleOutput.install();
    int ret = 0;
    try {
      new ScriptEx().run();
    } catch (Throwable e) {
      e.printStackTrace();
      ret = 1;
    }
    ANSI.disableConsoleMode();
    System.exit(ret);
  }

  public void usage() {
    System.out.println("ScriptEx command [args]");
    System.out.println("  get-week : return current week in year (1-52)");
    System.out.println("  get-epoch-ms : return current epoch (1970) in ms");
    System.out.println("  get-epoch-sec : return current epoch (1970) in seconds");
    System.out.println("  get-epoch-ldap : return current epoch (1601) in 100-ns");
  }

  public void run() {
    if (args == null || args.length < 1) {
      usage();
      return;
    }
    switch (args[0]) {
      case "get-week":
        Calendar cal = Calendar.getInstance();
        System.out.println(String.format("%d", cal.get(Calendar.WEEK_OF_YEAR)));
        return;
      case "get-epoch-ms":
        System.out.println(System.currentTimeMillis());
        return;
      case "get-epoch-sec":
        System.out.println(System.currentTimeMillis() / 1000L);
        return;
      case "get-epoch-ldap":
        System.out.println((System.currentTimeMillis() - 11644473600000L) * 10000L);  //1970 - 1601 = 11644473600000 ms
        return;
      default:
        System.out.println("Unknown command:" + args[0]);
        usage();
        return;
    }
  }
}
