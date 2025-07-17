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
import java.util.*;

import javaforce.*;

public class IP4 implements Comparable<IP4> {
  /** IP4 Address. */
  public byte[] ip = new byte[4];
  /** Local device (optional) */
  public String device;
  public IP4() {
  }
  public IP4(String str) {
    setIP(str);
  }
  public IP4(String str, String device) {
    setIP(str);
    this.device = device;
  }
  public IP4(IP4 ip4) {
    setIP(ip4);
  }
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
        ip[a] = UByte.valueOf(ips[a]);
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  public boolean setIP(InetAddress addr) {
    return setIP(addr.getHostAddress());
  }
  public boolean setIP(IP4 o) {
    for(int a=0;a<4;a++) {
      ip[a] = o.ip[a];
    }
    return true;
  }
  public void mask(IP4 o) {
    for(int a=0;a<4;a++) {
      ip[a] &= o.ip[a];
    }
  }
  public void or(IP4 o) {
    for(int a=0;a<4;a++) {
      ip[a] |= o.ip[a];
    }
  }
  public boolean allZero() {
    for(int a=0;a<4;a++) {
      if (ip[a] != 0) return false;
    }
    return true;
  }
  public boolean allOne() {
    for(int a=0;a<4;a++) {
      if (ip[a] != -1) return false;
    }
    return true;
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
  public int toInt() {
    int value = ip[0] & 0xff;
    value <<= 8;
    value += ip[1] & 0xff;
    value <<= 8;
    value += ip[2] & 0xff;
    value <<= 8;
    value += ip[3] & 0xff;
    return value;
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
    ip6.ip[5] = (short)0xffff;
    ip6.ip[6] = (short)((ip[0] << 8) + (ip[1] & 0xff));
    ip6.ip[7] = (short)((ip[2] << 8) + (ip[3] & 0xff));
    return ip6;
  }

  public static IP4 getLoopbackIP() {
    IP4 ip4 = new IP4();
    ip4.ip[0] = 127;
    ip4.ip[3] = 1;
    return ip4;
  }

  /** Return list of local IP4 addresses. */
  public static IP4[] list() {
    ArrayList<IP4> list = new ArrayList<>();
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
        if (!networkInterface.isLoopback()) {
          Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
          String device = networkInterface.getDisplayName();
          for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            if (inetAddress.isSiteLocalAddress()) {
              String ip = inetAddress.getHostAddress();
              if (isIP(ip)) {
                list.add(new IP4(ip, device));
              }
            }
          }
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    return list.toArray(new IP4[list.size()]);
  }

  public static void test(String ip, boolean expect) {
    IP4 ip4 = new IP4();
    boolean res = ip4.setIP(ip);
    if (res != expect) {
      JFLog.log("failed:" + ip);
    }
    JFLog.log(IP4.isIP(ip) + ":" + ip + ":" + ip4.toString());
  }

  private static void tests() {
    test("1.1.1.1", true);
    test("127.0.0.1", true);
    test("255.255.255.0", true);
    test("1.1.1.-1", false);
    test("127.a.0.1", false);
    test("127..0.1", false);
    test("127.0.0.1.", false);
    JFLog.log("loopback=" + getLoopbackIP().toString());
  }

  private static void list_ips() {
    IP4[] list = list();
    for(IP4 ip : list) {
      System.out.println(ip.device + "=" + ip.toString());
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage:IP4 {tests | list}");
      return;
    }
    switch (args[0]) {
      case "tests": tests(); break;
      case "list": list_ips(); break;
    }
  }

  public int compareTo(IP4 o) {
    for(int a=0;a<4;a++) {
      if (ip[a] != o.ip[a]) return -1;
    }
    return 0;
  }
}
