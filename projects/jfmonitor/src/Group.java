/** Port Group, ether channel, trunk, bonded, LACP, what ever you call it.
 *
 * @author pquiring
 */

import java.io.Serializable;
import java.util.*;

public class Group implements Serializable {
  public static final long serialVersionUID = 1;

  public transient boolean valid;

  public String id;

  //interface
  public String ip, mask;

  //switchport
  public String mode;  //trunk or access
  public ArrayList<String> vlans = new ArrayList<>();  //allowed vlans
  public String vlan;  //native vlan
  public String group;

  public void setVLANs(String[] _vlans) {
    vlans.clear();
    for(String vlan : _vlans) {
      vlans.add(vlan);
    }
  }
}
