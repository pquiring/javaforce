/** Device.
 *
 * @author pquiring
 */

import java.io.Serializable;

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

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

  public void removeInvalid() {
    Port[] ps = hardware.ports.toArray(new Port[0]);
    for(Port port : ps) {
      if (!port.valid) {
        hardware.ports.remove(port);
      }
    }
    Port[] gs = hardware.groups.toArray(new Port[0]);
    for(Port group : gs) {
      if (!group.valid) {
        hardware.groups.remove(group);
      }
    }
    VLAN[] vs = hardware.vlans.toArray(new VLAN[0]);
    for(VLAN vlan : vs) {
      if (!vlan.valid) {
        hardware.vlans.remove(vlan);
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

  public void setPortName(Port port, String name) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.setPortName(this, port, name);
        break;
    }
  }

  public void createVLAN(String id, String name) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.createVLAN(this, id, name);
        break;
    }
  }

  public void removeVLAN(VLAN vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.removeVLAN(this, vlan.id);
        break;
    }
  }

  public void saveConfig() {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        cisco.saveConfig(this);
        break;
    }
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
