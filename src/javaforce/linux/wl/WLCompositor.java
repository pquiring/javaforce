package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_compositor object.
 *
 * @author pquiring
 */

public class WLCompositor extends WLObject {
  @SuppressWarnings("unchecked")
  public WLCompositor(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("create_surface", new Class[] {int.class}),
        cls.getMethod("create_region", new Class[] {int.class}),
        cls.getMethod("release", new Class[] {}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_compositor";
  }

  //requests

  public void create_surface(int new_id) {
    if (debug) client.log("WLCompositor.create_surface:new_id=" + new_id);
    invokeRequest(id, 0, new_id);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void create_region(int new_id) {
    if (debug) client.log("WLCompositor.create_region:new_id=" + new_id);
    invokeRequest(id, 1, new_id);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void release() {
    invokeRequest(id, 2);
  }

  //events
}
