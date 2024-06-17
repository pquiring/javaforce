/** VLAN
 *
 * @author pquiring
 */

import java.io.Serializable;

public class VLAN implements Serializable {
  public static final long serialVersionUID = 1;

  public transient boolean valid;

  public String id;
  public String name;

  //interface
  public String ip, mask;

  public static boolean validVLAN(String vlan) {

    return false;
  }

  public static boolean validVLANs(String vlans) {
    return false;
  }

  public static String[] splitVLANs(String vlans) {
    //TODO : support ranges
    String[] _vlans = vlans.split(",");
    return _vlans;
  }

  public static String joinVLANs(String[] vlans) {
    //TODO : support ranges
    StringBuilder sb = new StringBuilder();
    for(String vlan: vlans) {
      if (sb.length() > 0) sb.append(",");
      sb.append(vlan);
    }
    return sb.toString();
  }
}
