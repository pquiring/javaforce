package javaforce.webui;

import javaforce.*;
import javaforce.service.*;

/** WebUI Handler
 *
 * @author pquiring
 */

public interface WebUIHandler {
  /** Returns a root panel for a new client. */
  Panel getPanel(String name, HTTP.Parameters params, WebUIClient client);
  /** Returns raw resources from /static/... */
  byte[] getResource(String url, HTTP.Parameters params, WebRequest request, WebResponse response);
  /** Invoked when a client connects for init */
  void clientConnected(WebUIClient client);
  /** Invoked when a client disconnects for cleanup */
  void clientDisconnected(WebUIClient client);
}
