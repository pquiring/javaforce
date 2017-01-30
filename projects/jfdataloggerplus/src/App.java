/** App
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class App implements WebUIHandler {
  public static WebUIServer server;
  public static void main(String args[]) {
    server = new WebUIServer();
    server.start(new App(), 34001, false);
  }

  public Panel getRootPanel(WebUIClient client) {
    return new MainPanel();
  }

  public byte[] getResource(String url) {
    return null;
  }
}
