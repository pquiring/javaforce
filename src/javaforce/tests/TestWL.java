package javaforce.tests;

import javaforce.linux.wl.WLRegistry;
import javaforce.linux.wl.WLClient;
import javaforce.linux.wl.WLDisplay;

import javaforce.*;
import javaforce.linux.wl.*;

/** TestWL.
 *
 * @author pquiring
 */

public class TestWL implements WLNotify, WLWindowEvents {
  public static void main(String[] args) {
    new TestWL().run();
  }

  boolean active;
  WLClient client;
  WLDisplay display;
  WLRegistry registry;
  WLRForeignToplevelManager toplevel_manager;

  public void run() {
    active = true;
    client = new WLClient(this);
    if (!client.connect()) {
      return;
    }
    display = client.get_display();
    registry = display.get_registry(client.get_next_id());
    while (active) {
      JF.sleep(1000);
    }
    client.disconnect();
  }

  public void onRequest(String cls, String method, Object[] args) {
    JFLog.log("onRequest:" + cls + "," + method);
  }

  public void onEvent(String cls, String method, Object[] args) {
    JFLog.log("onEvent:" + cls + "," + method);
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
                toplevel_manager = (WLRForeignToplevelManager)registry.bind(name, iface, toplevel_manager.getVersion(), new_id);
                toplevel_manager.setWindowEvents(this);
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
    JFLog.log("TopLevel Window Change detected.");
  }
}
