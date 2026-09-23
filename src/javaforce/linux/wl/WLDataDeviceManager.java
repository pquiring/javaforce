package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_data_device_manager object.
 *
 * @author pquiring
 */

public class WLDataDeviceManager extends WLObject {
  @SuppressWarnings("unchecked")
  public WLDataDeviceManager(WLClient client, int id) {
    super(client, id);
    setVersion(4);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("create_data_source", new Class[] {int.class}),
        cls.getMethod("get_data_device", new Class[] {int.class, int.class}),
        cls.getMethod("release", new Class[] {}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_data_device_manager";
  }

  //requests

  public void create_data_source(int new_id) {
    if (debug) client.log("WLDataDeviceManager.create_data_source:new_id=" + new_id);
    invokeRequest(id, 0, new_id);
    //TODO : use proper object
    client.setObject(new_id, new WLUnknown(client, new_id));
  }

  public void get_data_device(int new_id, int wl_seat) {
    if (debug) client.log("WLDataDeviceManager.get_data_device:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_seat);
    //TODO : use proper object
    client.setObject(new_id, new WLUnknown(client, new_id));
  }

  //V4

  public void release() {
    if (debug) client.log("WLDataDeviceManager.release");
    invokeRequest(id, 2);
  }

  //events
}
