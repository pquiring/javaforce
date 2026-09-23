package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wp_viewporter object.
 *
 * @author pquiring
 */

public class WPViewporter extends WLObject {
  @SuppressWarnings("unchecked")
  public WPViewporter(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_viewport", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wp_viewporter";
  }

  //requests

  public void destroy() {
    if (debug) client.log("WPViewporter.destroy");
    invokeRequest(id, 0);
  }

  public void get_viewport(int new_id, int wl_surface) {
    if (debug) client.log("WPViewporter.get_viewport:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_surface);
    //TODO : use proper object
    client.setObject(new_id, new WLUnknown(client, new_id));
  }

  //events
}
