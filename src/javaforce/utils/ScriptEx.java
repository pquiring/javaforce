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
    JFNative.load_ffmpeg = false;
    JFNative.load();
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
  }

  public void run() {
    if (args == null || args.length < 1) {
      usage();
      return;
    }
    switch (args[0]) {
      case "getweek":
        Calendar cal = Calendar.getInstance();
        System.out.println(String.format("%d", cal.get(Calendar.WEEK_OF_YEAR)));
        return;
      default:
        System.out.println("Unknown command:" + args[0]);
        usage();
        return;
    }
  }
}
