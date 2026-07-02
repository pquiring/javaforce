package javaforce.api;

import java.awt.*;

import javaforce.linux.*;
import javaforce.ffm.*;

/** Linux X11 API.
 *
 * @author pquiring
 */

public interface X11API {

  public static X11API getInstance() {
    Linux.getInstance();  //load linux libs
    return X11FFM.getInstance();
  }

  public long x11_get_id(Window w);
  public void x11_set_desktop(long xid);
  public void x11_set_dock(long xid);
  public void x11_set_strut(long xid, int panelHeight, int x, int y, int width, int height);
  public void x11_tray_main(long parentid, int screenWidth, int trayPos, int trayHeight);
  public void x11_tray_reposition(int screenWidth, int trayPos, int trayHeight);
  public int x11_tray_width();
  public void x11_tray_stop();
  public void x11_set_listener(X11Listener listener);
  public void x11_window_list_main();
  public void x11_window_list_stop();
  public void x11_minimize_all();
  public void x11_raise_window(long xid);
  public void x11_map_window(long xid);
  public void x11_unmap_window(long xid);
  public int x11_keysym_to_keycode(char keysym);
  public boolean x11_send_event(int keycode, boolean down);
  public boolean x11_send_event_id(long id, int keycode, boolean down);
}
