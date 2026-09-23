package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_data_device object.
 *
 * @author pquiring
 */

public class WLDataDevice extends WLObject {
  @SuppressWarnings("unchecked")
  public WLDataDevice(WLClient client, int id) {
    super(client, id);
    setVersion(4);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("start_drag", new Class[] {int.class, int.class, int.class, int.class}),
        cls.getMethod("set_selection", new Class[] {int.class, int.class}),
        cls.getMethod("release", new Class[] {}),
      };
      events = new Method[] {
        cls.getMethod("data_offer", new Class[] {int.class}),
        cls.getMethod("enter", new Class[] {int.class, int.class, int.class, int.class, int.class}),
        cls.getMethod("leave", new Class[] {}),
        cls.getMethod("motion", new Class[] {int.class, int.class, int.class}),
        cls.getMethod("drop", new Class[] {}),
        cls.getMethod("selection", new Class[] {int.class}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_data_device";
  }

  //requests

  public void start_drag(int src, int org, int icon, int serial) {
    if (debug) client.log("WLDataDevice.start_drag:new_id=" + src + "," + org + "," + icon + "," + serial);
    invokeRequest(id, 0, src, org, icon, serial);
  }

  public void set_selection(int src, int serial) {
    if (debug) client.log("WLDataDevice.set_selection:new_id=" + src);
    invokeRequest(id, 1, src, serial);
  }

  //V2

  public void release() {
    if (debug) client.log("WLDataDevice.release");
    invokeRequest(id, 2);
  }

  //events

  public void data_offer(int new_id) {
    if (debug) client.log("WLDataDevice.data_offer:new_id=" + new_id);
  }

  public void enter(int serial, int surface, int x, int y, int id) {
    if (debug) client.log("WLDataDevice.enter");
  }

  public void leave() {
    if (debug) client.log("WLDataDevice.leave");
  }

  public void motion(int time, int x, int y) {
    if (debug) client.log("WLDataDevice.motion");
  }

  public void drop() {
    if (debug) client.log("WLDataDevice.drop");
  }

  public void selection(int new_id) {
    if (debug) client.log("WLDataDevice.selection:new_id=" + new_id);
  }
}
