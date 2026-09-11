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
    super(client);
    this.id = id;
    Class cls = getClass();
    try {
      events = new Method[] {
        cls.getMethod("done", new Class[] {int.class}),
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getName() {
    return "wl_callback";
  }

  //requests

  //events

  public void done(int callback_data) {
  }
}
