package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wlr_foreign_toplevel_handle_v1 object.
 *
 * @author pquiring
 */

public class WLRForeignToplevelHandle extends WLObject {
  @SuppressWarnings("unchecked")
  public WLRForeignToplevelHandle(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
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

  public String getName() {
    return "zwlr_foreign_toplevel_handle_v1";
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

  public void activate() {
    invokeRequest(id, 4);
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

  public void unset_fullscreen(int wl_output) {
    invokeRequest(id, 9, wl_output);
  }

  //events

  public void title(String title) {

  }

  public void app_id(String app_id) {

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

  }

  //V3

  public void parent(int handle) {

  }

}
