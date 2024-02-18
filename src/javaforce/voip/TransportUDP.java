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
  private TransportInterface iface;

  public String getName() { return "UDP"; }

  public boolean open(String localhost, int localport, TransportInterface iface) {
    //NOTE : iface not used in UDP
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

  public boolean disconnect(String host, int port) {
    return false;
  }

  public boolean error() {
    return error;
  }

  public String[] getClients() {
    //not tracked for UDP clients
    return new String[0];
  }
}
