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
  public String name;

  //interface
  public String ip, mask;

  //switchport
  public String mode;  //trunk or access
  public ArrayList<String> vlans = new ArrayList<>();  //allowed vlans
  public String vlan;  //native vlan
  public String group;
  public boolean isGroup;

  public transient boolean link;

  public String getGroupID() {
    if (!isGroup) return "-1";
    int idx = JF.indexOfDigit(id);
    if (idx == -1) return "-1";
    return id.substring(idx);
  }

  public String getGroup() {
    if (group == null) group = "";
    return group;
  }

  public static boolean validGroup(String gid) {
    if (gid.length() == 0) return true;
    return gid.equals(JF.filter(gid, JF.filter_numeric));
  }

  public String getVLANs() {
    return VLAN.joinVLANs(vlans.toArray(JF.StringArrayType));
  }

  public void setVLANs(String[] _vlans) {
    vlans.clear();
    for(String vlan : _vlans) {
      vlans.add(vlan);
    }
  }

  public String getVLAN() {
    if (vlan == null) vlan = "1";
    return vlan;
  }

  public void setVLAN(String _vlan) {
    vlan = _vlan;
  }

  public String info(Port[] ports) {
    StringBuilder sb = new StringBuilder();
    sb.append(id);
    if (vlans.size() > 0) {
      sb.append(" vlans:[");
      for(String vlan : vlans) {
        sb.append(vlan);
        sb.append(",");
      }
      sb.append("]");
    }
    if (vlan != null && vlan.length() > 0) {
      sb.append(" vlan:" + vlan);
    }
    if (isGroup) {
      String gid = getGroupID();
      sb.append(" ports:[");
      for(Port port : ports) {
        if (port.getGroup().equals(gid)) {
          sb.append(port.getPortNumber());
        }
      }
      sb.append("]");
    }
    return sb.toString();
  }

  public String getPortNumber() {
    int idx = JF.indexOfDigit(id);
    if (idx == -1) {
      return id;
    } else {
      return id.substring(idx);
    }
  }

  public String toString() {
    return getPortNumber();
  }
}
