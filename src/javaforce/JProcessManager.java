package javaforce;

import java.net.*;
import java.awt.Window;
import java.util.*;
import java.security.Permission;

/**
 * JProcessManager is used to manage multiple processes within one JVM.<br> You
 * can create new processes and kill running processes.<br>
 *
 * @see JProcess
 */
public class JProcessManager {

  private ArrayList<JProcess> list;
  private int nextpid = 1;
  private JSecurityManager sm;
  private SecurityManager orgsm;
  private ThreadGroup rootThreadGroup;
  private Object lock;
  private Thread mainThread;

  public JProcessManager() {
    list = new ArrayList<JProcess>();
    orgsm = System.getSecurityManager();
    sm = new JSecurityManager();
    System.setSecurityManager(sm);
    rootThreadGroup = new ThreadGroup("JProcessManager");
    lock = new Object();
  }

  /**
   * Create a new instance of JProcess.<br>
   *
   * @param className the name of the class that contains the main(String [])
   * method.
   * @param args a list of arguments to pass to main(String []).
   * @param paths a list of URLs for the classpath. ie:
   * "file:///c:/path/file.jar".
   * @return JProcess an unstarted process.
   */
  public JProcess createJProcess(String className, String[] args, URL[] paths, String user_dir) {
    JProcess process;
    synchronized (lock) {
      process = JProcess.createInstance(className, args, paths, rootThreadGroup, nextpid++, user_dir);
      if (process == null) {
        return null;
      }
      list.add(process);
    }
    return process;
  }

  /**
   * Returns a list of running process.
   */
  public ArrayList<JProcess> getList() {
    return list;
  }

  /**
   * Kills a process based on its pid.
   *
   * @see JProcess.getPid
   */
  public synchronized boolean kill(int pid) {
    JProcess process = getProcessFor(pid);
    if (process == null) {
      return false;
    }
    synchronized (lock) {
      list.remove(process);
    }
    process.kill();
    System.gc();
    return false;
  }

  /**
   * Returns the current JProcess that is running in the current thread.
   */
  public JProcess getCurrentProcess() {
    return sm.getCurrentProcess();
  }

  private class JSecurityManager extends SecurityManager {

    public void checkExit(int status) {
      JProcess process = getCurrentProcess();
      if (process != null) {
        kill(process.getPid());
        throw new SecurityException();
      }
    }

    public void checkPermission(Permission perm) {
    }

    public void checkPermission(Permission perm, Object ctx) {
    }

    public boolean checkTopLevelWindow(Object obj) {
      if (!(obj instanceof Window)) {
        return true;
      }
      JProcess process = getCurrentProcess();
      if (process == null) {
        return true;
      }
      process.addWindow((Window) obj);
      return true;
    }

    public JProcess getCurrentProcess() {
      ThreadGroup group = getThreadGroup();
      JProcess match = null;
      match = getProcessFor(group);
      if (match != null) {
        return match;
      }
      Class[] class_context = getClassContext();
      for (int i = 0; i < class_context.length; i++) {
        ClassLoader loader = class_context[i].getClassLoader();
        if (loader != null) {
          match = getProcessFor(loader);
          if (match != null) {
            return match;
          }
        }
      }
      return null;
    }
  }

  /**
   * Returns a JProcess for the specified ThreadGroup
   *
   * @see JProcess.getThreadGroup
   */
  JProcess getProcessFor(ThreadGroup threadGroup) {
    JProcess process;
    synchronized (lock) {
      for (int a = 0; a < list.size(); a++) {
        process = list.get(a);
        if (process.getThreadGroup().parentOf(threadGroup)) {
          return process;
        }
      }
    }
    return null;
  }

  /**
   * Returns a JProcess for the specified ClassLoader
   *
   * @see JProcess.getClassLoader
   */
  JProcess getProcessFor(ClassLoader classLoader) {
    JProcess process;
    synchronized (lock) {
      for (int a = 0; a < list.size(); a++) {
        process = list.get(a);
        if (process.getClassLoader() == classLoader) {
          return process;
        }
      }
    }
    return null;
  }

  /**
   * Returns a JProcess for the specified pid
   *
   * @see JProcess.getPid
   */
  JProcess getProcessFor(int pid) {
    JProcess process;
    synchronized (lock) {
      for (int a = 0; a < list.size(); a++) {
        process = list.get(a);
        if (process.getPid() == pid) {
          return process;
        }
      }
    }
    return null;
  }
};
