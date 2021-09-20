package javaforce;

import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.awt.Window;

import javaforce.awt.*;

/**
 * JProcess represents one process within a JVM.<br> Each JProcess has its own
 * ThreadGroup, ClassLoader.<br> You must use JProcessManager to create
 * JProcess'es.<br>
 *
 * @see JProcessManager
 */
public class JProcess implements Runnable {

  private URLClassLoader loader;
  private ThreadGroup threadGroup;
  private String name;
  private int pid;  //unique ID
  private Thread mainThread;
  private int state;
  private final static int INIT = 0;
  private final static int RUNNING = 1;
  private final static int STOPPED = 2;  //System.exit() or kill()ed
  private Method mainMethod;
  private String args[];
  private ArrayList<Window> windowList;
  private String className;
  private String user_dir;

  private JProcess() {
    windowList = new ArrayList<Window>();
  }

  /**
   * DO NOT CALL DIRECTLY. YOU MUST CALL JProcessManager.createJProcess()
   * INSTEAD.<br> Creates an instance of JProcess.<br>
   */
  public static JProcess createInstance(String className, String[] args, URL[] paths, ThreadGroup rootThreadGroup, int pid, String user_dir) {
    JProcess process = new JProcess();
    process.pid = pid;
    process.state = INIT;
    process.className = className;
    process.user_dir = user_dir;
    if (args != null) {
      process.args = args;
    } else {
      process.args = new String[0];
    }
    if (paths == null) {
      paths = new URL[0];
    }
    process.loader = new URLClassLoader(paths);
    process.name = process.toString();
    process.threadGroup = new ThreadGroup(rootThreadGroup, process.name);
    process.threadGroup.setMaxPriority(Thread.MAX_PRIORITY - 1);
    try {
      Class<?> target_class = process.loader.loadClass(className);
      process.mainMethod = target_class.getMethod("main", String[].class);
      if (((process.mainMethod.getModifiers() & Modifier.STATIC) == 0) || (process.mainMethod.getModifiers() & Modifier.PUBLIC) == 0) {
        return null;
      }
      return process;
    } catch (Exception e) {
      JFAWT.showError("error", e.toString());
      return null;
    }
  }

  /**
   * Starts a process once created from JProcessManager.createJProcess().
   */
  public synchronized boolean start() {
    if (state != INIT) {
      return false;
    }
    System.setProperty("user.dir", user_dir);  //doesn't work!
    mainThread = new Thread(threadGroup, this, "JProcess : " + name);
    mainThread.start();
    state = RUNNING;
    return true;
  }

  /**
   * Do not call directly
   */
  public void run() {
    try {
      mainMethod.invoke(null, new Object[]{args});
    } catch (Exception e) {
    }
  }

  /**
   * DO NOT CALL DIRECTLY. YOU MUST USE JProcessManager.kill() INSTEAD.
   *
   * @see JProcessManager.kill
   */
  public synchronized boolean kill() {
    if (state != RUNNING) {
      return false;
    }
    state = STOPPED;
    for (int a = 0; a < windowList.size(); a++) {
      windowList.get(a).dispose();
    }
    windowList.clear();
//    threadGroup.stop();  //TODO : find native method replacement
    return true;
  }

  /**
   * DO NOT CALL.<br> Used by JProcessManager to manage which Window's belong to
   * this process.
   */
  public void addWindow(Window window) {
    windowList.add(window);
  }

  public int getPid() {
    return pid;
  }

  public ThreadGroup getThreadGroup() {
    return threadGroup;
  }

  public ClassLoader getClassLoader() {
    return loader;
  }

  public String getName() {
    return name;
  }
};
