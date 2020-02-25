package jfcontrols.functions;

/** Function Service
 *
 * @author pquiring
 */

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.tools.*;

import javaforce.*;
import javaforce.jni.*;

import jfcontrols.app.*;
import jfcontrols.api.*;
import jfcontrols.db.*;
import jfcontrols.tags.*;

public class FunctionService extends Thread {
  public static volatile boolean active;
  public static Object done = new Object();
  public static Object rapi = new Object();
  public static Object wapi = new Object();
  public static Object fapi = new Object();
  public static FunctionLoader loader;

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

  private void try_run() {
    if (active) {
      JFLog.log("Error:FunctionService already running");
      Main.trace();
      return;
    }
    active = true;
    Class mainCls, initCls;
    File mainFile = new File(Paths.configPath + "/work/class/func_1.class");
    boolean compile = false;
    if (!mainFile.exists()) {
      FunctionService.generateFunction(1);
      compile = true;
    }
    File initFile = new File(Paths.configPath + "/work/class/func_2.class");
    if (!initFile.exists()) {
      FunctionService.generateFunction(2);
      compile = true;
    }
    if (compile) {
      FunctionService.compileProgram();
    }
    if (!mainFile.exists()) {
      JFLog.log("main function not compiled");
      active = false;
      return;
    }
    if (!initFile.exists()) {
      JFLog.log("init function not compiled");
      active = false;
      return;
    }
    loader = new FunctionLoader();
    try {
      mainCls = loader.loadClass("func_1");
      initCls = loader.loadClass("func_2");
    } catch (Exception e) {
      JFLog.log(e);
      active = false;
      JFLog.log("Function.Service can not start, program not compiled!");
      return;
    }
    Method main, init;
    try {
      main = mainCls.getMethod("code", TagBase[].class);
      init = initCls.getMethod("code", TagBase[].class);
    } catch (Exception e) {
      JFLog.log(e);
      active = false;
      JFLog.log("Function.Service can not start, program not compiled!");
      return;
    }
    Object mainObj, initObj;
    try {
      mainObj = mainCls.newInstance();
      initObj = initCls.newInstance();
    } catch (Exception e) {
      JFLog.log(e);
      active = false;
      JFLog.log("Function.Service can not start, program not compiled!");
      return;
    }
    JFLog.log("Function.Service starting...");
    TagsService.doReads();
    try {
      init.invoke(initObj, new Object[] {null});
    } catch (Exception e) {
      JFLog.log(e);
    }
    TagsService.doWrites();
    TagBase tag = TagsService.getTag("system.scantime");
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

  public static boolean generateFunction(int fid) {
    FunctionRow func = Database.getFunctionById(fid);
    JFLog.log("Compiling func:" + fid + ":" + func.name);
    String code = FunctionCompiler.generateFunction(fid);
    if (code == null) return false;
    new File(Paths.configPath + "/work/java").mkdirs();
    new File(Paths.configPath + "/work/class").mkdirs();
    String java_file = Paths.configPath + "/work/java/func_" + fid + ".java";
    String class_file = Paths.configPath + "/work/class/func_" + fid + ".class";
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
  public static boolean compileProgram() {
    JFLog.log("Compiling functions...");
    try {
      File files[] = new File(Paths.configPath + "/work/java").listFiles(
        new FilenameFilter() {
           public boolean accept(File dir, String name) {
             return name.endsWith(".java");
           }
        }
      );
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

      Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));
      Iterable<String> options = Arrays.asList(new String[] {
        "-cp", System.getProperty("java.class.path"),
        "-d", Paths.configPath + "/work/class"
      });
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits1);

      boolean success = task.call();

      fileManager.close();

      if (success) {
        JFLog.log("Compilation successful!");
        restart();
        return true;
      }
      JFLog.log("Error:Compilation failed! See log output and try again.");
      return false;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  public static boolean[][] getDebugEnabled(int fid) {
    File clsFile = new File(Paths.configPath + "/work/class/func_" + fid + ".class");
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
    File clsFile = new File(Paths.configPath + "/work/class/func_" + fid + ".class");
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
    File clsFile = new File(Paths.configPath + "/work/class/func_" + fid + ".class");
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
