package javaforce.linux;

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.api.linux.*;
import javaforce.ffm.FFM;
import static javaforce.linux.Linux.*;

/** Wayland Compositor.
 *
 * WIP (incomplete)
 *
 * Based on wlroots.
 *
 * @author pquiring
 */

public class Wayland {
  private static boolean debug = true;
  public boolean start() {
    WaylandAPI api = WaylandAPI.getInstance();
    Arena arena = Arena.ofAuto();
    long display = api.wl_display_create();
    if (debug) JFLog.log("display=" + display);
    if (display == 0) {
      JFLog.log("Wayland:wl_display_create() failed!");
      return false;
    }
    long event_loop = api.wl_display_get_event_loop(display);
    if (debug) JFLog.log("event_loop=" + event_loop);
    if (event_loop == 0) {
      JFLog.log("Wayland:wl_event_loop_create() failed!");
      return false;
    }
    if (false) {
      api.wlr_fixes_create(display, 1);
    }
    //setup signals
    MemorySegment handle_signal = FFM.getFunctionUpCall(this, "handle_signal", int.class, new Class[] {int.class, long.class}, arena);
    api.wl_event_loop_add_signal(event_loop, SIGHUP, handle_signal.address(), display);
    api.wl_event_loop_add_signal(event_loop, SIGINT, handle_signal.address(), display);
    api.wl_event_loop_add_signal(event_loop, SIGTERM, handle_signal.address(), display);
    api.wl_event_loop_add_signal(event_loop, SIGCHLD, handle_signal.address(), display);
    LinuxAPI.getInstance().setEnv("LIBSEAT_BACKEND", "logind");
    long session = 0;
    MemorySegment session_ptr = arena.allocateFrom(JAVA_LONG, session);
    long backend = api.wlr_backend_autocreate(event_loop, session_ptr.address());
    if (debug) JFLog.log("backend=" + backend);
    if (backend == 0) {
      JFLog.log("Wayland:wlr_backend_autocreate() failed!");
      return false;
    }
    if (session_ptr.address() != 0) {
      session = session_ptr.get(JAVA_LONG, 0);
      if (debug) JFLog.log("session=" + session);
    }
    api.wlr_backend_start(backend);
    long renderer = api.wlr_renderer_autocreate(backend);
    if (debug) JFLog.log("renderer=" + renderer);
    if (renderer == 0) {
      JFLog.log("Wayland:wlr_renderer_autocreate() failed!");
      return false;
    }
    long compositor = api.wlr_compositor_create(display, 6, renderer);
    if (debug) JFLog.log("compositor=" + compositor);
    if (compositor == 0) {
      JFLog.log("Wayland:wlr_compositor_create() failed!");
      return false;
    }
    long xwayland = api.wlr_xwayland_create(display, compositor, true);
    if (debug) JFLog.log("xwayland=" + xwayland);
    String socket = api.wl_display_add_socket_auto(display);
    if (debug) JFLog.log("WAYLAND_SOCKET=" + socket);
    LinuxAPI.getInstance().setEnv("WAYLAND_DISPLAY", socket);
    if (debug) JFLog.log("wl_display_run");
    api.wl_display_run(display);
    if (debug) JFLog.log("wayland shutdown...");
    api.wlr_xwayland_destroy(xwayland);
    api.wlr_backend_destroy(backend);
    api.wl_display_destroy(display);
    return true;
  }
  public int handle_signal(int sig, long data) {
    return 0;
  }
  public void stop() {
    //TODO
  }
}
