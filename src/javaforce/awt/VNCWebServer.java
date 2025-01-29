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

  public WebUIServer secure_server;
  public WebUIServer server;
  private KeyMgmt keys;

  public void start(int port, int secure_port) {
    initSecureWebKeys();
    server = new WebUIServer();
    server.start(this, port);
    secure_server = new WebUIServer();
    secure_server.start(this, secure_port, keys);
  }

  public void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
    if (secure_server != null) {
      secure_server.stop();
      secure_server = null;
    }
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
    boolean scale = false;
    String param_scale = params.get("scale");
    if (param_scale != null) {
      scale = param_scale.equals("true");
    }
    if (scale) {
      opts |= VNCWebConsole.OPT_SCALE;
    }
    String password = params.get("password");
    return VNCWebConsole.createPanel(port, password, opts, client);
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}
