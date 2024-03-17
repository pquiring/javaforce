package javaforce.vm;

/** MAC
 *
 * @author pquiring
 */

import java.util.*;

public class MAC {
  public static String generate() {
    Random r = new Random();
    StringBuilder sb = new StringBuilder();
    sb.append("4a:46");  //JF
    for(int a=0;a<4;a++) {
      sb.append(':');
      sb.append(String.format("%02x", r.nextInt(0xff)));
    }
    return sb.toString();
  }
  public static boolean valid(String mac) {
    //xx:xx:xx:xx:xx:xx
    if (mac.length() != 17) return false;
    String[] f = mac.split("[:]");
    if (f.length != 6) return false;
    for(int a=0;a<6;a++) {
      String o = f[a];
      if (o.length() != 2) return false;
      try {
        Integer.valueOf(o, 16);
      } catch (Exception e) {
        return false;
      }
    }
    return true;
  }
}
