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
      events = new Method[] {
        cls.getMethod("error", new Class[] {int.class, int.class, String.class}),
        cls.getMethod("delete_id", new Class[] {int.class}),
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getName() {
    return "wl_display";
  }

  //requests

  public void sync() {
    if (debug) JFLog.log("sync");
    int new_id = client.get_next_id();
    invokeRequest(id, 0, new_id);
  }

  public WLRegistry get_registry(WLNotify notify) {
    if (debug) JFLog.log("get_registry");
    int new_id = client.get_next_id();
    WLRegistry registry = new WLRegistry(client, new_id);
    registry.setNotify(notify);
    client.setObject(new_id, registry);
    invokeRequest(id, 1, new_id);
    return registry;
  }

  //events

  public void error(int obj_id, int error_code, String msg) {
    JFLog.log("Wayland Error:object=" + obj_id + ":error_code=" + error_code + ":" + msg);
  }

  public void delete_id(int old_id) {
    client.removeObject(old_id);
  }
}
