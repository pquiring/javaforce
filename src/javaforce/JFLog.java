package javaforce;

import java.io.*;
import java.util.*;

/**
 * JFLog is a file logger with support for multiple files and optional outputs
 * to System.out as well.
 */

public class JFLog {
  private static class LogInstance {
    private Object lock = new Object();
    private FileOutputStream fos;
    private PrintStream stdout;
    private long filesize;
    private String filename;
    private boolean enabled = true;
  }
  private static HashMap<Integer, LogInstance> list = new HashMap<Integer, LogInstance>();
  private static boolean useTimestamp = false;
  private static long timestampBase;

  private static class TraceException extends Exception {
    public TraceException(String msg) {
      super(msg);
    }
  }

  public static boolean init(int id, String filename, boolean append, PrintStream stdout) {
    LogInstance log = new LogInstance();
    log.stdout = stdout;
    log.filename = filename;
    if (filename != null) {
      if (append) {
        File file = new File(filename);
        log.filesize = file.length();
      } else {
        log.filesize = 0;
      }
      try {
        log.fos = new FileOutputStream(filename, append);
      } catch (Exception e) {
        System.out.println("Log:create file failed:" + filename);
        return false;
      }
    }
    synchronized(list) {
      list.put(id, log);
    }
    return true;
  }

  public static boolean init(String filename, boolean stdout) {
    return init(0, filename, false, stdout ? System.out : null);
  }

  public static boolean init(int id, String filename, boolean stdout) {
    return init(id, filename, false, stdout ? System.out : null);
  }

  public static boolean append(String filename, boolean stdout) {
    return init(0, filename, true, stdout ? System.out : null);
  }

  public static boolean append(int id, String filename, boolean stdout) {
    return init(id, filename, true, stdout ? System.out : null);
  }

  public static boolean close(int id) {
    LogInstance log = list.get(id);
    if (log == null) {
      return false;
    }
    synchronized(list) {
      list.remove(id);
    }
    try {
      if (log.fos != null) {
        log.fos.close();
      }
    } catch (Exception e) {
    }
    return true;
  }

  public static boolean close() {
    return close(0);
  }

  /** Uses a timestamp instead of date/time for each message logged. */
  public static void enableTimestamp(boolean state) {
    timestampBase = System.nanoTime() / 1000000;
    useTimestamp = state;
  }

  public static boolean log(String msg) {
    return log(0, msg);
  }

  public static boolean log(int id, String msg) {
    LogInstance log = list.get(id);
    if (log == null) {
      System.out.println(msg);
      return false;
    }
    if (!log.enabled) {
      return true;
    }
    if (!useTimestamp) {
      Calendar cal = Calendar.getInstance();
      msg = String.format("[%1$04d/%2$02d/%3$02d %4$02d:%5$02d:%6$02d] %7$s\r\n",
              cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
              cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), msg);
    } else {
      msg = String.format("[%1$d] %2$s\r\n", (System.nanoTime() / 1000000) - timestampBase, msg);
    }
    synchronized (log.lock) {
      if (log.stdout != null) {
        log.stdout.print(msg);
      }
      if (log.fos == null) return true;
      try {
        log.fos.write(msg.getBytes());
        log.fos.flush();
      } catch (Exception e) {
        System.out.println("Log:write file failed:" + id);
        return false;
      }
      log.filesize += msg.length();
      if (log.filesize > 1024 * 1024) {
        log.filesize = 0;
        //start new log file
        Calendar cal = Calendar.getInstance();
        String tmp = String.format(".%1$04d-%2$02d-%3$02d-%4$02d-%5$02d-%6$02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        int idx = log.filename.lastIndexOf('.');
        if (idx == -1) {
          tmp = log.filename + tmp;
        } else {
          tmp = log.filename.substring(0, idx) + tmp + log.filename.substring(idx);
        }
        try {
          log.fos.close();
        } catch (Exception e1) {
        }
        File file = new File(log.filename);
        file.renameTo(new File(tmp));
        try {
          log.fos = new FileOutputStream(log.filename);
        } catch (Exception e2) {
        }
      }
    }
    return true;
  }

  public static boolean log(Throwable t) {
    return log(0, t);
  }

  public static boolean log(int id, Throwable t) {
    StringBuilder buf = new StringBuilder();
    buf.append(t.toString());
    buf.append("\r\n");
    StackTraceElement ste[] = t.getStackTrace();
    int start = 0;
    if (t instanceof TraceException) {
      start = 1;  //skip JFLog.logTrace() step
    }
    if (ste != null) {
      for (int a = start; a < ste.length; a++) {
        buf.append("\tat ");
        buf.append(ste[a].toString());
        buf.append("\r\n");
      }
    }
    return log(id, buf.toString());
  }

  public static boolean log(String msg, Throwable t) {
    return log(0, msg, t);
  }

  public static boolean log(int id, String msg, Throwable t) {
    if (!log(id, msg)) {
      return false;
    }
    return log(id, t);
  }

  public static void setEnabled(int id, boolean state) {
    LogInstance log = list.get(id);
    if (log == null) {
      return;
    }
    log.enabled = state;
  }

  public static void setEnabled(boolean state) {
    setEnabled(0, state);
  }

  /**
   * NOTE: write() doesn't cycle log files.
   */
  public static boolean write(int id, byte data[], int off, int len) {
    LogInstance log = list.get(id);
    synchronized (log.lock) {
      try {
        log.fos.write(data, off, len);
        log.fos.flush();
        log.filesize += len;
      } catch (Exception e) {
        return false;
      }
    }
    return true;
  }

  /**
   * NOTE: write() doesn't cycle log files.
   */
  public static boolean write(byte data[], int off, int len) {
    return write(0, data, off, len);
  }

  public static OutputStream getOutputStream(int id) {
    LogInstance log = list.get(id);
    if (log == null) {
      return null;
    }
    return log.fos;
  }

  public static OutputStream getOutputStream() {
    return getOutputStream(0);
  }

  public static void logTrace(int id, String msg) {
    try {
      throw new TraceException(msg);
    } catch (Exception e) {
      log(id, e);
    }
  }

  public static void logTrace(String msg) {
    try {
      throw new TraceException(msg);
    } catch (Exception e) {
      log(0, e);
    }
  }

}
