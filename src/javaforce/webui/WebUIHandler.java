package javaforce.webui;

/** WebUI Handler
 *
 * @author pquiring
 */

public interface WebUIHandler {
  /** Returns a root panel for a new client. */
  Panel getRootPanel(WebUIClient client);
  /** Returns raw resources from /static/... */
  byte[] getResource(String url);
  /** Invoked when a client disconnects for cleanup */
  void clientDisconnected(WebUIClient client);
}
