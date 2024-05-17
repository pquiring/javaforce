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
    String keyfile = Service.dataPath + "/jfdataloggerplus.key";
    String password = "password";
    KeyParams params = new KeyParams();
    params.dname = "CN=jfdataloggerplus.sourceforge.net, O=server, OU=webserver, C=CA";;
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

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    return new MainPanel();
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
    return null;
  }

  public void clientConnected(WebUIClient client) {}
  public void clientDisconnected(WebUIClient client) {}

}
