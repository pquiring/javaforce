package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wlr_layer_shell object.
 *
 * @author pquiring
 */

public class WLRLayerShell extends WLObject {
  @SuppressWarnings("unchecked")
  public WLRLayerShell(WLClient client, int id) {
    super(client, id);
    setVersion(3);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("get_layer_surface", new Class[] {int.class, int.class, int.class, int.class, String.class}),
        cls.getMethod("destroy", new Class[] {}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  //wl_layer
  public static final int LAYER_BACKGROUND = 0;
  public static final int LAYER_BOTTOM = 1;
  public static final int LAYER_TOP = 2;
  public static final int LAYER_OVERLAY = 3;

  public String get_wl_name() {
    return "zwlr_layer_shell_v1";
  }

  //requests

  public void get_layer_surface(int new_id, int wl_surface, int wl_output, int wl_layer, String namespace) {
    if (debug) client.log("WLRLayerShell.get_layer_surface:new_id=" + new_id);
    invokeRequest(id, 0, new_id, wl_surface, wl_output, wl_layer, namespace);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  //V3

  public void destroy() {
    invokeRequest(id, 1);
  }

  //events
}
