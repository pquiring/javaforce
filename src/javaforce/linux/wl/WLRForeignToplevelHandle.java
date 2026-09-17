package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wlr_foreign_toplevel_handle_v1 object.
 *
 * @author pquiring
 */

public class WLRForeignToplevelHandle extends WLObject {
  @SuppressWarnings("unchecked")
  public WLRForeignToplevelHandle(WLClient client, int id, WLRForeignToplevelManager manager) {
    super(client, id);
    this.manager = manager;
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("set_maximized", new Class[] {}),
        cls.getMethod("unset_maximized", new Class[] {}),
        cls.getMethod("set_minimized", new Class[] {}),
        cls.getMethod("unset_minimized", new Class[] {}),
        cls.getMethod("activate", new Class[] {int.class}),
        cls.getMethod("close", new Class[] {}),
        cls.getMethod("set_rectangle", new Class[] {int.class, int.class, int.class, int.class, int.class}),
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("set_fullscreen", new Class[] {int.class}),
        cls.getMethod("unset_fullscreen", new Class[] {}),
      };
      events = new Method[] {
        cls.getMethod("title", new Class[] {String.class}),
        cls.getMethod("app_id", new Class[] {String.class}),
        cls.getMethod("output_enter", new Class[] {int.class}),
        cls.getMethod("output_leave", new Class[] {int.class}),
        cls.getMethod("state", new Class[] {int.class}),
        cls.getMethod("done", new Class[] {}),
        cls.getMethod("closed", new Class[] {}),
        cls.getMethod("parent", new Class[] {int.class}),
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private WLRForeignToplevelManager manager;
  private String title;
  private String app_id;

  public String file;  //user defined

  public int getHandle() {
    return id;
  }

  public String get_wl_name() {
    return "zwlr_foreign_toplevel_handle_v1";
  }

  public String getTitle() {
    return title;
  }

  public String getAppID() {
    return app_id;
  }

  //requests

  public void set_maximized() {
    invokeRequest(id, 0);
  }

  public void unset_maximized() {
    invokeRequest(id, 1);
  }

  public void set_minimized() {
    invokeRequest(id, 2);
  }

  public void unset_minimized() {
    invokeRequest(id, 3);
  }

  public void activate(int wl_seat) {
    invokeRequest(id, 4, wl_seat);
  }

  public void close() {
    invokeRequest(id, 5);
  }

  public void set_rectangle(int wl_surface, int x, int y, int width, int height) {
    invokeRequest(id, 6, wl_surface, x, y, width, height);
  }

  public void destroy() {
    invokeRequest(id, 7);
  }

  //V2

  public void set_fullscreen(int wl_output) {
    invokeRequest(id, 8, wl_output);
  }

  public void unset_fullscreen() {
    invokeRequest(id, 9);
  }

  //events

  public void title(String title) {
    this.title = title;
    manager.onWindowChange();
  }

  public void app_id(String app_id) {
    this.app_id = app_id;
    manager.onWindowChange();
  }

  public void output_enter(int wl_output) {

  }

  public void output_leave(int wl_output) {

  }

  public void state(int state) {

  }

  public void done() {

  }

  public void closed() {
    manager.removeWindow(this);
  }

  //V3

  public void parent(int handle) {

  }

}
