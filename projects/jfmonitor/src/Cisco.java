/** Cisco API
 *
 * @author pquiring
 */

import javaforce.*;

public class Cisco {
  public boolean addVLAN(Device device, Port port, String vlan) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    String cmds = "config terminal;interface " + port.id + ";switchport trunk allowed vlan add " + vlan + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean removeVLAN(Device device, Port port, String vlan) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;interface " + port.id + ";switchport trunk allowed vlan remove " + vlan + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean setVLAN(Device device, Port port, String vlan) {
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;interface " + port.id + ";switchport trunk native vlan " + vlan + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
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
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;vlan " + vid + ";name " + name + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
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
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;no vlan " + vid + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
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
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;interface " + iid + ";ip addr " + iface_ip + " " + iface_mask + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
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
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;interface " + iid + ";no ip;exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public static final int MODE_ACCESS = 0;
  public static final int MODE_TRUNK = 1;
  public static final int MODE_NO_SWITCHPORT = 2;
  public boolean setSwitchMode(Device device, Port port, int mode) {
    String cmds = null;
    switch (mode) {
      case MODE_ACCESS:
        cmds = "config terminal;interface " + port.id + ";switchport mode access;exit;exit;exit";
        break;
      case MODE_TRUNK:
        cmds = "config terminal;interface " + port.id + ";switchport mode trunk;exit;exit;exit";
        break;
      case MODE_NO_SWITCHPORT:
        cmds = "config terminal;interface " + port.id + ";no switchport;exit;exit;exit";
        break;
    }
    if (cmds == null) return false;
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
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
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;interface port-channel " + gid + ";exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
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
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;no interface port-channel " + gid + ";exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean groupAddPort(Device device, Port port, String gid) {
    //interface # ; channel-group # mode on
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;interface " + port.id + ";channel-group " + gid + " mode on;exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean groupRemovePort(Device device, Port port) {
    //interface # ; no channel-group #
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    String cmds = "config terminal;interface " + port.id + ";no channel-group;exit;exit;exit";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String result = ssh.script(cmds.split(";"));
    if (result == null) return false;
    boolean ok = result.indexOf('%') == -1;
    if (!ok) {
      JFLog.log("Error:" + result);
    }
    return ok;
  }
  public boolean queryConfig(Device device) {
    //query device configuration
    if (device.hardware == null) {
      device.hardware = new Hardware();
    }
    SSH ssh = new SSH();
    SSH.Options options = new SSH.Options();
    options.username = device.hardware.user;
    options.password = device.hardware.pass;
    options.type = SSH.TYPE_EXEC;
    options.command = "show configuration";
    String ip = device.getip();
    if (ip == null) return false;
    if (!ssh.connect(ip, 22, options)) return false;
    String cfg = ssh.getOutput();
    if (cfg == null || cfg.length() == 0) return false;
    device.hardware.config = cfg;
    String[] lns = cfg.replaceAll("\\r", "").split("\n");
    VLAN vlan = null;
    Port group = null;
    Port port = null;
    device.resetValid();
    for(String ln : lns) {
      //decode config
      if (ln.startsWith("!")) {
        //end of section
        vlan = null;
        group = null;
        port = null;
        continue;
      }
      ln = ln.toLowerCase();
      String[] f = ln.split(" ");
      switch(f[0]) {
        case "version":
          device.hardware.version = f[1];
          break;
        case "interface":
          int idx = JF.indexOfDigit(f[1]);
          if (idx == -1) continue;
          String name = f[1].substring(0, idx);
          switch (name) {
            case "vlan": vlan = device.getVLAN(f[1]); break;
            case "port-channel": group = device.getGroup(f[1]); break;
            default: port = device.getPort(f[2]); break;
          }
          break;
        case "switchport":
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
                    if (group != null) {
                      group.setVLANs(f[4].split(","));
                    }
                  }
                  break;
              }
              break;
            case "mode":
              if (port != null) {
                port.mode = f[2];  //trunk or access
              }
              if (group != null) {
                group.mode = f[2];  //trunk or access
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
          if (port != null) {
            port.ip = f[2];
            port.mask = f[3];
          }
          if (group != null) {
            group.ip = f[2];
            group.mask = f[3];
          }
          if (vlan != null) {
            vlan.ip = f[2];
            vlan.mask = f[3];
          }
          break;
        case "no":
          switch(f[2]) {
            case "ip":
              if (port != null) {
                port.ip = null;
                port.mask = null;
              }
              if (group != null) {
                group.ip = null;
                group.mask = null;
              }
              if (vlan != null) {
                vlan.ip = null;
                vlan.mask = null;
              }
              break;
          }
          break;
        case "shutdown":
          break;
        case "spanning-tree":
          break;
      }
    }
    device.removeInvalid();
    return true;
  }

  public boolean queryState(Device device) {
    //TODO : get link, etc.
    return false;
  }
}
