package jfcontrols.functions;

/** Function Service
 *
 * @author pquiring
 */

import java.io.*;
import java.lang.reflect.*;

import javaforce.*;

import jfcontrols.app.*;
import jfcontrols.api.*;
import jfcontrols.sql.*;
import jfcontrols.tags.*;

public class FunctionService extends Thread {
  public static volatile boolean active;
  public static Object done = new Object();
  public static Object rapi = new Object();
  public static Object wapi = new Object();
  public static Object fapi = new Object();
  public static FunctionLoader loader = new FunctionLoader();

  public static void main() {
    new FunctionService().start();
  }

  public void run() {
    SQL sql = SQLService.getSQL();
    Class mainCls, initCls;
    File mainFile = new File("work/class/func_1.class");
    boolean compile = false;
    if (!mainFile.exists()) {
      FunctionService.generateFunction(1, sql);
      compile = true;
    }
    File initFile = new File("work/class/func_2.class");
    if (!initFile.exists()) {
      FunctionService.generateFunction(2, sql);
      compile = true;
    }
    if (compile) {
      FunctionService.compileProgram(sql);
    }
    sql.close();
    sql = null;
    if (!mainFile.exists()) {
      JFLog.log("main function not compiled");
      return;
    }
    if (!initFile.exists()) {
      JFLog.log("init function not compiled");
      return;
    }
    try {
      mainCls = loader.loadClass("func_1");
      initCls = loader.loadClass("func_2");
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    Method main, init;
    try {
      main = mainCls.getMethod("code", TagAddr[].class);
      init = initCls.getMethod("code", TagAddr[].class);
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    active = true;
    TagsService.doReads();
    try {
      init.invoke(null, new Object[] {null});
    } catch (Exception e) {
      JFLog.log(e);
    }
    TagsService.doWrites();
    TagAddr ta = TagAddr.decode("system.scantime", null);
    TagBase tag = TagsService.getTag(ta, null);
    while (active) {
      long begin = System.currentTimeMillis();
      TagsService.doReads();
      synchronized(rapi) {
        rapi.notifyAll();
      }
      synchronized(fapi) {
        fapi.notifyAll();
      }
      try {
        main.invoke(null, new Object[] {null});
      } catch (Exception e) {
        JFLog.log(e);
      }
      synchronized(wapi) {
        wapi.notifyAll();
      }
      TagsService.doWrites();
      long end = System.currentTimeMillis();
      long scantime = end - begin;
      if (!Main.debug) {
        tag.setValue(ta, Long.toString(scantime));
      }
//      System.out.println("scantime=" + scantime);
    }
    synchronized(done) {
      done.notify();
    }
  }

  public static void cancel() {
    synchronized(done) {
      active = false;
      try {done.wait();} catch (Exception e) {}
    }
  }

  private static void restart() {
    if (isActive()) {
      cancel();
    }
    main();
  }

  public static boolean isActive() {
    return active;
  }

  public static void addReadQuery(TagsQuery q) {
    synchronized(rapi) {
      try {rapi.wait(10 * 1000);} catch (Exception e) {return;}
      for(int a=0;a<q.count;a++) {
        q.values[a] = q.tags[a].getValue(q.addr[a]);
      }
    }
  }

  public static void addWriteQuery(TagsQuery q) {
    synchronized(wapi) {
      try {wapi.wait(10 * 1000);} catch (Exception e) {return;}
      for(int a=0;a<q.count;a++) {
        q.tags[a].setValue(q.addr[a], q.values[a]);
      }
    }
  }

  public static void functionRequest(int fid) {
    synchronized(fapi) {
      try {fapi.wait(10 * 1000);} catch (Exception e) {return;}
      Class cls;
      try {
        cls = loader.loadClass("func_" + fid);
      } catch (Exception e) {
        JFLog.log(e);
        return;
      }
      Method main;
      try {
        main = cls.getMethod("code", TagAddr[].class);
      } catch (Exception e) {
        JFLog.log(e);
        return;
      }
      try {
        main.invoke(null, new Object[] {null});
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public static boolean generateFunction(int fid, SQL sql) {
    String code = FunctionCompiler.generateFunction(fid, sql);
    new File("work/java").mkdirs();
    new File("work/class").mkdirs();
    String java_file = "work/java/func_" + fid + ".java";
    String class_file = "work/class/func_" + fid + ".class";
    try {
      FileOutputStream fos = new FileOutputStream(java_file);
      fos.write(code.getBytes());
      fos.close();
      return true;
    } catch (Exception e) {
      new File(java_file).delete();
      JFLog.log(e);
      return false;
    }
  }
  public static boolean compileProgram(SQL sql) {
    try {
      ShellProcess sp = new ShellProcess();
      sp.keepOutput(true);
      String output = sp.run(new String[] {"javac", "-cp", "jfcontrols.jar" + File.pathSeparator + "javaforce.jar", "work/java/*.java", "-d", "work/class"}, true);
      JFLog.log("compiler=" + output);
      restart();
      return output.length() == 0;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}
