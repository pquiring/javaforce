package javaforce.webui;

/** WebUI Handler
 *
 * @author pquiring
 */

public interface WebUIHandler {
  /** Returns a root panel for a new client. */
  Panel getRootPanel(Client client);
  /** Returns raw resources from /static/... */
  byte[] getResource(String url);
}
