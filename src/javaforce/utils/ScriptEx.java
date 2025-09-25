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

  public static final boolean ansi = false;

  public static void main(String[] args) {
    ScriptEx.args = args;
    if (ansi) {
      ANSI.enableConsoleMode();
      ConsoleOutput.install();
    }
    int ret = 0;
    try {
      new ScriptEx().run();
    } catch (Throwable e) {
      e.printStackTrace();
      ret = 1;
    }
    if (ansi) {
      ANSI.disableConsoleMode();
    }
    System.exit(ret);
  }

  public void usage() {
    System.out.println("ScriptEx command [args]");
    System.out.println("  get-week");
    System.out.println("   : return current week in year (1-52)");
    System.out.println("  get-epoch-ms");
    System.out.println("   : return current epoch (1970) in ms");
    System.out.println("  get-epoch-sec");
    System.out.println("   : return current epoch (1970) in seconds");
    System.out.println("  get-epoch-ldap");
    System.out.println("   : return current epoch (1601) in 100-ns");
    System.out.println("  runas [domain\\]user password cmd args");
    System.out.println("   : run cmd as another user");
    System.out.println("  runasadmin cmd args");
    System.out.println("   : run cmd with admin rights");
    System.exit(1);
  }

  public void run() {
    if (args == null || args.length < 1) {
      usage();
      return;
    }
    int flags = 0;
    switch (args[0]) {
      case "get-week": {
        Calendar cal = Calendar.getInstance();
        System.out.println(String.format("%d", cal.get(Calendar.WEEK_OF_YEAR)));
        return;
      }
      case "get-epoch-ms": {
        System.out.println(System.currentTimeMillis());
        return;
      }
      case "get-epoch-sec": {
        System.out.println(System.currentTimeMillis() / 1000L);
        return;
      }
      case "get-epoch-ldap": {
        System.out.println((System.currentTimeMillis() - 11644473600000L) * 10000L);  //1970 - 1601 = 11644473600000 ms
        return;
      }
      case "runas": {
        if (args.length < 4) {
          usage();
          return;
        }
        String domain = ".";
        String user = args[1];
        String pass = args[2];
        int idx = user.indexOf('\\');
        if (idx != -1) {
          //user contains domain
          domain = user.substring(0, idx);
          user = user.substring(idx + 1);
        }
        if (false) {
          //does not work as intended
          String[] cmd = new String[args.length - 3];
          for(int a=3;a<args.length;a++) {
            cmd[a - 3] = args[a];
          }
          if (!WinNative.impersonateUser(domain, user, pass)) {
            JFLog.log("Logon failed");
            return;
          }
          ShellProcess sp = new ShellProcess();
          sp.inherit(true);
          sp.run(cmd, true);
        } else {
          String cmd = args[3];
          String cmdargs = "";
          for(int a=4;a<args.length;a++) {
            if (cmdargs.length() > 0) cmdargs += " ";
            cmdargs += args[a];
          }
          if (!WinNative.createProcessAsUser(domain, user, pass, cmd, cmdargs, flags)) {
            JFLog.log("Logon failed");
            return;
          }
        }
        return;
      }
      case "runasadmin": {
        String cmd = args[1];
        String cmdargs = "";
        for(int a=2;a<args.length;a++) {
          if (cmdargs.length() > 0) cmdargs += " ";
          cmdargs += args[a];
        }
        WinNative.shellExecute("runas", cmd, cmdargs);
        return;
      }
      default: {
        System.out.println("Unknown command:" + args[0]);
        usage();
        return;
      }
    }
  }
}
