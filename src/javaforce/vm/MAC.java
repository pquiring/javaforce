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
}
