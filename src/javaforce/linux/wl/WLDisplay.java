package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_display object.
 *
 * @author pquiring
 */

public class WLDisplay extends WLObject {
  @SuppressWarnings("unchecked")
  public WLDisplay(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("sync", new Class[] {int.class}),
        cls.getMethod("get_registry", new Class[] {int.class}),
      };
      events = new Method[] {
        cls.getMethod("error", new Class[] {int.class, int.class, String.class}),
        cls.getMethod("delete_id", new Class[] {int.class}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  private WLRegistry registry;

  public String get_wl_name() {
    return "wl_display";
  }

  //requests

  public void sync(int new_id) {
    if (debug) client.log("WLDisplay.sync:new_id=" + new_id);
    invokeRequest(id, 0, new_id);
  }

  public WLRegistry get_registry(int new_id) {
    if (registry != null) {
      return registry;
    }
    if (debug) client.log("WLDisplay:get_registry:new_id=" + new_id);
    registry = new WLRegistry(client, new_id);
    registry.setNotify(notify);
    client.setObject(new_id, registry);
    invokeRequest(id, 1, new_id);
    return registry;
  }

  //events

  public void error(int obj_id, int error_code, String msg) {
    client.log("Wayland Error:object=" + obj_id + ":error_code=" + error_code + ":" + msg);
  }

  public void delete_id(int old_id) {
    client.removeObject(old_id);
  }
}
