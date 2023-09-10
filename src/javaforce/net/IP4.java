package javaforce.net;

/** IP4
 *
 * 32 bits = host addr
 *
 * See Subnet4 for network addr.
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

public class IP4 {
  public short[] ip = new short[4];
  public static boolean isIP(String str) {
    if (str.equals("0:0:0:0:0:0:0:1")) {
      //IP6 localhost
      str = "127.0.0.1";
    }
    String[] os = str.split("[.]", -1);
    if (os.length != 4) {
      return false;
    }
    try {
      for(String o : os) {
        if (o.length() > 3) return false;
        if (!o.equals(JF.filter(o, JF.filter_numeric))) return false;
        int val = Integer.parseInt(o);
        if (val < 0 || val > 255) return false;
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  public boolean setIP(String str) {
    if (str.equals("0:0:0:0:0:0:0:1")) {
      //IP6 localhost
      str = "127.0.0.1";
    }
    if (!isIP(str)) return false;
    String[] ips = str.split("[.]", -1);
    try {
      for(int a=0;a<4;a++) {
        ip[a] = Short.valueOf(ips[a]);
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  public boolean setIP(InetAddress addr) {
    return setIP(addr.getHostAddress());
  }
  public InetAddress toInetAddress() {
    try {
      return InetAddress.getByName(toIP4String());
    } catch (Exception e) {
      JFLog.log("Error:IP4.toInetAddress() failed:" + toIP4String());
      return null;
    }
  }
  public String toIP4String() {
    return String.format("%d.%d.%d.%d", ip[0] & 0xff, ip[1] & 0xff, ip[2] & 0xff, ip[3] & 0xff);
  }
  public String toString() {
    return toIP4String();
  }
  public boolean isEmpty() {
    for(int a=0;a<4;a++) {
      if (ip[a] != 0) return false;
    }
    return true;
  }
  public boolean isMulticastAddress() {
    return toInetAddress().isMulticastAddress();
  }

  public IP6 toIP6() {
    IP6 ip6 = new IP6();
    ip6.ip[6] = 0xffff;
    ip6.ip[7] = ip[0] << 8 + ip[1];
    ip6.ip[8] = ip[2] << 8 + ip[3];
    return ip6;
  }

  public IP4 getLoopbackIP() {
    IP4 ip4 = new IP4();
    ip4.ip[0] = 127;
    ip4.ip[3] = 1;
    return ip4;
  }

  public static void test(String ip) {
    IP4 ip4 = new IP4();
    ip4.setIP(ip);
    JFLog.log(IP4.isIP(ip) + ":" + ip + ":" + ip4.toString());
  }

  public static void main(String[] args) {
    test("1.1.1.1");
    test("127.0.0.1");
    test("255.255.255.0");
    test("1.1.1.-1");
    test("127.a.0.1");
    test("127..0.1");
    test("127.0.0.1.");
  }
}
