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
    super(client);
    this.id = id;
    Class cls = getClass();
    try {
      events = new Method[] {
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //requests

  public void create_surface(int new_id) {
    invokeRequest(id, 0, new_id);
  }

  public void create_region(int new_id) {
    invokeRequest(id, 1, new_id);
  }

  public void release() {
    invokeRequest(id, 2);
  }

  //events
}
