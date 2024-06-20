/** Network Port
 *
 * @author peterq.admin
 */

import java.io.Serializable;

import java.util.*;

import javaforce.*;

public class Port implements Serializable, Comparable<Port> {
  public static final long serialVersionUID = 1;

  public static final Port[] ArrayType = new Port[0];

  public transient boolean valid;

  public String id;
  public String name = "";

  //interface
  public String ip = "";
  public String mask = "";

  //switchport
  public String mode = "";  //trunk or access or ip
  public ArrayList<String> vlans = new ArrayList<>();  //allowed vlans
  public String vlan = "1";  //native vlan
  public String group = "";
  public boolean isGroup;
  public boolean link;

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

  public int getMode() {
    return Cisco.getSwitchMode(mode);
  }

  public static boolean validGroup(String gid) {
    if (gid.length() == 0) return true;
    if (!gid.equals(JF.filter(gid, JF.filter_numeric))) return false;
    int value = Integer.valueOf(gid);
    if (value < 1 || value > 4094) return false;
    return true;
  }

  public String getName() {
    if (name == null) name = "";
    return name;
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
    if (mode != null) {
      sb.append(" mode:" + mode);
    }
    if (vlans.size() > 0) {
      sb.append(" vlans:[");
      int cnt = 0;
      for(String vlan : vlans) {
        if (cnt > 0) sb.append(",");
        sb.append(vlan);
        cnt++;
      }
      sb.append("]");
    }
    sb.append(" vlan:" + getVLAN());
    if (isGroup) {
      String gid = getGroupID();
      sb.append(" ports:[");
      int cnt = 0;
      for(Port port : ports) {
        if (port.getGroup().equals(gid)) {
          if (cnt > 0) sb.append(",");
          sb.append(port.getNumber());
          cnt++;
        }
      }
      sb.append("]");
    }
    return sb.toString();
  }

  public String getNumber() {
    int idx = JF.indexOfDigit(id);
    if (idx == -1) {
      return id;
    } else {
      return id.substring(idx);
    }
  }

  public String toString() {
    return getNumber();
  }

  public int compareTo(Port o) {
    if (!mode.equals(o.mode)) return -1;
    if (!getVLANs().equals(o.getVLANs())) return -1;
    if (!vlan.equals(o.vlan)) return -1;
    if (!group.equals(o.group)) return -1;
    return 0;
  }
}
