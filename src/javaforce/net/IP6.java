package javaforce.net;

/** IP6
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

public class IP6 {
  public short[] ip = new short[8];
  public static boolean isIP(String str) {
    String[] os = str.split("[:]");
    if (os.length != 8) {
      return false;
    }
    try {
      for(String o : os) {
        if (o.length() > 4) return false;
        if (!o.equals(JF.filter(o, JF.filter_hex))) return false;
      }
    } catch (Exception e) {}
    return true;
  }
  public boolean setIP(String str) {
    String[] os = str.split("[:]");
    if (os.length != 8) {
      JFLog.log("invalid ip:" + str);
      return false;
    }
    for(int a=0;a<8;a++) {
      if (os[a].length() > 4) return false;
      ip[a] = Short.valueOf(os[a], 16);
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
}
