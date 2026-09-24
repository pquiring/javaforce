package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** ext_data_control_device_v1 object.
 *
 * @author pquiring
 */

public class EXTDataControlDevice extends WLObject {
  @SuppressWarnings("unchecked")
  public EXTDataControlDevice(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("set_selection", new Class[] {int.class}),
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("set_primary_selection", new Class[] {int.class}),
      };
      events = new Method[] {
        cls.getMethod("data_offer", new Class[] {int.class}),
        cls.getMethod("selection", new Class[] {int.class}),
        cls.getMethod("finished", new Class[] {}),
        cls.getMethod("primary_selection", new Class[] {int.class}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "ext_data_control_device_v1";
  }

  //requests

  public void set_selection(int src) {
    if (debug) client.log("EXTDataControlDevice.set_selection");
    invokeRequest(id, 0, src);
  }

  public void destroy() {
    invokeRequest(id, 1);
  }

  public void set_primary_selection(int src) {
    if (debug) client.log("EXTDataControlDevice.set_primary_selection");
    invokeRequest(id, 1, src);
  }

  //events

  public void data_offer(int new_id) {
    if (debug) client.log("EXTDataControlDevice.data_offer:new_id=" + new_id);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void selection(int src) {
    if (debug) client.log("EXTDataControlDevice.selection");
  }

  public void finished() {
    if (debug) client.log("EXTDataControlDevice.finished");
  }

  public void primary_selection(int src) {
    if (debug) client.log("EXTDataControlDevice.primary_selection");
  }

}
