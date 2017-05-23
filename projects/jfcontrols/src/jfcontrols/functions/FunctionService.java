package jfcontrols.functions;

/** Logic Service
 *
 * @author pquiring
 */

import java.io.*;
import java.lang.reflect.*;

import javaforce.*;

import jfcontrols.tags.*;

public class FunctionService extends Thread {
  public static volatile boolean active;
  public static Object lock = new Object();

  public static void main() {
    new FunctionService().start();
  }

  public void run() {
    LogicLoader loader = new LogicLoader();
    Class mainCls, initCls;
    File mainFile = new File("work/class/main.class");
    if (!mainFile.exists()) {
      JFLog.log("main function not compiled");
      return;
    }
    File initFile = new File("work/class/init.class");
    if (!initFile.exists()) {
      JFLog.log("init function not compiled");
      return;
    }
    try {
      mainCls = loader.loadClass("code.func_0");
      initCls = loader.loadClass("code.func_1");
    } catch (Exception e) {
      JFLog.log(e);
      return;
    }
    Method main, init;
    try {
      main = mainCls.getMethod("code", Tag[].class);
      init = initCls.getMethod("code", Tag[].class);
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
      TagsService.doReads();
      try {
        main.invoke(null, new Object[] {null});
      } catch (Exception e) {
        JFLog.log(e);
      }
      TagsService.doWrites();
    }
    synchronized(lock) {
      lock.notify();
    }
  }

  public static void cancel() {
    synchronized(lock) {
      active = false;
      try {lock.wait();} catch (Exception e) {}
    }
  }

  public static boolean isActive() {
    return active;
  }

  public static boolean generateFunction(int fid, SQL sql) {
    String code = FunctionCompiler.generateFunction(fid, sql);
    new File("work/java").mkdirs();
    new File("work/class").mkdirs();
    String java_file = "work/java/Code_" + fid + ".java";
    String class_file = "work/class/Code_" + fid + ".class";
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
      Process p = Runtime.getRuntime().exec(new String[] {"javac" , "work/java/*.java", "-d", "work/class"});
      p.waitFor();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}
