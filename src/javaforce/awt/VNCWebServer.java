package javaforce.awt;

/** VNC Web Server
 *
 * Default port : 5800
 *
 * @author pquiring
 */

import java.io.*;
import java.awt.Font;
import java.awt.Graphics;
import java.util.*;

import javaforce.*;
import javaforce.webui.*;
import javaforce.service.*;

public class VNCWebServer implements WebUIHandler {

  public WebUIServer server;
  private KeyMgmt keys;

  public void start(int port) {
    initSecureWebKeys();
    JFLog.log("VNCWebServer starting on port " + port + "...");
    server = new WebUIServer();
    server.start(this, port, keys);
  }

  public void stop() {
    if (server == null) return;
    server.stop();
    server = null;
  }

  private void initSecureWebKeys() {
    String keyfile = JF.getConfigPath() + "/jfvncweb.key";
    String password = "password";
    KeyParams params = new KeyParams();
    params.dname = "CN=jfkvm.sourceforge.net, O=server, OU=webserver, C=CA";
    if (new File(keyfile).exists()) {
      //load existing keys
      keys = new KeyMgmt();
      try {
        FileInputStream fis = new FileInputStream(keyfile);
        keys.open(fis, password);
        fis.close();
      } catch (Exception e) {
        if (!keys.isValid()) {
          //generate random keys
          keys = KeyMgmt.create(keyfile, password, "webserver", params, password);
        }
        JFLog.log(e);
      }
    } else {
      //generate random keys
      keys = KeyMgmt.create(keyfile, password, "webserver", params, password);
    }
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    int port = 5900;
    String param_port = params.get("port");
    if (param_port != null) {
      port = JF.atoi(param_port);
      if (port < 1 || port > 65535) {
        port = 5900;
      }
    }
    int opts = 0;
    boolean toolbar = true;
    String param_toolbar = params.get("toolbar");
    if (param_toolbar != null) {
      toolbar = param_toolbar.equals("true");
    }
    if (toolbar) {
      opts |= VNCWebConsole.OPT_TOOLBAR;
    }
    //TODO : create get password panel
    return VNCWebConsole.createPanel(port, params.get("password"), opts);
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}
