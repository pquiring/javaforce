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

public class TestWL implements WLNotify {
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
    client = new WLClient();
    if (!client.connect()) {
      return;
    }
    display = new WLDisplay(client);
    registry = display.get_registry(this);
    while (active) {
      JF.sleep(1000);
    }
    client.disconnect();
  }

  public void onEvent(String cls, String method, Object[] args) {
    switch (cls) {
      case "WLRegistry": {
        switch (method) {
          case "global": {
            int name = (Integer)args[0];
            String iface = (String)args[1];
            int ver = (Integer)args[2];
            switch (iface) {
              case "zwlr_foreign_toplevel_manager_v1":
                int new_id = client.get_next_id();
                toplevel_manager = new WLRForeignToplevelManager(client, new_id);
                client.setObject(new_id, toplevel_manager);
                registry.bind(name, new_id);
                break;
            }
            break;
          }
        }
        break;
      }
      case "WLRForeignToplevelManager": {
        break;
      }
      case "WLRForeignToplevelHandle": {
        break;
      }
    }
  }
}
