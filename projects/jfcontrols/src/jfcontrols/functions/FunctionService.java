package jfcontrols.functions;

/** Function Service
 *
 * @author pquiring
 */

import java.io.*;
import java.lang.reflect.*;

import javaforce.*;

import jfcontrols.api.*;
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
    Class mainCls, initCls;
    File mainFile = new File("work/class/func_1.class");
    if (!mainFile.exists()) {
      JFLog.log("main function not compiled");
      return;
    }
    File initFile = new File("work/class/func_2.class");
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
      JFLog.log("scan cycle=" + (end-begin));
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
      //TODO : log output - use javaforce.ShellProcess
      Process p = Runtime.getRuntime().exec(new String[] {"javac", "-cp", "jfcontrols.jar" + File.pathSeparator + "javaforce.jar", "work/java/*.java", "-d", "work/class"});
      InputStream is = p.getInputStream();
      p.waitFor();
      byte out[] = JF.readAll(is);
      if (out != null) JFLog.log("compiler=" + new String(out));
      restart();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}
