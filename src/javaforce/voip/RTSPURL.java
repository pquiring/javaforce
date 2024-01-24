package javaforce.voip;

/** Registers the RTSP protocol with java.net.URL.
 *
 * @author pquiring
 */

import java.io.IOException;
import java.net.*;

public class RTSPURL extends URLStreamHandler implements URLStreamHandlerFactory {
  private static boolean registered = false;
  public static void register() {
    if (registered) return;
    URL.setURLStreamHandlerFactory(new RTSPURL());
    registered = true;
  }

  public URLStreamHandler createURLStreamHandler(String protocol) {
    if (protocol.equals("rtsp")) {
      return new RTSPURL();
    }
    return null;
  }

  protected URLConnection openConnection(URL u) throws IOException {
    return null;
  }

  /** Removes user info from RTSP URL. */
  public static String cleanURL(String url) {
    //need to remove user:pass from url
    //rtsp://user:pass@host:port/path?opt1=val1&opt2=val2
    int idx = url.indexOf('@');
    if (idx == -1) return url;
    return "rtsp://" + url.substring(idx + 1);
  }

  public static String getUserInfo(String urlstr) {
    try {
      return new URI(urlstr).toURL().getUserInfo();
    } catch (Exception e) {
      return null;
    }
  }

  public static String getHost(String urlstr) {
    try {
      return new URI(urlstr).toURL().getHost();
    } catch (Exception e) {
      return null;
    }
  }

  public static int getPort(String urlstr) {
    try {
      return new URI(urlstr).toURL().getPort();
    } catch (Exception e) {
      return 554;
    }
  }
}
