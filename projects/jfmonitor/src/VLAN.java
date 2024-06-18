/** VLAN
 *
 * @author pquiring
 */

import java.io.Serializable;

import javaforce.*;

public class VLAN implements Serializable, Comparable<VLAN> {
  public static final long serialVersionUID = 1;

  public static final VLAN[] ArrayType = new VLAN[0];

  public transient boolean valid;

  public VLAN() {}

  public VLAN(String id, String name) {
    JFLog.log("VLAN(" + id + "," + name + ")");
    this.id = id;
    this.name = name;
    ip = "";
    mask = "";
  }

  public String id;
  public String name;

  //interface
  public String ip, mask;

  public String getNumber() {
    int idx = JF.indexOfDigit(id);
    if (idx == -1) {
      return id;
    } else {
      return id.substring(idx);
    }
  }

  public static boolean validVLAN(String vlan) {
    String num = JF.filter(vlan, JF.filter_numeric);
    if (!vlan.equals(num)) return false;
    int val = Integer.valueOf(vlan);
    if (val > 4095) return false;
    return true;
  }

  public static boolean validVLANs(String vlans) {
    if (vlans.equals("ALL")) return true;
    String[] list = vlans.split(",");
    for(String vlan : list) {
      if (vlan.length() == 0) return false;
      int idx = vlan.indexOf('-');
      if (idx == -1) {
        //single
        if (!validVLAN(vlan)) return false;
      } else {
        //range
        String[] p = vlan.split("[-]");
        if (p.length != 2) return false;
        if (!validVLAN(p[0])) return false;
        if (!validVLAN(p[1])) return false;
        int v0 = Integer.valueOf(p[0]);
        int v1 = Integer.valueOf(p[1]);
        if (v0 >= v1) return false;
      }
    }
    //TODO : check if ranges overlap, single within ranges, duplicate single, etc.
    return true;
  }

  public static String[] splitVLANs(String vlans) {
    String[] _vlans = vlans.split(",");
    return _vlans;
  }

  public static String joinVLANs(String[] vlans) {
    StringBuilder sb = new StringBuilder();
    for(String vlan: vlans) {
      if (sb.length() > 0) sb.append(",");
      sb.append(vlan);
    }
    return sb.toString();
  }

  public int compareTo(VLAN o) {
    int tv = Integer.valueOf(getNumber());
    int ov = Integer.valueOf(o.getNumber());
    if (tv < ov) return -1;
    if (tv > ov) return 1;
    return 0;
  }
}
