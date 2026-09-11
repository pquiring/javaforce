package javaforce.linux.wl;

import java.lang.reflect.*;
import java.util.*;

import javaforce.*;

/** wlr_foreign_toplevel_manager_v1 object.
 *
 * @author pquiring
 */

public class WLRForeignToplevelManager extends WLObject {

  private HashMap<Integer, WLObject> handles = new HashMap<>();

  @SuppressWarnings("unchecked")
  public WLRForeignToplevelManager(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      events = new Method[] {
        cls.getMethod("toplevel", new Class[] {int.class}),
        cls.getMethod("finished", new Class[] {}),
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getName() {
    return "zwlr_foreign_toplevel_manager_v1";
  }

  //requests

  public void stop() {
    invokeRequest(id, 0);
  }

  //events

  public void toplevel(int handle) {
    handles.put(handle, new WLRForeignToplevelHandle(client, handle));
  }

  public void finished() {

  }
}
