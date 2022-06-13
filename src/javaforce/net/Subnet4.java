package javaforce.net;

/** Subnet : IP4 + Mask
 *
 * @author pquiring
 */

public class Subnet4 {
  private IP4 ip = new IP4(), mask = new IP4();
  public boolean setIP(String str) {
    return ip.setIP(str);
  }
  public boolean setMask(String str) {
    if (!mask.setIP(str)) return false;
    maskIP();
    return true;
  }
  public boolean matches(IP4 in) {
    IP4 copy = new IP4();
    for(int a=0;a<4;a++) {
      copy.ip[a] = in.ip[a];
    }
    for(int a=0;a<4;a++) {
      copy.ip[a] &= mask.ip[a];
    }
    for(int a=0;a<4;a++) {
      if (copy.ip[a] != ip.ip[a]) return false;
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
}
