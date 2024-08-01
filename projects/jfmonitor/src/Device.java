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
  public String loc;  //location : where this device lives : DEVICE_MAC:PORT

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

  public Port getPort(String id, boolean create) {
    for(Port port : hardware.ports) {
      if (port.id.equals(id)) {
        port.valid = true;
        return port;
      }
    }
    if (!create) return null;
    Port port = new Port();
    port.id = id;
    hardware.ports.add(port);
    port.valid = true;
    return port;
  }

  public Port getPortByNumber(String number) {
    for(Port port : hardware.ports) {
      if (port.getSlotsPort().equals(number)) {
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

  public String getLocation() {
    if (loc == null) return "";
    //convert MAC to IP
    String[] mac_port = loc.split("[:]");
    if (mac_port.length != 2) return "???";
    String mac = mac_port[0];
    String port = mac_port[1];
    String ip = Config.current.getip(mac);
    if (ip == null) ip = "???";
    return "Switch=" + ip + " Port=" + port;
  }

  public boolean configSetSwitchMode(Port port, int mode) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setSwitchMode(this, port, mode)) {
          port.mode = Cisco.getSwitchMode(mode);
          if (mode == Cisco.MODE_ACCESS) {
            port.vlans.clear();
          }
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configAddPort_IP(Port port, String ip, String mask) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.addInterfaceIP(this, port.id, ip, mask)) {
          port.ip = ip;
          port.mask = mask;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemovePort_IP(Port port) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeInterfaceIP(this, port.id)) {
          port.ip = "";
          port.mask = "";
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configAddPort_DHCP_Relay(Port port, String dhcp_relay) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.addInterfaceDHCPRelay(this, port.id, dhcp_relay)) {
          port.dhcp_relay = dhcp_relay;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemovePort_DHCP_Relay(Port port) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeInterfaceDHCPRelay(this, port.id)) {
          port.dhcp_relay = "";
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configPortShutdown(Port port, boolean state) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setInterfaceShutdown(this, port.id, state)) {
          port.shutdown = state;
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
          String[] vs = VLAN.splitVLANs(vlans, false);
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

  public boolean configAddVLANs(Port[] ports, String vlans) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.addVLANs(this, ports, vlans)) {
          for(Port port : ports) {
            port.addVLANs(VLAN.splitVLANs(vlans, true));
          }
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemoveVLANs(Port[] ports, String vlans) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeVLANs(this, ports, vlans)) {
          for(Port port : ports) {
            port.removeVLANs(VLAN.splitVLANs(vlans, true));
          }
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetTrunkVLAN(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setVLAN(this, port, vlan, Cisco.MODE_TRUNK)) {
          port.vlan = vlan;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetAccessVLAN(Port port, String vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setVLAN(this, port, vlan, Cisco.MODE_ACCESS)) {
          port.access_vlan = vlan;
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

  public boolean configRemoveVLAN_IP(VLAN vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeInterfaceIP(this, vlan.id)) {
          vlan.ip = "";
          vlan.mask = "";
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetVLAN_STP(VLAN vlan, boolean state) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setVLAN_STP(this, vlan, state)) {
          vlan.stp = state;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configAddVLAN_DHCP_Relay(VLAN vlan, String dhcp_relay) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.addInterfaceDHCPRelay(this, vlan.id, dhcp_relay)) {
          vlan.dhcp_relay = dhcp_relay;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemoveVLAN_DHCP_Relay(VLAN vlan) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeInterfaceDHCPRelay(this, vlan.id)) {
          vlan.dhcp_relay = "";
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configVLAN_Shutdown(VLAN vlan, boolean state) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setInterfaceShutdown(this, vlan.id, state)) {
          vlan.shutdown = state;
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
        hardware.groups.add(group);
        //setup group settings to match ports
        int mode = Cisco.getSwitchMode(first.mode);
        if (!cisco.setSwitchMode(this, group, mode)) return false;
        group.mode = Cisco.getSwitchMode(mode);
        if (mode == Cisco.MODE_TRUNK) {
          if (!cisco.setVLANs(this, group, first.getVLANs())) return false;
        }
        group.setVLANs(first.getVLANs());
        if (!cisco.setVLAN(this, group, first.getTrunkVLAN(), mode)) return false;
        group.setVLAN(first.getTrunkVLAN());
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
        //TODO : remove group from all ports???
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

  public boolean configSetRoutingMode(boolean state) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setRoutingMode(this, state)) {
          hardware.routing = state;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configSetDefaultGateway(String ip) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.setDefaultGateway(this, ip)) {
          hardware.gateway = ip;
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configAddRoute(Route route) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.addRoute(this, route)) {
          addRoute(route);
          return true;
        }
        break;
    }
    return false;
  }

  public boolean configRemoveRoute(Route route) {
    switch (type) {
      case TYPE_CISCO:
        Cisco cisco = new Cisco();
        if (cisco.removeRoute(this, route)) {
          removeRoute(route);
          return true;
        }
        break;
    }
    return false;
  }

  private void addRoute(Route route) {
    for(Route r : hardware.routes) {
      if (r.compareTo(route) == 0) {
        //update route
        r.gateway = route.gateway;
        return;
      }
    }
    //new route
    hardware.routes.add(route);
  }

  private void removeRoute(Route route) {
    for(Route r : hardware.routes) {
      if (r.compareTo(route) == 0) {
        hardware.routes.remove(r);
        return;
      }
    }
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
    String _this = Config.current.getip(this.mac);
    if (_this == null) {
      _this = mac;
    }
    String _to = Config.current.getip(to.mac);
    if (_to == null) {
      _to = to.mac;
    }
    if (_this.length() < _to.length()) return -1;
    if (_this.length() > _to.length()) return 1;
    return _this.compareTo(_to);
  }
}
