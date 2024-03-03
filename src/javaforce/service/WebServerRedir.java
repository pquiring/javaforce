package javaforce.service;

/** Simple web server that redirects users to a different port.
 *
 * Typical used to redirect port 80 to 443.
 *
 * @author pquiring
 */

public class WebServerRedir implements WebHandler {
  private WebServer server;
  private int redir;

  public boolean start(int port, int redir) {
    if (port == redir) return false;
    this.redir = redir;
    server = new WebServer();
    server.start(this, port);
    return true;
  }

  public void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  public void doGet(WebRequest req, WebResponse res) {
    String host = req.getHost();
    res.sendRedirect("https://" + host + ":" + redir);
  }

  public void doPost(WebRequest req, WebResponse res) {
    doGet(req, res);
  }
}
