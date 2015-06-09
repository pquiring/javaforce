package javaforce.utils;

/**
 * Monitor Dir (Linux : inotify)
 *
 * Uses JNA
 *
 * @author pquiring
 *
 * Created : Nov 3, 2013
 */

import java.util.*;

import javaforce.*;
import javaforce.jni.*;


public class monitordir {
  public static interface Listener {
    public void folderChangeEvent(String event, String path);
  }
  /** Loads native library.  Only need to call once per process. */
  private static int fd;
  public static boolean init() {
    try {
      fd = LnxNative.inotify_init();
      if (fd == -1) throw new Exception("inotify_init failed");
      new Worker().start();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  /** Stops all watches. */
  public static void uninit() {
    active = false;
    LnxNative.inotify_close(fd);
  }
  /** Start watching a folder. */
  public static int add(String path) {
    int wd = LnxNative.inotify_add_watch(fd, path, IN_ALL);
//    JFLog.log("wd=" + wd);
    return wd;
  }
  /** Stops watching a folder. */
  public static void remove(int wd) {
    LnxNative.inotify_rm_watch(fd, wd);
    map.remove(wd);
  }
  private static boolean active = true;
  private static HashMap<Integer, Listener> map = new HashMap<Integer, Listener>();
  public static void setListener(int wd, Listener listener) {
    map.put(wd, listener);
  }
  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage:jf-monitor-dir folder");
      return;
    }
    if (!init()) return;
    add(args[0]);
    try { new Object().wait(); } catch (Exception e) {}  //wait forever
  }
  public static class Worker extends Thread {
    public void run() {
//      JFLog.log("worker start");
      while (active) {
        byte data[] = LnxNative.inotify_read(fd);
        int pos = 0;
        int siz = data.length;
        while (siz > 12) {
          int _wd = LE.getuint32(data, pos);
          pos += 4;
          int _mask = LE.getuint32(data, pos);
          pos += 4;
          //cookie
          pos += 4;
          int _len = LE.getuint32(data, pos);
          pos += 4;
          String _name = _len > 0 ? LE.getString(data, pos, _len) : null;
          siz -= 16 + _len;
          String _event = null;
          switch (_mask & IN_ALL) {
            case IN_CREATE: _event = "CREATED"; break;
            case IN_DELETE: _event = "DELETED"; break;
            case IN_MOVED_FROM: _event = "MOVED_FROM"; break;
            case IN_MOVED_TO: _event = "MOVED_TO"; break;
            case IN_DELETE_SELF: _event = "DELETE_SELF"; break;
            case IN_MOVED_SELF: _event = "MOVED_SELF"; break;
          }
          if (_event == null) continue;
          Listener listener = map.get(_wd);
          if (listener != null) {
            listener.folderChangeEvent(_event, _name);
          } else {
            JFLog.log(_event + ":" + _name);
          }
        }
      }
//      JFLog.log("worker end");
    }
  }
  private static final int IN_MOVED_FROM = 0x040;
  private static final int IN_MOVED_TO = 0x080;
  private static final int IN_CREATE = 0x100;
  private static final int IN_DELETE = 0x200;
  private static final int IN_DELETE_SELF = 0x0400;
  private static final int IN_MOVED_SELF = 0x0800;
  private static final int IN_ALL = 0xfc0;
/*
  public class inotify_event extends Structure {
     public int wd;       // Watch descriptor
     public int mask;     // Mask of events
     public int cookie;   // Unique cookie associating related
                        //   events (for rename(2))
     public int len;      // Size of name field
     public char name[];   // Optional null-terminated name
   };
*/
}
