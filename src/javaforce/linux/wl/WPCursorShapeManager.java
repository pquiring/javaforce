package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wp_cursor_shape_manager_v1 object.
 *
 * @author pquiring
 */

public class WPCursorShapeManager extends WLObject {
  @SuppressWarnings("unchecked")
  public WPCursorShapeManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_pointer", new Class[] {int.class, int.class}),
        cls.getMethod("get_tablet_tool_v2", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wp_cursor_shape_manager_v1";
  }

  //requests

  public void destroy() {
    invokeRequest(id, 0);
  }

  public void get_pointer(int new_id, int wl_pointer) {
    if (debug) client.log("WPCursorShapeManager.get_pointer:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_pointer);
    //TODO : use proper object
    client.setObject(new_id, new WLUnknown(client, new_id));
  }

  public void get_tablet_tool_v2(int new_id, int wl_tool) {
    if (debug) client.log("WPCursorShapeManager.get_tablet_tool_v2:new_id=" + new_id);
    invokeRequest(id, 2, new_id, wl_tool);
    //TODO : use proper object
    client.setObject(new_id, new WLUnknown(client, new_id));
  }

  //events
}
