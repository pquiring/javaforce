package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** zxdg_output_manager_v1 object.
 *
 * @author pquiring
 */

public class XDGOutputManager extends WLObject {
  @SuppressWarnings("unchecked")
  public XDGOutputManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_xdg_output", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "zxdg_output_manager_v1";
  }

  //requests

  public void destroy() {
    if (debug) client.log("XDGOutputManager.destroy");
    invokeRequest(id, 0);
  }

  public void get_xdg_output(int new_id, int wl_output) {
    if (debug) client.log("XDGOutputManager.get_xdg_output:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_output);
    //TODO : use proper object
    client.setObject(new_id, new WLUnknown(client, new_id));
  }

  //events
}
