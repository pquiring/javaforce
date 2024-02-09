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
  public static void createURL(String uri) {
    try {
      int idx = uri.indexOf('@');
      if (idx == -1) idx = 7;  //rtsp://
      url = new URI("rtsp://user:password@" + uri.substring(idx)).toURL();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static void changeURL(String path) {
    try {
      String host = Config.url.getHost();
      int port = Config.url.getPort();
      url = new URI("rtsp://user:password@" + host + (port > 0 ? (":" + port) : "") + path).toURL();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static URL newURL(String path) {
    try {
      String host = Config.url.getHost();
      int port = Config.url.getPort();
      return new URI("rtsp://user:password@" + host + (port > 0 ? (":" + port) : "") + path).toURL();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
