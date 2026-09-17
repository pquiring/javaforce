package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_buffer object.
 *
 * @author pquiring
 */

public class WLBuffer extends WLObject {
  @SuppressWarnings("unchecked")
  public WLBuffer(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
      };
      events = new Method[] {
        cls.getMethod("release", new Class[] {}),
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_buffer";
  }

  //requests

  public void destroy() {
    invokeRequest(id, 0);
  }

  //events

  public void release() {
  }
}
