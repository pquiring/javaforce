package jfnetboot;

/** Config service.
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class ConfigService implements WebUIHandler {
  private WebUIServer web;

  public void init() {
    web = new WebUIServer();
    web.start(this, Settings.current.web_port, false);
  }

  public Panel getRootPanel(WebUIClient client) {
    return new LoginPanel();
  }

  public byte[] getResource(String url) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}
