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
import java.security.*;
import java.security.cert.*;

import javaforce.*;

public class TransportTLSServer extends TransportTCPServer {
  private static TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
      public void checkClientTrusted(X509Certificate[] certs, String authType) {}
      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    }
  };

  public String getName() { return "TLS"; }

  public boolean open(int localport) {
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
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
