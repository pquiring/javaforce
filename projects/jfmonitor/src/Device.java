/** Device.
 *
 * @author pquiring
 */

import java.io.Serializable;

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
      clone.hardware = clone.hardware.clone();
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
      VLAN[] vs = hardware.vlans.toArray(new VLAN[0]);
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
    port.name = "";
    hardware.ports.add(port);
    port.valid = true;
    return port;
  }

  public Port getPortByNumber(String number) {
    for(Port port : hardware.ports) {
      if (port.getPortNumber().equals(number)) {
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
    vlan.name = "";
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

  public boolean configSetVLANs(Port port, String vlans) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        return cisco.setVLANs(this, port, vlans);
    }
    return false;
  }

  public boolean configAddVLANs(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        return cisco.addVLANs(this, port, vlan);
    }
    return false;
  }

  public boolean configRemoveVLANs(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        return cisco.removeVLANs(this, port, vlan);
    }
    return false;
  }

  public boolean configSetVLAN(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        return cisco.setVLAN(this, port, vlan);
    }
    return false;
  }

  public void configSetPortName(Port port, String name) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.setPortName(this, port, name);
        break;
    }
  }

  public void configCreateVLAN(String id, String name) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.createVLAN(this, id, name);
        break;
    }
  }

  public void configRemoveVLAN(VLAN vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.removeVLAN(this, vlan.id);
        break;
    }
  }

  public void configCreateGroup(String gid, Port[] ports) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.createGroup(this, gid);
        for(Port port : ports) {
          cisco.addPortToGroup(this, gid, port);
        }
        break;
    }
  }

  public void configRemoveGroup(String gid) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.removeGroup(this, gid);
        break;
    }
  }

  public void configSetGroup(String gid, Port port) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (gid.length() == 0) {
          cisco.removePortFromGroup(this, port);
        } else {
          cisco.addPortToGroup(this, gid, port);
        }
        break;
    }
  }

  public String nextGroupID() {
    Port[] groups = hardware.groups.toArray(Port.ArrayType);
    if (groups.length == 0) return "1";
    int max = 1;
    for(Port group : groups) {
      int gid = Integer.valueOf(group.getGroupID());
      if (gid > max) {
        max = gid + 1;
      }
    }
    return Integer.toString(max);
  }

  public void saveConfig() {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.saveConfig(this);
        break;
    }
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
