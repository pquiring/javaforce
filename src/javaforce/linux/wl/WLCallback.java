package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_callback object.
 *
 * @author pquiring
 */

public class WLCallback extends WLObject {
  @SuppressWarnings("unchecked")
  public WLCallback(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
      };
      events = new Method[] {
        cls.getMethod("done", new Class[] {int.class}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_callback";
  }

  //requests

  //events

  public void done(int callback_data) {
  }
}
