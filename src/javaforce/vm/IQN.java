package javaforce.vm;

/** IQN
 *
 * @author pquiring
 */

import java.util.*;

public class IQN {
  public static String generate(String fqn) {
    //format : iqn.YYYY-MM.{REVERSE-DNS}:{FQN}:{int-id}:{byte-id}
    Random rand = new Random();
    Calendar now = Calendar.getInstance();
    int year = now.get(Calendar.YEAR);
    int month = now.get(Calendar.MONTH) + 1;
    StringBuilder iqn = new StringBuilder();
    iqn.append("iqn");
    iqn.append('.');
    iqn.append(String.format("%04d-%02d", year, month));
    iqn.append('.');
    //reverse dns
    iqn.append("net.sourceforge.jfkvm");
    //fqn
    String[] pts = fqn.split("[.]");
    for(String pt : pts) {
      iqn.append('.');
      iqn.append(pt);
    }
    iqn.append(':');
    int id32 = rand.nextInt(0x7fffffff);
    iqn.append(Integer.toString(id32));
    iqn.append(':');
    int id8 = rand.nextInt(0x7f);
    iqn.append(Integer.toString(id8));
    return iqn.toString();
  }
}
