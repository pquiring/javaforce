package jfcontrols.functions;

/** Function Service
 *
 * @author pquiring
 */

import java.io.*;
import java.lang.reflect.*;

import javaforce.*;
import javaforce.jni.*;

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
  public static FunctionLoader loader;
  public static SQL sql;

  private static String jdk;

  static {
    String ext = null;
    if (JF.isWindows()) {
      jdk = WinNative.findJDKHome();
      if (jdk == null) {
        jdk = "c:\\Program Files\\Java\\JDK8";
      }
      JFLog.log("java.app.home=" + System.getProperty("java.app.home"));
      ext = ".exe";
    } else {
      jdk = "/usr/bin";
      ext = "";
    }
    File javac = new File(jdk + "/bin/javac" + ext);
    if (!javac.exists()) {
      Main.addMessage("Unable to find javac");
    }
    JFLog.log("JDK=" + jdk);
  }

  public static void main() {
    new FunctionService().start();
  }

  public void run() {
    if (active) {
      JFLog.log("Error:FunctionService already running");
      Main.trace();
      return;
    }
    Class mainCls, initCls;
    TagsCache tags = new TagsCache();
    File mainFile = new File(Paths.dataPath + "/work/class/func_1.class");
    boolean compile = false;
    if (sql == null) {
      sql = SQLService.getSQL();
      FunctionRuntime.sql = sql;
    }
    if (!mainFile.exists()) {
      FunctionService.generateFunction(1, sql);
      compile = true;
    }
    File initFile = new File(Paths.dataPath + "/work/class/func_2.class");
    if (!initFile.exists()) {
      FunctionService.generateFunction(2, sql);
      compile = true;
    }
    if (compile) {
      FunctionService.compileProgram(sql);
    }
    if (!mainFile.exists()) {
      JFLog.log("main function not compiled");
      return;
    }
    if (!initFile.exists()) {
      JFLog.log("init function not compiled");
      return;
    }
    loader = new FunctionLoader();
    try {
      mainCls = loader.loadClass("func_1");
      initCls = loader.loadClass("func_2");
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    Method main, init;
    try {
      main = mainCls.getMethod("code", TagBase[].class);
      init = initCls.getMethod("code", TagBase[].class);
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    Object mainObj, initObj;
    try {
      mainObj = mainCls.newInstance();
      initObj = initCls.newInstance();
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    active = true;
    TagsService.doReads();
    try {
      init.invoke(initObj, new Object[] {null});
    } catch (Exception e) {
      JFLog.log(e);
    }
    TagsService.doWrites();
    TagAddr ta = tags.decode("system.scantime");
    TagBase tag = tags.getTag(ta);
    JFLog.log("Function.Service starting...");
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
        main.invoke(mainObj, new Object[] {null});
      } catch (InvocationTargetException ite) {
        Throwable e = ite.getTargetException();
        JFLog.log(e);
      } catch (Exception e) {
        JFLog.log(e);
      }
      synchronized(wapi) {
        wapi.notifyAll();
      }
      TagsService.doWrites();
      long end = System.currentTimeMillis();
      long scantime = end - FunctionRuntime.now;
      if (!Main.debug) {
        tag.setValue(Long.toString(scantime));
      }
//      System.out.println("scantime=" + scantime);
    }
    JFLog.log("Function.Service stopping...");
    sql.close();
    sql = null;
    FunctionService.sql = null;
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
    loader = null;
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
        main = cls.getMethod("code", TagBase[].class);
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
    String name = sql.select1value("select name from jfc_funcs where id=" + fid);
    JFLog.log("Compiling func:" + fid + ":" + name);
    String code = FunctionCompiler.generateFunction(fid, sql);
    if (code == null) return false;
    new File(Paths.dataPath + "/work/java").mkdirs();
    new File(Paths.dataPath + "/work/class").mkdirs();
    String java_file = Paths.dataPath + "/work/java/func_" + fid + ".java";
    String class_file = Paths.dataPath + "/work/class/func_" + fid + ".class";
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
  public static String error;
  public static boolean compileProgram(SQL sql) {
    try {
      ShellProcess sp = new ShellProcess();
      sp.keepOutput(true);
      error = sp.run(new String[] {jdk + "/bin/javac", "-cp", "jfcontrols.jar" + File.pathSeparator + "javaforce.jar", Paths.dataPath + "/work/java/*.java", "-d", Paths.dataPath + "/work/class"}, true);
      if (error.length() == 0) {
        restart();
      }
      return error.length() == 0;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  public static boolean[][] getDebugEnabled(int fid) {
    File clsFile = new File(Paths.dataPath + "/work/class/func_" + fid + ".class");
    if (!clsFile.exists()) return null;
    Class cls;
    try {
      cls = loader.loadClass("func_" + fid);
      Field fld = cls.getField("debug_en");
      boolean flags[][] = (boolean[][])fld.get(null);
      return flags;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
  public static String[] getDebugTagValues(int fid) {
    File clsFile = new File(Paths.dataPath + "/work/class/func_" + fid + ".class");
    if (!clsFile.exists()) return null;
    Class cls;
    try {
      cls = loader.loadClass("func_" + fid);
      Field fld = cls.getField("debug_tv");
      String flags[] = (String[])fld.get(null);
      return flags;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
  public static boolean functionUpToDate(int fid, long revision) {
    File clsFile = new File(Paths.dataPath + "/work/class/func_" + fid + ".class");
    if (!clsFile.exists()) return false;
    Class cls;
    try {
      cls = loader.loadClass("func_" + fid);
      Field fld = cls.getField("revision");
      long rev = (long)fld.get(null);
      return rev == revision;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

}
