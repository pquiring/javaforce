package javaforce.voip;

/** SIP TLS Transport
 *
 * Client needs to only connect to one endpoint.
 *
 * @author pquiring
 *
 * Created : Jan 29, 2014
 */

import java.net.*;
import javax.net.ssl.*;

import javaforce.*;

public class TransportTLSServer extends TransportTCPServer {
  private static TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }
      public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
      public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    }
  };

  public String getName() { return "TLS"; }

  public boolean open(int localport) {
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      SSLServerSocketFactory sslsocketfactory = (SSLServerSocketFactory) sc.getServerSocketFactory();  //this method will work with untrusted certs
      ss = sslsocketfactory.createServerSocket();
      ss.bind(new InetSocketAddress(InetAddress.getLocalHost(), localport));
      new WorkerAccepter().start();
    } catch (Exception e) {
      JFLog.log(e);
    }
    return true;
  }
}
