package javaforce.net;

/** Subnet : IP4 + Mask
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

public class Subnet4 {
  private IP4 ip = new IP4(), mask = new IP4();
  public static boolean isSubnet(String str) {
    if (!IP4.isIP(str)) return false;
    //TODO : check bits
    return true;
  }
  public static String fromCIDR(int cidr) {
    int mask = 0x80000000;
    cidr--;
    while (cidr > 0) {
      mask >>= 1;
      cidr--;
    }
    return String.format("%d.%d.%d.%d",
      (mask & 0xff000000) >>> 24,
      (mask & 0xff0000) >> 16,
      (mask & 0xff00) >> 8,
      (mask & 0xff)
    );
  };
  public boolean setIP(String str) {
    return ip.setIP(str);
  }
  public boolean setIP(InetAddress addr) {
    return setIP(addr.getHostAddress());
  }
  public boolean setMask(String str) {
    if (!mask.setIP(str)) return false;
    maskIP();
    return true;
  }
  public boolean setMask(InetAddress addr) {
    return setMask(addr.getHostAddress());
  }
  /** Checks if IP address is within defined Subnet.
   @param in = IP address to check
   */
  public boolean matches(IP4 in) {
    short o;
    for(int a=0;a<4;a++) {
      o = (short)(in.ip[a] & mask.ip[a]);
      if (o != ip.ip[a]) return false;
    }
    return true;
  }
  private void maskIP() {
    for(int a=0;a<4;a++) {
      ip.ip[a] &= mask.ip[a];
    }
  }
  public String toString() {
    return ip.toString() + "/" + mask.toString();
  }

  public static void test(Subnet4 net, String ip) {
    IP4 ip4 = new IP4();
    ip4.setIP(ip);
    JFLog.log(net.matches(ip4) + ":" + ip);
  }

  public static void main(String[] args) {
    Subnet4 net = new Subnet4();
    net.setIP("192.168.1.0");
    net.setMask("255.255.255.0");
    test(net, "192.168.1.5");
    test(net, "192.168.5.5");
    JFLog.log("CIDR/26=" + fromCIDR(26));
    JFLog.log("CIDR/24=" + fromCIDR(24));
    JFLog.log("CIDR/16=" + fromCIDR(16));
  }
}
