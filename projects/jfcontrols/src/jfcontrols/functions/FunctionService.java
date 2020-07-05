package jfcontrols.functions;

/** Function Service
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.jni.*;

import jfcontrols.app.*;
import jfcontrols.api.*;
import jfcontrols.db.*;
import jfcontrols.tags.*;
import jfcontrols.logic.*;

public class FunctionService extends Thread {
  public static volatile boolean active;
  public static Object done = new Object();
  public static Object rapi = new Object();
  public static Object wapi = new Object();
  public static Object fapi = new Object();

  private static String jdk;

  static {
    String ext = null;
    if (JF.isWindows()) {
      jdk = WinNative.findJDKHome();
      JFLog.log("java.app.home=" + System.getProperty("java.app.home"));
      ext = ".exe";
    } else {
      jdk = "/usr/bin";
      ext = "";
    }
    if (jdk != null) {
      File javac = new File(jdk + "/bin/javac" + ext);
      if (!javac.exists()) {
        Main.addMessage("Unable to find javac");
      }
    }
    JFLog.log("JDK=" + jdk);
  }

  public static void main() {
    new FunctionService().start();
  }

  public void run() {
    try {
      try_run();
    } catch (Exception e) {
      JFLog.log(e);
      active = false;
    }
  }

  private static ArrayList<LogicFunction> functions = new ArrayList<>();

  public static LogicFunction getFunction(int fid) {
    int size = functions.size();
    for(int a=0;a<size;a++) {
      LogicFunction func = functions.get(a);
      if (func.id == fid) return func;
    }
    JFLog.log("Error:fid not found:" + fid);
    return null;
  }

  private void try_run() {
    if (active) {
      JFLog.log("Error:FunctionService already running");
      Main.trace();
      return;
    }
    active = true;

    JFLog.log("Function.Service starting...");

    JFLog.log("Building all functions...");
    FunctionRow funcs[] = Database.getFunctions();
    for(int a=0;a<funcs.length;a++) {
      LogicFunction func = FunctionCompiler.generateFunction(funcs[a].id, funcs[a].revision);
      functions.add(func);
    }

    TagsService.doReads();
    try {
      LogicFunction init = getFunction(Database.FUNC_INIT);
      init.execute(new LogicPos(128));
    } catch (Exception e) {
      JFLog.log(e);
    }
    TagsService.doWrites();
    TagBase tag = TagsService.getTag("system.scantime");
    LogicPos pos = new LogicPos(128);
    while (active) {
      FunctionRuntime.now = System.currentTimeMillis();
      FunctionRuntime.alarm_clear_ack();
      TagsService.doReads();
      synchronized(rapi) {
        rapi.notifyAll();
      }
      synchronized(fapi) {
        fapi.notifyAll();
      }
      try {
        LogicFunction main = getFunction(Database.FUNC_MAIN);
        pos.stackpos = 0;
        main.execute(pos);
      } catch (Exception e) {
        JFLog.log(e);
      }
      synchronized(wapi) {
        wapi.notifyAll();
      }
      TagsService.doWrites();
      long end = System.currentTimeMillis();
      long scantime = end - FunctionRuntime.now;
      tag.setValue(Long.toString(scantime));
      if (Main.debug_scantime) {
        JFLog.log("scantime=" + scantime);
      }
    }
    JFLog.log("Function.Service stopping...");
    synchronized(done) {
      done.notify();
    }
    active = false;
  }

  public static void cancel() {
    if (!active) return;
    synchronized(done) {
      active = false;
      try {done.wait();} catch (Exception e) {}
    }
  }

  private static void restart() {
    if (isActive()) {
      cancel();
    }
    System.gc();
    main();
  }

  public static boolean isActive() {
    return active;
  }

  public static void addReadQuery(TagsQuery q) {
    synchronized(rapi) {
      try {rapi.wait(10 * 1000);} catch (Exception e) {return;}
      for(int a=0;a<q.count;a++) {
        q.values[a] = q.tags[a].getValue();
      }
    }
  }

  public static void addWriteQuery(TagsQuery q) {
    synchronized(wapi) {
      try {wapi.wait(10 * 1000);} catch (Exception e) {return;}
      for(int a=0;a<q.count;a++) {
        q.tags[a].setValue(q.values[a]);
      }
    }
  }

  public static void functionRequest(int fid) {
    synchronized(fapi) {
      try {
        fapi.wait(10 * 1000);
        LogicFunction func = getFunction(fid);
        if (func == null) return;
        func.execute(new LogicPos(128));
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public static boolean recompileFunction(int fid) {
    synchronized(fapi) {
      try {
        fapi.wait(10 * 1000);
        LogicFunction org = getFunction(fid);
        LogicFunction func = generateFunction(fid);
        if (func == null) throw new Exception("");
        if (org != null) {
          functions.remove(org);
        }
        functions.add(func);
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
  }

  public static void deleteFunction(int fid) {
    synchronized(fapi) {
      try {
        fapi.wait(10 * 1000);
        LogicFunction org = getFunction(fid);
        if (org != null) {
          functions.remove(org);
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public static LogicFunction generateFunction(int fid) {
    FunctionRow func = Database.getFunctionById(fid);
    JFLog.log("Compiling func:" + fid + ":" + func.name);
    LogicFunction code = FunctionCompiler.generateFunction(fid, func.revision);
    return code;
  }
  public static boolean[][] getDebugEnabled(int fid) {
    LogicFunction func = getFunction(fid);
    if (func == null) return null;
    return func.debug_en;
  }
  public static String[] getDebugTagValues(int fid) {
    LogicFunction func = getFunction(fid);
    if (func == null) return null;
    return func.debug_tv;
  }
  public static boolean functionUpToDate(int fid, long revision) {
    LogicFunction func = getFunction(fid);
    if (func == null) {
      return false;
    }
    return func.revision == revision;
  }
}
