/** App
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.webui.*;

public class App implements WebUIHandler {
  public static WebUIServer server;
  private static KeyMgmt keys;
  public static WebServerRedir redirService;

  private static void initSecureWebKeys() {
    String dname = "CN=jfkvm.sourceforge.net, O=server, OU=webserver, C=CA";
    String keyfile = Service.dataPath + "/jfkvm.key";
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

  public static void serviceStart(String args[]) {
    Service.start();
    initSecureWebKeys();
    //start config service
    server = new WebUIServer();
    server.start(new App(), 443, keys);
    //start redir service
    redirService = new WebServerRedir();
    redirService.start(80, 443);
  }

  public static void serviceStop() {
    Service.stop();
    server.stop();
    if (redirService != null) {
      try {
        redirService.stop();
      } catch (Exception e) {
        JFLog.log(e);
      }
      redirService = null;
    }
  }

  public Panel getRootPanel(WebUIClient client) {
    return new MainPanel();
  }

  public byte[] getResource(String url) {
    return null;
  }

  public void clientConnected(WebUIClient client) {}
  public void clientDisconnected(WebUIClient client) {}

}
