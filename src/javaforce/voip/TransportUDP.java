package javaforce.voip;

/** SIP UDP Transport
 *
 * @author pquiring
 *
 * Created Jan 30, 2014
 */

import java.net.*;

import javaforce.*;

public class TransportUDP implements Transport {
  private DatagramSocket ds;
  private boolean active = false;
  private boolean error = false;

  public String getName() { return "UDP"; }

  public boolean open(String localhost, int localport) {
    try {
      ds = new DatagramSocket(localport);
      active = true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean close() {
    active = false;
    if (ds != null) {
      ds.close();
      ds = null;
    }
    return true;
  }

  public boolean send(byte[] data, int off, int len, InetAddress host, int port) {
    try {
      DatagramPacket dp = new DatagramPacket(data, off, len, host, port);
      ds.send(dp);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean receive(Packet packet) {
    try {
      DatagramPacket dp = new DatagramPacket(packet.data, packet.data.length);
      ds.receive(dp);
      packet.length = dp.getLength();
      packet.host = dp.getAddress().getHostAddress();
      packet.port = dp.getPort();
    } catch (Exception e) {
      error = true;
      if (active) JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean error() {
    return error;
  }

}
