/** Network Port
 *
 * @author peterq.admin
 */

import java.io.Serializable;

import java.util.*;

import javaforce.*;

public class Port implements Serializable {
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

  public transient boolean link;

  public void setVLANs(String[] _vlans) {
    vlans.clear();
    for(String vlan : _vlans) {
      vlans.add(vlan);
    }
  }

  public String toString() {
    int idx = JF.indexOfDigit(id);
    if (idx == -1) {
      return id;
    } else {
      return id.substring(idx);
    }
  }
}
