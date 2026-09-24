package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** xdg_activation_v1 object.
 *
 * @author pquiring
 */

public class XDGActivation extends WLObject {
  @SuppressWarnings("unchecked")
  public XDGActivation(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("get_activation_token", new Class[] {int.class}),
        cls.getMethod("activate", new Class[] {String.class, int.class}),
      };
      events = new Method[] {
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "xdg_activation_v1";
  }

  //requests

  public void destroy() {
    if (debug) client.log("XDGActivation.destroy");
    invokeRequest(id, 0);
  }

  public void get_activation_token(int new_id) {
    if (debug) client.log("XDGActivation.get_activation_token:new_id=" + new_id);
    invokeRequest(id, 1, new_id);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void activate(String token, int wl_surface) {
    if (debug) client.log("XDGActivation.activate");
    invokeRequest(id, 2, token, wl_surface);
  }

  //events
}
