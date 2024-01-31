package viewer;

/** Viewer Config
 *
 * @author pquiring
 */

import java.net.*;

public class Config {
  public static URL url;
  private static int nextPort = 6000;
  public static synchronized int getLocalPort() {
    if (nextPort > 7000) nextPort = 6000;
    int port = nextPort;
    nextPort += 2;
    return port;
  }
  public static String changeURL(String path) {
    String host = Config.url.getHost();
    int port = Config.url.getPort();
    return "rtsp://" + host + (port > 0 ? (":" + port) : "") + path;
  }
}
