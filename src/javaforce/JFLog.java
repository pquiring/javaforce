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
    private long retention = -1;
    private long maxfilesize = 1024 * 1024;
  }
  private static HashMap<Integer, LogInstance> list = new HashMap<Integer, LogInstance>();
  private static boolean useTimestamp = false;
  private static long timestampBase;

  //recommended trace ids
  public static final int DEFAULT = 0;
  public static final int TRACE = 1;
  public static final int DEBUG = 2;
  public static final int INFO = 3;
  public static final int WARN = 4;
  public static final int ERROR = 5;

  private static class TraceException extends Exception {
    public TraceException(String msg) {
      super(msg);
    }
  }

  public static boolean init(int id, String filename, boolean append, PrintStream stdout) {
    close(id);
    LogInstance log = new LogInstance();
    log.stdout = stdout;
    if (filename != null) {
      log.filename = filename.replaceAll("\\\\", "/");
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
    return init(DEFAULT, filename, false, stdout ? System.out : null);
  }

  public static boolean init(int id, String filename, boolean stdout) {
    return init(id, filename, false, stdout ? System.out : null);
  }

  public static boolean append(String filename, boolean stdout) {
    return init(DEFAULT, filename, true, stdout ? System.out : null);
  }

  public static boolean append(int id, String filename, boolean stdout) {
    return init(id, filename, true, stdout ? System.out : null);
  }

  public static void setRetention(int id, int days) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
    }
    if (log == null) {
      return;
    }
    log.retention = days;
  }

  public static void setRetention(int days) {
    setRetention(DEFAULT, days);
  }

  private static final long KB = 1024;
  private static final long GB = 1024 * 1024 * 1024;

  /** Sets max file size for log.
   * Once the file is greater the log will rotate()
   */
  public static void setMaxFilesize(int id, long size) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
    }
    if (log == null) {
      return;
    }
    if (size < KB) size = KB;
    if (size > GB) size = GB;
    log.maxfilesize = size;
  }

  /** Sets max file size for log.
   * Once the file is greater the log will rotate()
   */
  public static void setMaxFilesize(long size) {
    setMaxFilesize(DEFAULT, size);
  }

  public static boolean close(int id) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
      if (log == null) {
        return false;
      }
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
    return close(DEFAULT);
  }

  /** Uses a timestamp instead of date/time for each message logged.
   * Timestamp is based on time when this method is called.
   * Effects all logs.
   */
  public static void enableTimestamp(boolean state) {
    timestampBase = System.nanoTime() / 1000000;
    useTimestamp = state;
  }

  public static boolean log(String msg) {
    return log(DEFAULT, msg);
  }

  private static final long ms_per_day = 24 * 60 * 60 * 1000;

  public static boolean log(int id, String msg) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
    }
    if (!useTimestamp) {
      Calendar cal = Calendar.getInstance();
      msg = String.format("[%1$04d/%2$02d/%3$02d %4$02d:%5$02d:%6$02d] %7$s%8$s",
              cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
              cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), msg, JF.eol);
    } else {
      msg = String.format("[%1$d] %2$s%3$s", (System.nanoTime() / 1000000) - timestampBase, msg, JF.eol);
    }
    if (log == null) {
      System.out.print(msg);
      return false;
    }
    if (!log.enabled) {
      return true;
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
      if (log.filesize > log.maxfilesize) {
        rotate(id);
      }
    }
    return true;
  }

  /** Rotates log file. */
  public static boolean rotate(int id) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
    }
    if (log == null) {
      return false;
    }
    if (log.filename == null) {
      return false;
    }
    if (log.filesize == 0) {
      return false;
    }
    synchronized (log.lock) {
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
      if (log.retention != -1) {
        File files[] = null;
        int fidx = log.filename.lastIndexOf('/');
        if (fidx == -1) {
          //no path
          files = new File(".").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return (name.startsWith(log.filename));
            }
          });
        } else {
          String folder = log.filename.substring(0, fidx);
          String filename = null;
          if (idx == -1) {
            filename = log.filename.substring(fidx + 1);
          } else {
            filename = log.filename.substring(fidx + 1, idx);
          }
          String fn = filename;
          files = new File(folder).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return name.startsWith(fn);
            }
          });
        }
        if (files != null) {
          //delete files older than retention
          long timestamp = System.currentTimeMillis() - (log.retention * ms_per_day);
          for(File logfile : files) {
            if (logfile.lastModified() < timestamp) {
              logfile.delete();
            }
          }
        }
      }
      try {
        log.fos = new FileOutputStream(log.filename);
      } catch (Exception e2) {
      }
    }
    return true;
  }

  /** Rotates log file. */
  public static boolean rotate() {
    return rotate(DEFAULT);
  }

  public static boolean log(Throwable t) {
    return log(DEFAULT, t);
  }

  public static boolean log(int id, Throwable t) {
    StringBuilder buf = new StringBuilder();
    buf.append(t.toString());
    buf.append(JF.eol);
    StackTraceElement ste[] = t.getStackTrace();
    int start = 0;
    if (t instanceof TraceException) {
      start = 1;  //skip JFLog.logTrace() step
    }
    if (ste != null) {
      for (int a = start; a < ste.length; a++) {
        buf.append("\tat ");
        buf.append(ste[a].toString());
        buf.append(JF.eol);
      }
    }
    return log(id, buf.toString());
  }

  public static boolean log(String msg, Throwable t) {
    return log(DEFAULT, msg, t);
  }

  public static boolean log(int id, String msg, Throwable t) {
    if (!log(id, msg)) {
      return false;
    }
    return log(id, t);
  }

  public static void setEnabled(int id, boolean state) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
      if (log == null) {
        log = new LogInstance();
        list.put(id, log);
      }
      log.enabled = state;
    }
  }

  public static void setEnabled(boolean state) {
    setEnabled(DEFAULT, state);
  }

  /**
   * NOTE: write() doesn't cycle log files.
   */
  public static boolean write(int id, byte data[], int off, int len) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
    }
    if (log == null) return false;
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
    return write(DEFAULT, data, off, len);
  }

  public static OutputStream getOutputStream(int id) {
    LogInstance log;
    synchronized(list) {
      log = list.get(id);
    }
    if (log == null) {
      return null;
    }
    return log.fos;
  }

  public static OutputStream getOutputStream() {
    return getOutputStream(DEFAULT);
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
      log(DEFAULT, e);
    }
  }
}
