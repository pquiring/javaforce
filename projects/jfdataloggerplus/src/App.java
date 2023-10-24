/** App
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class App implements WebUIHandler {
  public static WebUIServer server;

  public static void main(String args[]) {
  }

  public static void serviceStart(String args[]) {
    Service.start();
    server = new WebUIServer();
    server.start(new App(), 80, false);
  }

  public static void serviceStop() {
    Service.stop();
    server.stop();
  }

  public Panel getRootPanel(WebUIClient client) {
    return new MainPanel();
  }

  public byte[] getResource(String url) {
    return null;
  }

  public void clientConnected(WebUIClient client) {}
  public void clientDisconnected(WebUIClient client) {}

}
