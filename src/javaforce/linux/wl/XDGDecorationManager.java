package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** zxdg_decoration_manager_v1 object.
 *
 * @author pquiring
 */

public class XDGDecorationManager extends WLObject {
  @SuppressWarnings("unchecked")
  public XDGDecorationManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_toplevel_decoration", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "zxdg_decoration_manager_v1";
  }

  //requests

  public void destroy() {
    if (debug) client.log("XDGDecorationManager.destroy");
    invokeRequest(id, 0);
  }

  public void get_toplevel_decoration(int new_id, int xdg_toplevel) {
    if (debug) client.log("XDGDecorationManager.get_toplevel_decoration:new_id=" + new_id);
    invokeRequest(id, 1, new_id, xdg_toplevel);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  //events
}
