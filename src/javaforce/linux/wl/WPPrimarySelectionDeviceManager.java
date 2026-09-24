package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** zwp_primary_selection_device_manager_v1 object.
 *
 * @author pquiring
 */

public class WPPrimarySelectionDeviceManager extends WLObject {
  @SuppressWarnings("unchecked")
  public WPPrimarySelectionDeviceManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("create_source", new Class[] {int.class}),
        cls.getMethod("get_device", new Class[] {int.class, int.class}),
        cls.getMethod("destroy", new Class[] {}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "zwp_primary_selection_device_manager_v1";
  }

  //requests

  public void create_source(int new_id) {
    if (debug) client.log("WPPrimarySelectionDeviceManager.create_source:new_id=" + new_id);
    invokeRequest(id, 0, new_id);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void get_device(int new_id, int wl_seat) {
    if (debug) client.log("WPPrimarySelectionDeviceManager.get_device:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_seat);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void destroy() {
    invokeRequest(id, 2);
  }

  //events
}
