package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** ext_data_control_manager_v1 object.
 *
 * @author pquiring
 */

public class EXTDataControlManager extends WLObject {
  @SuppressWarnings("unchecked")
  public EXTDataControlManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("create_data_source", new Class[] {int.class}),
        cls.getMethod("get_data_device", new Class[] {int.class, int.class}),
        cls.getMethod("destroy", new Class[] {}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "ext_data_control_manager_v1";
  }

  //requests

  public void create_data_source(int new_id) {
    if (debug) client.log("EXTDataControlManager.create_data_source:new_id=" + new_id);
    invokeRequest(id, 0, new_id);
  }

  public void get_data_device(int new_id, int wl_seat) {
    if (debug) client.log("EXTDataControlManager.get_device:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_seat);
  }

  public void destroy() {
    invokeRequest(id, 2);
  }

  //events
}
