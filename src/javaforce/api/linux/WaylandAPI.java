package javaforce.api.linux;

import java.awt.*;

import javaforce.ffm.*;

/** EXPERIMENTAL Wayland native API
 *
 * @author pquiring
 */

@NativeLibrary("wayland-server,wayland-client,wlroots")
public interface WaylandAPI {

  public static WaylandAPI getInstance() {
    return WaylandFFM.getInstance();
  }

  //internal

  public long wl_get_id(Window window);

  //wayland (server)

  public long wl_display_create();
  public long wl_event_loop_create();
  public long wl_event_loop_add_signal(long event_loop, int signal, long handler, long data);
  public long wl_display_get_event_loop(long display);
  public String wl_display_add_socket_auto(long display);
  public void wl_display_run(long display);

  public boolean wl_display_destroy(long display);

  //wayland (client)

  public long wl_display_connect(String name);
  public long wl_display_connect_to_fd(int fd);

  //wlroots

  public long wlr_session_create(long display);
  public long wlr_fixes_create(long display, int value);
  public long wlr_backend_autocreate(long event_loop, long session_ptr);
  public void wlr_backend_start(long backend);
  public long wlr_renderer_autocreate(long backend);
  public long wlr_compositor_create(long display, int version, long renderer);
  public long wlr_xwayland_create(long display, long compositor, boolean lazy);

  public void wlr_xwayland_destroy(long xwayland);
  public void wlr_backend_destroy(long backend);
}
