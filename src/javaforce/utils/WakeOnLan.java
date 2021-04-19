package javaforce.utils;

/** Service to send out WakeOnLan packets.
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

public class WakeOnLan {
  private DatagramSocket socket;
  /** Initialize WakeOnLan.
   *
   * Allocates a UDP socket.
   */
  public boolean init() {
    try {
      socket = new DatagramSocket();
      socket.setBroadcast(true);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return true;
  }

  /** Wakes a system with provided mac address. */
  public void wake(byte[] mac) {
    if (mac == null || mac.length != 6) return;
    byte[] data = new byte[102];
    //FF FF FF FF FF FF
    for(int a=0;a<6;a++) {
      data[0] = (byte)0xff;
    }
    //MAC * 16
    for(int a=0;a<16;a++) {
      System.arraycopy(mac, 0, data, 6 + a*6, 6);
    }
    try {
      DatagramPacket packet = new DatagramPacket(data, 0, data.length);
      packet.setAddress(InetAddress.getByName("255.255.255.255"));
      packet.setPort(0);
      socket.send(packet);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Wakes a system with provided mac address in hex format (0123456789AB). */
  public void wake(String mac) {
    if (mac == null || mac.length() != 12) return;
    byte[] binmac = new byte[6];
    for(int a=0;a<6;a++) {
      int idx = a * 2;
      String p = mac.substring(idx, idx+2);
      binmac[a] = Short.valueOf(p, 16).byteValue();
    }
    wake(binmac);
  }
}
