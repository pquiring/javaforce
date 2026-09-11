package javaforce.tests;

import javaforce.linux.wl.WLRegistry;
import javaforce.linux.wl.WLClient;
import javaforce.linux.wl.WLDisplay;

import javaforce.*;

/** TestWL.
 *
 * @author pquiring
 */

public class TestWL {
  public static void main(String[] args) {
    WLClient client = new WLClient();
    if (!client.connect()) {
      return;
    }
    WLDisplay display = new WLDisplay(client);
    WLRegistry registry = display.get_registry();
    JF.sleep(1000);
    client.disconnect();
  }
}
