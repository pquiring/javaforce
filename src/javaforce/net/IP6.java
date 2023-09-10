package javaforce.net;

/** IP6
 *
 * https://en.wikipedia.org/wiki/IPv6_address
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

public class IP6 {
  public int[] ip = new int[8];
  public static boolean isIP(String str) {
    int double_idx = str.indexOf("::");
    boolean compressed = double_idx != -1;
    String[] os = str.split("[:]", -1);
    if (compressed) {
      if (str.endsWith(":")) return false;
      if (str.indexOf("::", double_idx + 1) != -1) return false;  //too many ::
      if (os.length < 3 || os.length > 8) {
        return false;
      }
    } else {
      if (os.length != 8) {
        return false;
      }
    }
    try {
      for(String o : os) {
        if (o.length() > 4) return false;
        if (!o.equals(JF.filter(o, JF.filter_hex))) return false;
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  public boolean setIP(String str) {
    if (!isIP(str)) return false;
    String[] os = str.split("[:]", -1);
    try {
      int idx = 0;
      for(String o : os) {
        if (o.length() == 0) {
          //compressed field
          if (idx == 0) {
            //first field(s) are omitted (there will be two zero length fields)
            idx++;
          } else {
            //middle fields(s) are omitted
            idx = 8 - (os.length - (idx + 1));
          }
        } else {
          ip[idx++] = Integer.valueOf(o, 16);
        }
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
      return InetAddress.getByName(toIP6String());
    } catch (Exception e) {
      JFLog.log("Error:IP6.toInetAddress() failed:" + toIP6String());
      return null;
    }
  }
  public String toIP6String() {
    return String.format("%04x:%04x:%04x:%04x:%04x:%04x:%04x:%04x",
      ip[0] & 0xffff,
      ip[1] & 0xffff,
      ip[2] & 0xffff,
      ip[3] & 0xffff,
      ip[4] & 0xffff,
      ip[5] & 0xffff,
      ip[6] & 0xffff,
      ip[7] & 0xffff
    );
  }
  public String toString() {
    return toIP6String();
  }
  public boolean isEmpty() {
    for(int a=0;a<8;a++) {
      if (ip[a] != 0) return false;
    }
    return true;
  }
  public boolean isMulticastAddress() {
    return toInetAddress().isMulticastAddress();
  }

  public static void test(String ip) {
    IP6 ip6 = new IP6();
    ip6.setIP(ip);
    JFLog.log(IP6.isIP(ip) + "=" + ip + "=" + ip6.toString());
  }

  public static void main(String[] args) {
    test("1:2:3:4:5:6:7:8");
    test("1111:4444:7777:aaaa:bbbb:cccc:dddd:ffff");
    test("1:2:3::8");
    test("1:2::8");
    test("::8");
    test("::7:8");
    test("1.1.1.1");
    test("1111:4444:7777:aaaa:bbbb:cccc:dddd:ffff:");
    test("1111:4444:7777:aaaa:bbbb:cccc:dddd::ffff");
    test("1:2:3:::8");
  }
}
