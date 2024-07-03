/** Network Port
 *
 * @author peter.quiring
 */

import java.io.Serializable;

import java.util.*;

import javaforce.*;

public class Port implements Serializable, Comparable<Port> {
  public static final long serialVersionUID = 1;

  public static final Port[] ArrayType = new Port[0];

  public Port() {}
  public Port(String id) {this.id = id;}

  public transient boolean valid;

  public String id;
  public String name = "";

  //interface
  public String ip = "";
  public String mask = "";

  //switchport
  public String mode = "";  //trunk or access or ip
  public String access_vlan = "1";  //access vlan
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

  public String getName() {
    if (name == null) name = "";
    return name;
  }

  public String getIP() {
    if (ip == null) ip = "";
    return ip;
  }

  public String getMask() {
    if (mask == null) mask = "";
    return mask;
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

  public String getAccessVLAN() {
    if (access_vlan == null) access_vlan = "1";
    return access_vlan;
  }

  public String getVLANs() {
    return VLAN.joinVLANs(vlans.toArray(JF.StringArrayType));
  }

  public void setVLANs(String _vlans) {
    setVLANs(VLAN.splitVLANs(_vlans, false));
  }

  public void setVLANs(String[] _vlans) {
    vlans.clear();
    for(String vlan : _vlans) {
      vlans.add(vlan);
    }
  }

  public void addVLANs(String[] add_vlans) {
    String[] this_vlans = VLAN.splitVLANs(getVLANs(), true);
    String new_vlans = VLAN.addVLANs(this_vlans, add_vlans);
    setVLANs(new_vlans);
  }

  public void removeVLANs(String[] remove_vlans) {
    String[] this_vlans = VLAN.splitVLANs(getVLANs(), true);
    String new_vlans = VLAN.removeVLANs(this_vlans, remove_vlans);
    setVLANs(new_vlans);
  }

  public String getTrunkVLAN() {
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
    sb.append(" access{");
    sb.append("vlan:" + getAccessVLAN());
    sb.append("}");
    sb.append(" trunk{");
    if (vlans.size() > 0) {
      sb.append("vlans:[");
      int cnt = 0;
      for(String vlan : vlans) {
        if (cnt > 0) sb.append(",");
        sb.append(vlan);
        cnt++;
      }
      sb.append("]");
    }
    sb.append(" vlan:" + getTrunkVLAN());
    sb.append("}");
    if (isGroup) {
      String gid = getGroupID();
      sb.append(" ports:[");
      int cnt = 0;
      for(Port port : ports) {
        if (port.getGroup().equals(gid)) {
          if (cnt > 0) sb.append(",");
          sb.append(port.getSlotsPort());
          cnt++;
        }
      }
      sb.append("]");
    }
    if (name != null) {
      sb.append(" name:" + name);
    }
    return sb.toString();
  }

  public String getID() {
    return id;
  }

  /** Returns type : gigabitethernet, tengigethernet, etc. */
  public String getType() {
    int idx = JF.indexOfDigit(id);
    if (idx == -1) {
      return id;
    } else {
      return id.substring(0, idx);
    }
  }

  public String getSlots() {
    int i1 = JF.indexOfDigit(id);
    int i2 = id.lastIndexOf('/');
    if (i1 == -1 || i2 == -1) {
      return id;
    } else {
      return id.substring(i1, i2);
    }
  }

  /** Returns slot/[subslot]/port. */
  public String getSlotsPort() {
    int idx = JF.indexOfDigit(id);
    if (idx == -1) {
      return id;
    } else {
      return id.substring(idx);
    }
  }

  /** Returns port. */
  public String getPort() {
    int idx = id.lastIndexOf('/');
    if (idx == -1) {
      return id;
    } else {
      return id.substring(idx + 1);
    }
  }

  public int getPortInt() {
    return Integer.valueOf(getPort());
  }

  public String toString() {
    return getSlotsPort();
  }

  /** Compares ports for same type of ID. */
  public boolean equalsType(Port o) {
    return getType().equals(o.getType());
  }

  /** Compares ports for inclusion into new group. */
  public boolean equalsPort(Port o) {
    if (!mode.equals(o.mode)) return false;
    if (!getVLANs().equals(o.getVLANs())) return false;
    if (!vlan.equals(o.vlan)) return false;
    return true;
  }

  /** Compares id to sort Ports. */
  public int compareTo(Port o) {
    int c1 = getType().compareTo(o.getType());
    if (c1 != 0) return c1;
    int c2 = getSlots().compareTo(o.getSlots());
    if (c2 != 0) return c2;
    int v1 = getPortInt();
    int v2 = o.getPortInt();
    if (v1 < v2) return -1;
    if (v1 > v2) return 1;
    return 0;
  }
}
