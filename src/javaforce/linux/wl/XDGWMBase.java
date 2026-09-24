package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** xdg_wm_base object.
 *
 * @author pquiring
 */

public class XDGWMBase extends WLObject {
  @SuppressWarnings("unchecked")
  public XDGWMBase(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("create_positioner", new Class[] {int.class}),
        cls.getMethod("get_xdg_surface", new Class[] {int.class, int.class}),
        cls.getMethod("pong", new Class[] {int.class}),
      };
      events = new Method[] {
        cls.getMethod("ping", new Class[] {int.class}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "xdg_wm_base";
  }

  //requests

  public void destroy() {
    if (debug) client.log("XDGWMBase.destroy");
    invokeRequest(id, 0);
  }

  public void create_positioner(int new_id) {
    if (debug) client.log("XDGWMBase.create_positioner:new_id=" + new_id);
    invokeRequest(id, 1, new_id);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void get_xdg_surface(int new_id, int wl_surface) {
    if (debug) client.log("XDGWMBase.get_xdg_surface:new_id=" + new_id);
    invokeRequest(id, 2, new_id, wl_surface);
    //TODO : use proper object
    new WLUnknown(client, new_id);
  }

  public void pong(int serial) {
    if (debug) client.log("XDGWMBase.pong");
    invokeRequest(id, 3, serial);
  }

  //events

  public void ping(int serial) {
    if (debug) client.log("XDGWMBase.ping");
  }
}
