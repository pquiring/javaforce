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
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("bind", new Class[] {int.class, int.class}),
      };
      events = new Method[] {
        cls.getMethod("global", new Class[] {int.class, String.class, int.class}),
        cls.getMethod("global_remove", new Class[] {int.class}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_registry";
  }

  //requests

  //NOTE : new_id is preceded by iface, ver (this is the only time this happens in all of wayland protocol since the new_id is generic)
  public WLObject bind(int name, String iface, int ver, int new_id) {
    if (debug) {
      client.log("WLRegistry.bind:name=" + name + ",iface=" + iface + ",ver=0x" + Integer.toHexString(ver) + ",new_id=" + new_id);
    }
    invokeRequest(id, 0, name, iface, ver, new_id);
    return client.createObject(client.getGlobal(name), new_id);
  }

  //events

  public void global(int name, String iface, int ver) {
    if (debug) client.log("WLRegistry.global:name=" + name + ",iface=" + iface + ",ver=0x" + Integer.toHexString(ver));
    client.setGlobal(name, iface, ver);
  }

  public void global_remove(int name) {
    if (debug) client.log("WLRegistry.global_remove:name=" + name);
    client.removeGlobal(name);
  }
}
