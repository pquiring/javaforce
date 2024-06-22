package jfnetboot;

/** Config service : jfNetBoot
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.webui.*;
import javaforce.service.*;

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
    String keyfile = Paths.config + "/jfnetboot.key";
    String password = "password";
    KeyParams params = new KeyParams();
    params.dname = "CN=jfnetboot.sourceforge.net, O=server, OU=webserver, C=CA";;
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
    return new LoginPanel();
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}
