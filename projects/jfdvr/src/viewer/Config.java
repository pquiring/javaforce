package viewer;

/** Viewer Config
 *
 * @author pquiring
 */

import java.net.*;

public class Config {
  public static URL url;
  private static int nextPort = 5000;
  public static synchronized int getLocalPort() {
    if (nextPort > 10000) nextPort = 5000;
    int port = nextPort;
    nextPort += 2;
    return port;
  }
  public static String changeURL(String path) {
    String host = Config.url.getHost();
    int port = Config.url.getPort();
    return "rtsp://" + host + (port > 0 ? (":" + port) : "") + path;
  }
  public static String getParameter(String[] params, String name) {
    for(int a=0;a<params.length;a++) {
      String param = params[a];
      int idx = param.indexOf(":");
      if (idx == -1) continue;
      String key = param.substring(0, idx);
      String value = param.substring(idx + 1);
      if (key.equals(name)) {
        return value;
      }
    }
    return "";
  }
}
