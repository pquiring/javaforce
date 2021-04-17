package jfnetboot;

/** Config service.
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class Config implements WebUIHandler {
  private WebUIServer web;

  public void init() {
    web = new WebUIServer();
    web.start(this, 80, false);
  }

  public Panel getRootPanel(WebUIClient client) {
    return new ConfigPanel();
  }

  public byte[] getResource(String url) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}
