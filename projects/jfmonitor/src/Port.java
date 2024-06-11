/** Network Port
 *
 * @author peterq.admin
 */

import java.io.Serializable;

import java.util.*;

public class Port implements Serializable {
  public static final long serialVersionUID = 1;

  public transient boolean valid;

  public String id;
  public String ip, mask;  //interface IP
  public boolean link;
  public String mode;  //trunk or access (ignore ports that are null)
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
