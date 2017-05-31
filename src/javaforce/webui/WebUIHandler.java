package javaforce.webui;

/** WebUI Handler
 *
 * @author pquiring
 */

public interface WebUIHandler {
  /** Returns a root panel for a new client.
   *
   *  NOTE : You should not make changes to the components until this function has returned.
   *  If you do then use WebUIClient.isReady() to ensure you don't send requests before the client is ready.
   */
  Panel getRootPanel(WebUIClient client);
  /** Returns raw resources from /static/... */
  byte[] getResource(String url);
  /** Invoked when a client disconnects for cleanup */
  void clientDisconnected(WebUIClient client);
}
