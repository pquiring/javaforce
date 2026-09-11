package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_registry object.
 *
 * @author pquiring
 */

public class WLRegistry extends WLObject {
  @SuppressWarnings("unchecked")
  public WLRegistry(WLClient client, int id) {
    super(client);
    this.id = id;
    Class cls = getClass();
    try {
      events = new Method[] {
        cls.getMethod("global", new Class[] {int.class, String.class, int.class}),
        cls.getMethod("global_remove", new Class[] {int.class}),
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getName() {
    return "wl_registry";
  }

  //requests

  public void bind(int name, int new_id) {
    invokeRequest(id, 0, name, new_id);
  }

  //events

  public void global(int name, String iface, int ver) {
    if (debug) JFLog.log("WLRegistry.global:name=" + name + ",iface=" + iface + ",ver=0x" + Integer.toHexString(ver));
    client.setGlobal(name, iface);
  }

  public void global_remove(int name) {
    if (debug) JFLog.log("WLRegistry.global_remove:name=" + name);
    client.removeGlobal(name);
  }
}
