package javaforce.jni;

/**
 *
 * @author pquiring
 */

import java.io.*;
import java.awt.*;

import javaforce.linux.*;

public class LnxNative {
  static {
    JFNative.load();  //ensure native library is loaded
    if (JFNative.loaded && JFNative.load_ffmpeg) {
      Library libs[] = {new Library("libX11"), new Library("libGL"), new Library("libv4l2")};
      if (!JFNative.findLibraries(new File[] {new File("/usr/lib/x86_64-linux-gnu")}, libs, ".so", libs.length)) {
        for(int a=0;a<libs.length;a++) {
          if (libs[a].path == null) {
            System.out.println("Warning:Unable to find library:" + libs[a].name + ".so");
            if (libs[a].name.equals("libX11")) {
              have_x11 = false;
            }
            else if (libs[a].name.equals("libGL")) {
              have_gl = false;
            }
            else if (libs[a].name.equals("libfuse")) {
              have_fuse = false;
            }
            else if (libs[a].name.equals("libv4l2")) {
              have_v4l2 = false;
            }
          }
        }
      }
      lnxInit(libs[0].path, libs[1].path, libs[2].path);
    }
  }

  public static void load() {}  //ensure native library is loaded

  private static native boolean lnxInit(String libX11, String libGL, String libv4l2);

  public static boolean have_x11 = true;
  public static boolean have_gl = true;
  public static boolean have_fuse = true;
  public static boolean have_v4l2 = true;

  //com port
  public static native int comOpen(String name, int baud);  //assumes 8 data bits, 1 stop bit, no parity, etc.
  public static native void comClose(int handle);
  public static native int comRead(int handle, byte buf[]);
  public static native int comWrite(int handle, byte buf[]);

  //pty
  public static native long ptyAlloc();
  public static native void ptyFree(long ctx);  //free resources on parent side
  public static native String ptyOpen(long ctx);  //creates a pty and returns the slaveName (one use per ctx)
  public static native void ptyClose(long ctx);  //close pty
  public static native int ptyRead(long ctx, byte data[]);  //read child output on parent side
  public static native void ptyWrite(long ctx, byte data[]);  //write to child on parent side
  public static native void ptySetSize(long ctx, int x, int y);  //set child term size
  public static native long ptyChildExec(String slaveName, String cmd, String args[], String env[]);  //spawn child process

  //inotify (monitordir)
  public static native int inotify_init();  //return fd
  public static native int inotify_add_watch(int fd, String path, int mask);  //return wd
  public static native int inotify_rm_watch(int fd, int wd);
  public static native byte[] inotify_read(int fd);
  public static native void inotify_close(int fd);

  //X11
  public static native long x11_get_id(Window w);
  public static native void x11_set_desktop(long xid);
  public static native void x11_set_dock(long xid);
  public static native void x11_set_strut(long xid, int panelHeight, int x, int y, int width, int height);
  public static native void x11_tray_main(long parentid, int screenWidth, int trayPos, int trayHeight);
  public static native void x11_tray_reposition(int screenWidth, int trayPos, int trayHeight);
  public static native int x11_tray_width();
  public static native void x11_tray_stop();
  public static native void x11_set_listener(X11Listener listener);
  public static native void x11_window_list_main();
  public static native void x11_window_list_stop();
  public static native void x11_minimize_all();
  public static native void x11_raise_window(long xid);
  public static native void x11_map_window(long xid);
  public static native void x11_unmap_window(long xid);
  public static native int x11_keysym_to_keycode(char keysym);
  public static native boolean x11_send_event(int keycode, boolean down);
  public static native boolean x11_send_event(long id, int keycode, boolean down);

  //PAM (Pluggable Authentication Modules for Linux)
  public static native boolean authUser(String user, String pass);

  //setenv
  public static native void setenv(String name, String value);

  //console
  public static native void enableConsoleMode();
  public static native void disableConsoleMode();
  public static native int[] getConsoleSize();
  public static native int[] getConsolePos();
  public static native char readConsole();
  public static native boolean peekConsole();
}
