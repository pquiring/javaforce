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

public class TransportTLSClient extends TransportTCPClient {
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

  public void connect(InetAddress host, int port) throws Exception {
    this.remotehost = host;
    this.remoteport = port;
    socket.connect(new InetSocketAddress(host, port), 5000);
    SSLContext sc = SSLContext.getInstance("TLSv1.3");
    sc.init(null, trustAllCerts, new SecureRandom());
    SSLSocketFactory sslsocketfactory = (SSLSocketFactory) sc.getSocketFactory();  //this method will work with untrusted certs
    socket = sslsocketfactory.createSocket(socket, remotehost.getHostAddress(), remoteport, true);
    socket.setSoLinger(true, 0);  //allow to reuse socket again without waiting
    socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), localport));
    os = socket.getOutputStream();
    is = socket.getInputStream();
    active = true;
  }
}
