/** Device.
 *
 * @author pquiring
 */

import java.io.Serializable;
import javaforce.JFLog;

public class Device implements Serializable, Comparable<Device>, Cloneable {
  public static final long serialVersionUID = 1;

  public int type;
  public String mac;
  public String desc;

  //hardware config
  public Hardware hardware;

  public static final int TYPE_UNKNOWN = 0;
  public static final int TYPE_CISCO = 1;

  public Device() {}

  public Device clone() {
    try {
      Device clone = (Device)super.clone();  //shallow copy
      clone.hardware = hardware.clone();
      return clone;
    } catch (Exception e) {
      return null;
    }
  }

  public String getip() {
    return Config.current.getip(mac);
  }

  public void resetValid() {
    for(Port port : hardware.ports) {
      port.valid = false;
    }
    for(Port group : hardware.groups) {
      group.valid = false;
    }
    for(VLAN vlan : hardware.vlans) {
      vlan.valid = false;
    }
  }

  public void removeInvalid(boolean do_ports, boolean do_groups, boolean do_vlans) {
    if (do_ports) {
      Port[] ps = hardware.ports.toArray(Port.ArrayType);
      for(Port port : ps) {
        if (!port.valid) {
          hardware.ports.remove(port);
        }
      }
    }
    if (do_groups) {
      Port[] gs = hardware.groups.toArray(Port.ArrayType);
      for(Port group : gs) {
        if (!group.valid) {
          hardware.groups.remove(group);
        }
      }
    }
    if (do_vlans) {
      VLAN[] vs = hardware.vlans.toArray(VLAN.ArrayType);
      for(VLAN vlan : vs) {
        if (!vlan.valid) {
          hardware.vlans.remove(vlan);
        }
      }
    }
  }

  public Port getPort(String id) {
    for(Port port : hardware.ports) {
      if (port.id.equals(id)) {
        port.valid = true;
        return port;
      }
    }
    Port port = new Port();
    port.id = id;
    hardware.ports.add(port);
    port.valid = true;
    return port;
  }

  public Port getPortByNumber(String number) {
    for(Port port : hardware.ports) {
      if (port.getNumber().equals(number)) {
        port.valid = true;
        return port;
      }
    }
    return null;
  }

  public VLAN getVLAN(String id) {
    for(VLAN vlan : hardware.vlans) {
      if (vlan.id.equals(id)) {
        vlan.valid = true;
        return vlan;
      }
    }
    VLAN vlan = new VLAN();
    vlan.id = id;
    hardware.vlans.add(vlan);
    vlan.valid = true;
    return vlan;
  }

  public Port getGroup(String id) {
    for(Port group : hardware.groups) {
      if (group.id.equals(id)) {
        group.valid = true;
        return group;
      }
    }
    Port group = new Port();
    group.id = id;
    group.isGroup = true;
    hardware.groups.add(group);
    group.valid = true;
    return group;
  }

  public boolean configSetSwitchMode(Port port, int mode) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setSwitchMode(this, port, mode)) {
          port.mode = Cisco.getSwitchMode(mode);
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetVLANs(Port port, String vlans) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setVLANs(this, port, vlans)) {
          String[] vs = vlans.split(",");
          port.vlans.clear();
          for(String vlan : vs) {
            port.vlans.add(vlan);
          }
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configAddVLANs(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.addVLANs(this, port, vlan)) {
          port.vlans.add(vlan);
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemoveVLANs(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeVLANs(this, port, vlan)) {
          port.vlans.remove(vlan);
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetVLAN(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setVLAN(this, port, vlan, port.getMode())) {
          port.vlan = vlan;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetPortName(Port port, String name) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setPortName(this, port, name)) {
          port.name = name;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configCreateVLAN(String id, String name) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.createVLAN(this, id, name)) {
          hardware.vlans.add(new VLAN("vlan" + id, name));
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemoveVLAN(VLAN vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeVLAN(this, vlan.id)) {
          hardware.vlans.remove(vlan);
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configEditVLAN(VLAN vlan, String name) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setVLANName(this, vlan, name)) {
          vlan.name = name;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configAddVLAN_IP(VLAN vlan, String ip, String mask) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.addInterfaceIP(this, vlan.id, ip, mask)) {
          vlan.ip = ip;
          vlan.mask = mask;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemoveVLAN_IP(VLAN vlan, String ip, String mask) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeInterfaceIP(this, vlan.id)) {
          vlan.ip = ip;
          vlan.mask = mask;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configCreateGroup(String gid, Port[] ports) {
    switch (type) {
      case TYPE_CISCO:
        Port first = ports[0];
        Cisco cisco = new Cisco();
        if (!cisco.createGroup(this, gid)) return false;
        Port group = new Port();
        group.id = "port-channel" + gid;
        //setup group settings to match ports
        int mode = Cisco.getSwitchMode(first.mode);
        if (!cisco.setSwitchMode(this, group, mode)) return false;
        if (mode == Cisco.MODE_TRUNK) {
          if (!cisco.setVLANs(this, group, first.getVLANs())) return false;
        }
        if (!cisco.setVLAN(this, group, first.vlan, mode)) return false;
        //join ports to group
        for(Port port : ports) {
          if (!cisco.addPortToGroup(this, gid, port)) return false;
        }
        return true;
    }
    return false;
  }

  public boolean configRemoveGroup(String gid) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeGroup(this, gid)) {
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetGroup(String gid, Port port) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (gid.length() == 0) {
          if (!cisco.removePortFromGroup(this, port)) return false;
        } else {
          if (!cisco.addPortToGroup(this, gid, port)) return false;
        }
        return true;
    }
    return false;
  }

  public String nextGroupID() {
    Port[] groups = hardware.groups.toArray(Port.ArrayType);
    if (groups.length == 0) return "1";
    boolean[] used = new boolean[65];  //1-64
    for(Port group : groups) {
      int gid = Integer.valueOf(group.getGroupID());
      if (gid > 0 && gid < 65) {
        used[gid] = true;
      }
    }
    for(int gid=1;gid<65;gid++) {
      if (!used[gid]) return Integer.toString(gid);
    }
    return "-1";
  }

  public boolean saveConfig() {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        return cisco.saveConfig(this);
    }
    return false;
  }

  public boolean groupExists(String gid) {
    for(Port group : hardware.groups.toArray(Port.ArrayType)) {
      if (group.getGroupID().equals(gid)) return true;
    }
    return false;
  }

  public String toString() {
    return "Device:" + getip();
  }

  public int compareTo(Device to) {
    String this_ip = Config.current.getip(this.mac);
    if (this_ip == null) return 0;
    String to_ip = Config.current.getip(to.mac);
    if (to_ip == null) return 0;
    return this_ip.compareTo(to_ip);
  }
}
