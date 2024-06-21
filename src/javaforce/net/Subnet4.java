package javaforce.net;

/** Subnet : IP4 + Mask
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

public class Subnet4 {
  private IP4 ip = new IP4();
  private IP4 mask = new IP4();
  private IP4 inverse = new IP4();
  public static boolean isSubnet(String str) {
    if (!IP4.isIP(str)) return false;
    try {
      IP4 ip = new IP4();
      ip.setIP(str);
      char[] bin = Integer.toBinaryString(ip.toInt()).toCharArray();
      boolean zero = false;
      for(int i=0;i<bin.length;i++) {
        if (bin[i] == '0') {
          zero = true;
        } else {
          if (zero) return false;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
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
    if (!mask.setIP(str)) {
      JFLog.log("setMask:setIP failed");
      return false;
    }
    IP4 tmp = new IP4();
    tmp.setIP(ip);
    tmp.mask(mask);
    if (tmp.compareTo(ip) != 0) {
      JFLog.log("setMask:matches failed");
      return false;
    }
    setInverseMask();
    return true;
  }
  public boolean setMask(InetAddress addr) {
    return setMask(addr.getHostAddress());
  }
  private void setInverseMask() {
    for(int a=0;a<4;a++) {
      inverse.ip[a] = (byte)~mask.ip[a];
    }
  }
  /** Checks if IP address is within defined Subnet.
   @param in = IP address to check
   */
  public boolean isWithin(IP4 in) {
    IP4 tmp = new IP4();
    tmp.setIP(in);
    tmp.mask(mask);
    return tmp.compareTo(ip) == 0;
  }
  /** Checks if IP address is a valid device against defined subnet mask.
   * Does not check if address is 'within' defined subnet. Use isWithin() for that.
   */
  public boolean isDevice(IP4 in) {
    //device id all ones?
    IP4 device = new IP4();
    device.setIP(in);
    device.or(mask);
    if (device.allOne()) return false;
    //device id all zeros?
    device.setIP(in);
    device.mask(inverse);
    if (device.allZero()) return false;
    return true;
  }
  public String toString() {
    return ip.toString() + "/" + mask.toString();
  }

  public static void test_matches(Subnet4 net, String ip, boolean expect) {
    IP4 ip4 = new IP4();
    ip4.setIP(ip);
    boolean res = net.isWithin(ip4);
    JFLog.log("matches(" + ip + ")=" + res + ":" + ip);
    if (res != expect) JFLog.log("test failed!");
  }

  public static void test_subnet(String ip, boolean expect) {
    boolean res = isSubnet(ip);
    JFLog.log("isSubnet(" + ip + ")=" + res);
    if (res != expect) JFLog.log("test failed!");
  }

  public static void test_device(Subnet4 net, String ip, boolean expect) {
    boolean res = net.isDevice(new IP4(ip));
    JFLog.log("isDevice(" + ip + ")=" + res);
    if (res != expect) JFLog.log("test failed!");
  }

  public static void main(String[] args) {
    Subnet4 net = new Subnet4();
    if (!net.setIP("192.168.1.0")) JFLog.log("setIP failed");
    if (!net.setMask("255.255.255.0")) JFLog.log("setMask failed");
    test_matches(net, "192.168.1.5", true);
    test_matches(net, "192.168.5.5", false);
    test_subnet("255.255.255.0", true);
    test_subnet("255.255.128.0", true);
    test_subnet("255.255.0.8", false);
    test_device(net, "192.168.1.0", false);
    test_device(net, "192.168.1.1", true);
    test_device(net, "192.168.1.255", false);
    JFLog.log("CIDR/26=" + fromCIDR(26));
    JFLog.log("CIDR/24=" + fromCIDR(24));
    JFLog.log("CIDR/16=" + fromCIDR(16));
  }
}
