package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** zwp_relative_pointer_manager_v1 object.
 *
 * @author pquiring
 */

public class WPRelativePointerManager extends WLObject {
  @SuppressWarnings("unchecked")
  public WPRelativePointerManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_relative_pointer", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "zwp_relative_pointer_manager_v1";
  }

  //requests

  public void destroy() {
    if (debug) client.log("WPRelativePointerManager.destroy");
    invokeRequest(id, 0);
  }

  public void get_relative_pointer(int new_id, int wl_pointer) {
    if (debug) client.log("WPRelativePointerManager.get_relative_pointer:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_pointer);
  }

  //events
}
