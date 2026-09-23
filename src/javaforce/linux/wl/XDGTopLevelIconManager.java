package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** xdg_toplevel_icon_manager_v1 object.
 *
 * @author pquiring
 */

public class XDGTopLevelIconManager extends WLObject {
  @SuppressWarnings("unchecked")
  public XDGTopLevelIconManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("destroy", new Class[] {}),
        cls.getMethod("create_icon", new Class[] {int.class}),
        cls.getMethod("set_icon", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
        cls.getMethod("icon_size", new Class[] {int.class}),
        cls.getMethod("done", new Class[] {}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "xdg_toplevel_icon_manager_v1";
  }

  //requests

  public void destroy() {
    if (debug) client.log("XDGTopLevelIconManager.destroy");
    invokeRequest(id, 0);
  }

  public void create_icon(int new_id) {
    if (debug) client.log("XDGTopLevelIconManager.create_icon:new_id=" + new_id);
    invokeRequest(id, 1, new_id);
  }

  public void set_icon(int xdg_toplevel, int xdg_toplevel_icon) {
    if (debug) client.log("XDGTopLevelIconManager.set_icon");
    invokeRequest(id, 2, xdg_toplevel, xdg_toplevel_icon);
  }

  //events

  public void icon_size(int size) {
    if (debug) client.log("XDGTopLevelIconManager.icon_size");
  }

  public void done() {
    if (debug) client.log("XDGTopLevelIconManager.done");
  }
}
