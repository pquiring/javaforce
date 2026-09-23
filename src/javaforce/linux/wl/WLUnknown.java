package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_unknown object.
 *
 * @author pquiring
 */

public class WLUnknown extends WLObject {
  @SuppressWarnings("unchecked")
  public WLUnknown(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_unknown";
  }

  //requests

  //events
}
