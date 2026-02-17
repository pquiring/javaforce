package javaforce.jni;

/** X11 native JNI methods
 *
 * @author pquiring
 */

import java.awt.*;

import javaforce.linux.*;

public class X11 {
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
}
