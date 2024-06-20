/** Static Route
 *
 * @author peter.quiring
 */

import java.io.*;

public class Route implements Serializable, Comparable<Route> {
  public String ip = "";
  public String mask = "";
  public String gateway = "";

  public static final Route[] ArrayType = new Route[0];

  public String toString() {
    return ip + " " + mask + " " + gateway;
  }

  public int compareTo(Route o) {
    if (!ip.equals(o.ip)) return ip.compareTo(o.ip);
    if (!mask.equals(o.mask)) return mask.compareTo(o.mask);
    return 0;
  }
}
