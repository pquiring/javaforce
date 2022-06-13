package javaforce.net;

/** IP4 + Port
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

public class IP4Port extends IP4 {
  public int port;
  public boolean setPort(String str) {
    port = JF.atoi(str);
    if (port < 1 || port > 65535) {
      JFLog.log("invalid port:" + port);
      port = -1;
      return false;
    }
    return true;
  }
  public boolean setIP_Port(InetSocketAddress addr) {
    if (!setIP(addr.getAddress().getHostAddress())) return false;
    port = addr.getPort();
    return true;
  }
  public InetSocketAddress toInetSocketAddress() {
    return new InetSocketAddress(toInetAddress(), port);
  }
  public String toString() {
    return super.toString() + ":" + port;
  }
}
