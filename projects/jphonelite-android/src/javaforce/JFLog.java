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
    private boolean stdout;
    private long filesize;
    private String filename;
    private boolean enabled = true;
  }
  private static Hashtable<Integer, LogInstance> list = new Hashtable<Integer, LogInstance>();

  public static boolean init(int id, String filename, boolean stdout, boolean append) {
    LogInstance log = new LogInstance();
    log.stdout = stdout;
    log.filename = filename;
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
    list.put(id, log);
    return true;
  }

  public static boolean init(String filename, boolean stdout) {
    return init(0, filename, stdout, false);
  }

  public static boolean init(int id, String filename, boolean stdout) {
    return init(id, filename, stdout, false);
  }

  public static boolean append(String filename, boolean stdout) {
    return init(0, filename, stdout, true);
  }

  public static boolean append(int id, String filename, boolean stdout) {
    return init(id, filename, stdout, true);
  }

  public static boolean close(int id) {
    LogInstance log = list.get(id);
    if (log == null) {
      return false;
    }
    list.remove(id);
    try {
      log.fos.close();
    } catch (Exception e) {
    }
    return true;
  }

  public static boolean close() {
    return close(0);
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
    Calendar cal = Calendar.getInstance();
    msg = String.format("[%1$04d/%2$02d/%3$02d %4$02d:%5$02d:%6$02d] %7$s\r\n",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), msg);
    synchronized (log.lock) {
      if (log.stdout) {
        System.out.print(msg);
      }
      try {
        log.fos.write(msg.getBytes());
      } catch (Exception e) {
        System.out.println("Log:write file failed:" + id);
        return false;
      }
      log.filesize += msg.length();
      if (log.filesize > 1024 * 1024) {
        log.filesize = 0;
        //start new log file
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
    StringBuffer buf = new StringBuffer();
    buf.append(t.toString());
    buf.append("\r\n");
    StackTraceElement ste[] = t.getStackTrace();
    if (ste != null) {
      for (int a = 0; a < ste.length; a++) {
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
}
