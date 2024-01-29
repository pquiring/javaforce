package javaforce.voip;

/** SIP Transport interface
 *
 * NOTE:There is no "connect" function since this interface must work for "connection" and "connectionless" type
 * protocols.  It's up to the implementing class to connect in the send() function if needed.
 *
 * @author pquiring
 *
 * Created : Jan 30, 2014
 */

import java.net.*;

public interface Transport {
  public boolean open(String localhost, int localport);
  public boolean close();
  public boolean send(byte[] data, int off, int len, InetAddress host, int port);
  public boolean receive(Packet packet);  //blocking
  public String getName();
//  public int getLocalPort();
  public boolean error();
}
