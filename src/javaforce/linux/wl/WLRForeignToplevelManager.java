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
  private Object lock = new Object();
  private WLWindowEvents win_events;

  @SuppressWarnings("unchecked")
  public WLRForeignToplevelManager(WLClient client, int id, WLWindowEvents win_events) {
    super(client, id);
    this.win_events = win_events;
    setVersion(3);
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

  public String get_wl_name() {
    return "zwlr_foreign_toplevel_manager_v1";
  }

  public WLRForeignToplevelHandle[] getWindows() {
    synchronized (lock) {
      return handles.values().toArray(new WLRForeignToplevelHandle[0]);
    }
  }

  public void removeWindow(WLRForeignToplevelHandle window) {
    synchronized (lock) {
      handles.remove(window.getHandle());
    }
    win_events.onWindowChange();
  }

  public void onWindowChange() {
    win_events.onWindowChange();
  }

  //requests

  public void stop() {
    invokeRequest(id, 0);
  }

  //events

  public void toplevel(int handle) {
    synchronized (lock) {
      handles.put(handle, new WLRForeignToplevelHandle(client, handle, this));
    }
    win_events.onWindowChange();
  }

  public void finished() {
    //stop() request has been processed
  }
}
