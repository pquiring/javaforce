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
}
