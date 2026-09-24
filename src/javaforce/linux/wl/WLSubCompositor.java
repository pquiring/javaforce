package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_subcompositor object.
 *
 * @author pquiring
 */

public class WLSubCompositor extends WLObject {
  @SuppressWarnings("unchecked")
  public WLSubCompositor(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_subsurface", new Class[] {int.class, int.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_subcompositor";
  }

  //requests

  public void destroy() {
    invokeRequest(id, 0);
  }

  public void get_subsurface(int new_id, int wl_surface, int wl_parent) {
    if (debug) client.log("WLSubCompositor.get_subsurface:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_surface, wl_parent);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  //events
}
