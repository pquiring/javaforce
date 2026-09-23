package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** zwp_text_input_manager_v3 object.
 *
 * @author pquiring
 */

public class WPTextInputManager extends WLObject {
  @SuppressWarnings("unchecked")
  public WPTextInputManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_text_input", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "zwp_text_input_manager_v3";
  }

  //requests

  public void destroy() {
    invokeRequest(id, 0);
  }

  public void get_text_input(int new_id, int wl_seat) {
    if (debug) client.log("WPTextInputManager.get_text_input:new_id=" + new_id);
    invokeRequest(id, 1, new_id, wl_seat);
    //TODO : use proper object
    client.setObject(new_id, new WLUnknown(client, new_id));
  }

  //events
}
