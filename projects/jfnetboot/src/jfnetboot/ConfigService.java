package jfnetboot;

/** Config service.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.webui.*;
import javaforce.*;

public class ConfigService implements WebUIHandler {
  private WebUIServer web;
  private KeyMgmt keys;

  public void start() {
    initSecureWebKeys();
    web = new WebUIServer();
    web.start(this, 443, keys);
  }

  public void stop() {
    if (web == null) return;
    web.stop();
    web = null;
  }

  private void initSecureWebKeys() {
    String dname = "CN=jfnetboot.sourceforge.net, O=server, OU=webserver, C=CA";
    String keyfile = Paths.config + "/jfnetboot.key";
    String password = "password";
    if (new File(keyfile).exists()) {
      //load existing keys
      keys = new KeyMgmt();
      try {
        FileInputStream fis = new FileInputStream(keyfile);
        keys.open(fis, password.toCharArray());
        fis.close();
      } catch (Exception e) {
        if (!keys.isValid()) {
          //generate random keys
          keys = KeyMgmt.create(keyfile, "webserver", dname, password);
        }
        JFLog.log(e);
      }
    } else {
      //generate random keys
      keys = KeyMgmt.create(keyfile, "webserver", dname, password);
    }
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
