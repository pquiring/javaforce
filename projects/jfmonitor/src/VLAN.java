/** VLAN
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class VLAN implements Serializable, Comparable<VLAN> {
  public static final long serialVersionUID = 1;

  public static final VLAN[] ArrayType = new VLAN[0];

  public transient boolean valid;

  public VLAN() {}

  public VLAN(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String id;
  public String name;

  //interface
  public String ip = "";
  public String mask = "";
  public String dhcp_relay = "";

  //stp
  public boolean stp = true;

  public String getNumber() {
    int idx = JF.indexOfDigit(id);
    if (idx == -1) {
      return id;
    } else {
      return id.substring(idx);
    }
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

  public String getDHCPRelay() {
    if (dhcp_relay == null) dhcp_relay = "";
    return dhcp_relay;
  }

  public static boolean validVLAN(String vlan) {
    vlan = vlan.trim();
    if (vlan.length() == 0) return false;
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

  public static String[] splitVLANs(String vlans, boolean split_ranges) {
    String[] _vlans = vlans.split(",");
    if (split_ranges) {
      ArrayList<String> list = new ArrayList<>();
      for(String vlan : _vlans) {
        int idx = vlan.indexOf('-');
        if (idx == -1) {
          list.add(vlan);
        } else {
          int r1 = Integer.valueOf(vlan.substring(0, idx));
          int r2 = Integer.valueOf(vlan.substring(idx + 1));
          for(int v=r1;v<=r2;v++) {
            list.add(Integer.toString(v));
          }
        }
      }
      return list.toArray(JF.StringArrayType);
    } else {
      return _vlans;
    }
  }

  /** Joins VLANs with commas. */
  public static String joinVLANs(String[] vlans) {
    StringBuilder sb = new StringBuilder();
    for(String vlan: vlans) {
      if (sb.length() > 0) sb.append(",");
      sb.append(vlan);
    }
    return sb.toString();
  }

  /** Merge VLANs with ranges and commas. */
  public static String mergeVLANs(String[] vlans) {
    StringBuilder sb = new StringBuilder();
    //see Cisco.range() for similar logic
    String first = null;
    String last = null;
    for(String vlan : vlans) {
      if (first == null) {
        sb.append(vlan);
        first = vlan;
      } else {
        int _last = Integer.valueOf(last);
        int _next = Integer.valueOf(vlan);
        if (_next != _last+1) {
          if (last != first) {
            //range first-last
            sb.append("-");
            sb.append(last);
          }
          sb.append(",");
          sb.append(vlan);
          first = vlan;
        }
      }
      last = vlan;
    }
    if (last != first) {
      sb.append("-");
      sb.append(last);
    }
    return sb.toString();
  }

  /** Sorts String as Integers. */
  private static Comparator<String> cmp = new Comparator<>() {
    public int compare(String o1, String o2) {
      int v1 = Integer.valueOf(o1);
      int v2 = Integer.valueOf(o2);
      if (v1 < v2) return -1;
      if (v1 > v2) return 1;
      return 0;
    }
  };

  /** Add VLANs creating ranges as needed.
   * @param vlans = current vlans (ranges split up)
   * @param add = vlans to add (ranges split up)
   */
  public static String addVLANs(String[] vlans, String[] add) {
    Arrays.sort(vlans, cmp);
    Arrays.sort(add, cmp);
    int i1 = 0;
    int c1 = vlans.length;
    int i2 = 0;
    int c2 = add.length;
    ArrayList<String> list = new ArrayList<>();
    while (i1 < c1 && i2 < c2) {
      int v1 = Integer.valueOf(vlans[i1]);
      int v2 = Integer.valueOf(add[i2]);
      if (v1 == v2) {
        //duplicate
        list.add(vlans[i1]);
        i1++;
        i2++;
      } else if (v1 < v2) {
        list.add(vlans[i1]);
        i1++;
      } else if (v1 > v2) {
        list.add(add[i2]);
        i2++;
      }
    }
    while (i1 < c1) {
      list.add(vlans[i1++]);
    }
    while (i2 < c2) {
      list.add(add[i2++]);
    }
    return mergeVLANs(list.toArray(JF.StringArrayType));
  }

  /** Remove VLANs creating ranges as needed.
   * @param vlans = current vlans (ranges split up)
   * @param remove = vlans to remove (ranges split up)
   */
  public static String removeVLANs(String[] vlans, String[] remove) {
    Arrays.sort(vlans, cmp);
    Arrays.sort(remove, cmp);
    int i1 = 0;
    int c1 = vlans.length;
    int i2 = 0;
    int c2 = remove.length;
    ArrayList<String> list = new ArrayList<>();
    while (i1 < c1 && i2 < c2) {
      int v1 = Integer.valueOf(vlans[i1]);
      int v2 = Integer.valueOf(remove[i2]);
      if (v1 == v2) {
        //duplicate (remove)
        i1++;
        i2++;
      } else if (v1 < v2) {
        list.add(vlans[i1]);
        i1++;
      } else if (v1 > v2) {
        i2++;
      }
    }
    while (i1 < c1) {
      list.add(vlans[i1++]);
    }
    while (i2 < c2) {
      list.add(remove[i2++]);
    }
    return mergeVLANs(list.toArray(JF.StringArrayType));
  }

  public int compareTo(VLAN o) {
    int tv = Integer.valueOf(getNumber());
    int ov = Integer.valueOf(o.getNumber());
    if (tv < ov) return -1;
    if (tv > ov) return 1;
    return 0;
  }

  public static void main(String[] args) {
    //unit testing mergeVLANs()
    String[] vlans = new String[] {
      "1", "2", "3", "9", "10", "14", "100", "101", "1000", "1001", "2345"
    };
    String merge = mergeVLANs(vlans);
    JFLog.log("mergeVLANs=" + merge);
  }
}
