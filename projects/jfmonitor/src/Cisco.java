/** Cisco API
 *
 * If a command result is very long use "terminal length 0" to disable more.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class Cisco {
  public static boolean debug = false;
  public static boolean debug_cfg = false;
  private static String range(Port[] ports) {
    StringBuilder sb = new StringBuilder();
    //see VLAN.mergeVLANs() for similar logic
    sb.append("range ");
    Arrays.sort(ports);
    Port first = null;
    Port last = null;
    for(Port port : ports) {
      if (first == null) {
        sb.append(port.id);
        first = port;
      } else {
        int lv = last.getPortInt();
        int pv = port.getPortInt();
        if (!port.equalsType(first) || pv != lv + 1) {
          if (last != first) {
            //range first-last
            sb.append(" - ");
            sb.append(last.getPort());
          }
          sb.append(" , ");
          sb.append(port.id);
          first = port;
        }
      }
      last = port;
    }
    if (last != first) {
      sb.append(" - ");
      sb.append(last.getPort());
    }
    return sb.toString();
  }
  public boolean addVLANs(Device device, Port[] ports, String vlans) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + range(ports) + ";switchport trunk allowed vlan add " + vlans + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("addVLANs:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removeVLANs(Device device, Port[] ports, String vlans) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + range(ports) + ";switchport trunk allowed vlan remove " + vlans + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("removeLANs:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setVLANs(Device device, Port port, String vlans) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + port.id + ";switchport trunk allowed vlan " + vlans + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setVLANs:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setVLAN(Device device, Port port, String vlan, int mode) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = null;
    switch (mode) {
      case MODE_TRUNK:
        cmds = "config terminal;interface " + port.id + ";switchport trunk native vlan " + vlan + ";exit;exit;exit";
        break;
      case MODE_ACCESS:
        cmds = "config terminal;interface " + port.id + ";switchport access vlan " + vlan + ";exit;exit;exit";
        break;
      default:
        return false;
    }
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setVLAN:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean createVLAN(Device device, String vid, String name) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds;
    if (name.length() > 0) {
      cmds = "config terminal;vlan " + vid + ";name " + name + ";exit;exit;exit";
    } else {
      cmds = "config terminal;vlan " + vid + ";exit;exit;exit";
    }
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("createVLAN:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removeVLAN(Device device, String vid) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;no vlan " + vid + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("removeVLAN:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setVLAN_STP(Device device, VLAN vlan, boolean state) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = null;
    if (state) {
      cmds = "config terminal;spanning-tree vlan " + vlan.getNumber() + ";exit;exit";
    } else {
      cmds = "config terminal;no spanning-tree vlan " + vlan.getNumber() + ";exit;exit";
    }
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setVLAN_STP:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean addInterfaceIP(Device device, String iid, String iface_ip, String iface_mask) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + iid + ";ip addr " + iface_ip + " " + iface_mask + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("addInterfaceIP:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removeInterfaceIP(Device device, String iid) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + iid + ";no ip address;exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("removeInterfaceIP:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean addInterfaceDHCPRelay(Device device, String iid, String dhcp_relay) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + iid + ";ip helper-address " + dhcp_relay + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setInterfaceDHCPRelay:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removeInterfaceDHCPRelay(Device device, String iid) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + iid + ";no ip helper-address;exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setInterfaceDHCPRelay:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setInterfaceShutdown(Device device, String iid, boolean state) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = null;
    if (state) {
      cmds = "config terminal;interface " + iid + ";shutdown;exit;exit;exit";
    } else {
      cmds = "config terminal;interface " + iid + ";no shutdown;exit;exit;exit";
    }
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setInterfaceShutdown:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public static final int MODE_ACCESS = 0;
  public static final int MODE_TRUNK = 1;
  public static final int MODE_IP = 2;
  public static int getSwitchMode(String name) {
    switch (name) {
      case "access": return MODE_ACCESS;
      case "trunk": return MODE_TRUNK;
      case "ip": return MODE_IP;
    }
    return -1;
  }
  public static String getSwitchMode(int mode) {
    switch (mode) {
      case MODE_ACCESS: return "access";
      case MODE_TRUNK: return "trunk";
      case MODE_IP: return "ip";
    }
    return null;
  }
  public boolean setSwitchMode(Device device, Port port, int mode) {
    String cmds = null;
    switch (mode) {
      case MODE_ACCESS:
        cmds = "config terminal;interface " + port.id + ";switchport mode access;exit;exit;exit";
        break;
      case MODE_TRUNK:
        cmds = "config terminal;interface " + port.id + ";switchport mode trunk;exit;exit;exit";
        break;
      case MODE_IP:
        cmds = "config terminal;interface " + port.id + ";no switchport;exit;exit;exit";
        break;
    }
    if (cmds == null) return false;
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setSwitchMode:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  /** Create a static bonded/channel/etc group of ports. */
  public boolean createGroup(Device device, String gid) {
    //interface port-channel #
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface port-channel " + gid + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("createGroup:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean addPortToGroup(Device device, String gid, Port port) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + port.id + ";channel-group " + gid + " mode on;exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("createGroup:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removePortFromGroup(Device device, Port port) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + port.id + ";no channel-group;exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("createGroup:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removeGroup(Device device, String gid) {
    //no interface port-channel #
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;no interface port-channel " + gid + ";exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("removeGroup:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setRoutingMode(Device device, boolean state) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = null;
    if (state) {
      cmds = "config terminal;ip routing;exit;exit";
    } else {
      cmds = "config terminal;no ip routing;exit;exit";
    }
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setRoutingMode:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setDefaultGateway(Device device, String gateway) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;ip default-gateway " + gateway + ";exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setDefaultGateway:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean addRoute(Device device, Route route) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;ip route " + route.toString() + ";exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("addRoute:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removeRoute(Device device, Route route) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;no ip route " + route.toString() + ";exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("removeRoute:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean saveConfig(Device device) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "copy running-config startup-config;;exit";  //enter = confirm filename
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("saveConfig:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setVLANName(Device device, VLAN vlan, String name) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;vlan " + vlan.getNumber() + ";name " + name + ";exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setName(VLAN):" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setPortName(Device device, Port port, String name) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + port.id + ";desc " + name + ";exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (debug) {
      JFLog.log("setPortName:" + cmds);
      return true;
    }
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  //TODO : merge all queries into one connection
  public boolean queryConfig(Device device) {
    //query device configuration
    if (debug) {
      JFLog.log("Cisco.queryConfig:" + device);
    }
    if (device.hardware == null) {
      return false;
    }
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "terminal length 0;show running-config;show version | include Serial;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String cfg = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (cfg == null || cfg.length() == 0) return false;
    if (debug_cfg) {
      JFLog.log("Cisco.config=" + cfg);
    }
    device.hardware.config = cfg;
    device.hardware.saveConfig(device);
    String[] lns = cfg.replaceAll("\\r", "").split("\n");
    VLAN vlan = null;
    Port port = null;
    device.resetValid();
    for(String ln : lns) {
      ln = ln.toLowerCase().trim();
      //decode config
      if (ln.startsWith("!")) {
        //end of section
        vlan = null;
        if (port != null) {
          if (port.vlans.isEmpty()) {
            port.setVLANs(new String[] {"ALL"});
          }
        }
        port = null;
        continue;
      }
      String[] f = ln.split(" ");
      switch(f[0]) {
        case "version":
          device.hardware.version = f[1];
          break;
        case "snmp-server":
          switch (f[1]) {
            case "chassis-id":
              device.hardware.serial = f[2].toUpperCase();
              break;
          }
          break;
        case "system":
          switch (f[1]) {
            case "serial":
              switch (f[2]) {
                case "number":
                  int idx = ln.indexOf(':');
                  if (idx != -1) {
                    device.hardware.serial = ln.substring(idx + 1).trim().toUpperCase();
                  }
                  break;
              }
              break;
          }
          break;
        case "interface":
          int idx = JF.indexOfDigit(f[1]);
          if (idx == -1) continue;
          String name = f[1].substring(0, idx);
          String id = f[1].substring(idx);
          if (id.equals("0/0")) continue;  //ignore admin port
          switch (name) {
            case "vlan": vlan = device.getVLAN(f[1]); break;
            case "port-channel": port = device.getGroup(f[1]); break;
            default: port = device.getPort(f[1], true); break;
          }
          break;
        case "switchport":
          if (f.length == 1) break;
          switch(f[1]) {
            case "trunk":
              switch(f[2]) {
                case "native":
                  if (f[3].equals("vlan")) {
                    if (port != null) {
                      port.vlan = f[4];
                    }
                  }
                  break;
                case "allowed":
                  if (f[3].equals("vlan")) {
                    if (port != null) {
                      port.setVLANs(f[4].split(","));
                    }
                  }
                  break;
              }
              break;
            case "access":
              switch (f[2]) {
                case "vlan":
                  if (port != null) {
                    port.access_vlan = f[3];
                  }
                  break;
              }
              break;
            case "mode":
              if (port != null) {
                port.mode = f[2];  //trunk or access
              }
              break;
          }
          break;
        case "channel-group":
          if (port != null) {
            port.group = f[1];
          }
          break;
        case "ip":
          switch (f[1]) {
            case "address":
              if (port != null) {
                port.ip = f[2];
                port.mask = f[3];
              }
              if (vlan != null) {
                vlan.ip = f[2];
                vlan.mask = f[3];
              }
              break;
            case "helper-address":
              if (port != null) {
                port.dhcp_relay = f[2];
              }
              if (vlan != null) {
                vlan.dhcp_relay = f[2];
              }
              break;
            case "default-gateway":
              device.hardware.gateway = f[2];
              break;
            case "routing":
              device.hardware.routing = true;
              break;
            case "route":
              if (f[2].equals("static")) break;
              if (f.length != 5) break;
              Route route = new Route();
              route.ip = f[2];
              route.mask = f[3];
              route.gateway = f[4];
              device.hardware.routes.add(route);
              break;
          }
          break;
        case "no":
          switch(f[1]) {
            case "ip":
              if (port != null) {
                port.ip = null;
                port.mask = null;
              }
              if (vlan != null) {
                vlan.ip = null;
                vlan.mask = null;
              }
              break;
            case "switchport":
              if (port != null) {
                port.mode = "ip";
              }
              break;
            case "spanning-tree":
              switch (f[2]) {
                case "vlan":
                  String[] vlans = VLAN.splitVLANs(f[3], true);
                  for(String vid : vlans) {
                    VLAN _vlan = device.getVLAN("vlan" + vid);
                    _vlan.stp = false;
                  }
                  break;
              }
              break;
            case "shutdown":
              break;
          }
          break;
        case "shutdown":
          if (port != null) {
            port.shutdown = true;
          }
          if (vlan != null) {
            vlan.shutdown = true;
          }
          break;
        case "spanning-tree":
          break;
        case "description":
          String desc = ln.substring(12);
          if (port != null) {
            port.name = desc;
          }
          break;
      }
    }
    device.removeInvalid(true, true, true);
    return true;
  }

  public boolean queryVLANs(Device device) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    options.command = "show vlan brief";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String status = ssh.getOutput();
    ssh.disconnect();
    if (status == null || status.length() == 0) return false;
    /*
VLAN Name ...
---- ---  ...
1    default
    */
    String[] lns = status.replaceAll("\\r", "").split("\n");
    for(String ln : lns) {
      int i1 = JF.indexOfDigit(ln);
      if (i1 != 0) continue;
      String id = ln.substring(0, 4).trim();
      VLAN vlan = device.getVLAN("vlan" + id);
      if (vlan == null) continue;
      int i2 = ln.indexOf(' ', 5);
      if (i2 == -1) continue;
      vlan.name = ln.substring(5, i2);
    }
    return true;
  }

  public boolean queryStatus(Device device) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    options.command = "show interface status";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String status = ssh.getOutput();
    ssh.disconnect();
    if (status == null || status.length() == 0) return false;
    /*
Port         Name               Status       Vlan       Duplex  Speed Type
Gi1/0/1                         connected    trunk      a-full a-1000 10/100/1000BaseTX
Gi1/0/2                         notconnect   1            auto   auto 10/100/1000BaseTX
...
    */
    device.resetValid();
    String[] lns = status.replaceAll("\\r", "").split("\n");
    for(String ln : lns) {
      int i1 = JF.indexOfDigit(ln);
      if (i1 == -1) continue;
      int i2 = ln.indexOf(" ");
      if (i2 == -1) continue;
      String number = ln.substring(i1, i2);
      Port port = device.getPortByNumber(number);
      if (port == null) continue;
      port.link = ln.indexOf("connected") != -1;
    }
    device.removeInvalid(true, false, false);
    return true;
  }

  //TODO : show if hardware has unsaved changes
  public boolean queryDiff() {
    //show archive config differences
    return false;
  }

  public boolean queryMACTable(Device device) {
    //query mac table
    if (debug) {
      JFLog.log("Cisco.queryMACTable:" + device);
    }
    if (device.hardware == null) {
      return false;
    }
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "terminal length 0;show mac address-table;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String mac_table = ssh.script(cmds.split(";"));
    ssh.disconnect();
    if (mac_table == null || mac_table.length() == 0) return false;
    String[] lns = mac_table.replaceAll("\\r", "").split("\n");
    int count = 0;
    for(String ln : lns) {
      ln = ln.toLowerCase().trim();
      String[] fs = ln.split("[ ]+");
      if (fs.length != 4) {
        continue;
      }
      String vlan = fs[0];
      String mac = fs[1].replaceAll("[.]", "");
      String type = fs[2];
      String ports = fs[3];
      String port_id = null;
      int idx = JF.indexOfDigit(ports);
      if (idx != -1) {
        port_id = ports.substring(idx);
      } else {
        port_id = ports;
      }
      Port port = device.getPortByNumber(port_id);
      if (port == null) {
        if (debug) {
          JFLog.log("port not found:" + port_id);
        }
        continue;
      }
      Device dev = Config.current.getDevice(mac);
      if (dev == null) {
        if (debug) {
          JFLog.log("device not found:" + mac);
        }
        continue;
      }
      device.hardware.addMACTableEntry(mac, port_id);
      count++;
    }
    if (debug) {
      JFLog.log("MAC Table:device=" + device.mac + ":count=" + count);
    }
    return true;
  }

  public static void main(String[] args) {
    //TODO : unit testing range()
    Port[] ports = new Port[16];
    int p = 1;
    for(int i=0;i<16;i++) {
      if (i < 8) {
        ports[i] = new Port("ethernet1/" + p++);
      } else {
        ports[i] = new Port("gigabit1/" + p++);
      }
      if (i % 3 == 0) p++;
      if (i % 9 == 0) p++;
      JFLog.log("port[] = " + ports[i].getID());
    }
    String range = range(ports);
    JFLog.log("range=" + range);
  }
}
