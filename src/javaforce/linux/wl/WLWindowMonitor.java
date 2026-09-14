package javaforce.linux.wl;

import javaforce.*;

/** WLWindowMonitor.
 *
 * @author pquiring
 */

public class WLWindowMonitor implements WLNotify, WLWindowEvents {
  private boolean active;

  private WLClient client;
  private WLDisplay display;
  private WLRegistry registry;
  private WLRForeignToplevelManager toplevel_manager;

  private WLWindowEvents events;

  public WLWindowMonitor(WLWindowEvents events) {
    this.events = events;
  }

  public void start() {
    client = new WLClient();
    if (!client.connect()) {
      return;
    }
    display = client.get_display();
    registry = display.get_registry(this);
  }

  public void stop() {
    client.disconnect();
    client = null;
  }

  public WLRForeignToplevelHandle[] getWindows() {
    return toplevel_manager.getWindows();
  }

  public void onEvent(String cls, String method, Object[] args) {
    switch (cls) {
      case "wl_registry": {
        switch (method) {
          case "global": {
            int name = (Integer)args[0];
            String iface = (String)args[1];
            int ver = (Integer)args[2];
            switch (iface) {
              case "zwlr_foreign_toplevel_manager_v1":
                int new_id = client.get_next_id();
                toplevel_manager = new WLRForeignToplevelManager(client, new_id, this);
                registry.bind(name, iface, toplevel_manager.getVersion(), new_id);
                break;
            }
            break;
          }
        }
        break;
      }
      case "zwlr_foreign_toplevel_manager_v1": {
        break;
      }
      case "zwlr_foreign_toplevel_handle_v1": {
        break;
      }
    }
  }

  public void onWindowChange() {
    events.onWindowChange();
  }
}
