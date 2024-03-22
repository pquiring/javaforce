package service;

/** VM Management
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.vm.*;

public class VMM implements VMProvider {
  public Storage[] listPools() {
    return Config.current.pools.toArray(new Storage[0]);
  }

  public NetworkInterface[] listNetworkInterface() {
    return NetworkInterface.listPhysical();
  }

  public NetworkVirtual[] listNetworkVirtual() {
    return NetworkVirtual.listVirtual();
  }

  public NetworkPort[] listNetworkPort(NetworkVirtual virt) {
    return virt.listPort();
  }

  //type = DEVICE.TYPE_...
  public String[] listDevices(int type) {
    Device[] devs = Device.list(type);  //what is "Devs"...?
    String[] names = new String[devs.length];
    int idx = 0;
    for(Device dev : devs) {
      names[idx++] = dev.name;
    }
    return names;
  }

  public String cleanName(String name) {
    return JF.filter(name, JF.filter_alpha_numeric);
  }

  public String cleanNumber(String name) {
    return JF.filter(name, JF.filter_numeric);
  }

  public Storage getPoolByName(String name) {
    for(Storage pool : Config.current.pools) {
      if (pool.name.equals(name)) {
        return pool;
      }
    }
    return null;
  }

  public int getVLAN(String network) {
    for(NetworkVLAN vlan : Config.current.vlans) {
      if (vlan.name.equals(network)) {
        return vlan.vlan;
      }
    }
    return 0;
  }

  public NetworkBridge getBridge(String network) {
    String bridge_name = null;
    for(NetworkVLAN vlan : Config.current.vlans) {
      if (vlan.name.equals(network)) {
        bridge_name = vlan.bridge;
        break;
      }
    }
    if (bridge_name == null) {
      JFLog.log("ERROR:NetworkVLAN not found:" + network);
      return null;
    }
    NetworkBridge[] bridges = NetworkBridge.list();
    for(NetworkBridge bridge : bridges) {
      if (bridge.name.equals(bridge_name)) {
        return bridge;
      }
    }
    JFLog.log("ERROR:NetworkVLAN not found:" + network);
    return null;
  }

  public int getVNCPort(String vmname) {
    int port = 5900;
    VirtualMachine[] vms = VirtualMachine.list();
    boolean ok;
    do {
      port++;
      ok = true;
      for(VirtualMachine vm : vms) {
        if (vm.getVNC() == port) {
          ok = false;
          break;
        }
      }
    } while (!ok);
    return port;
  }

  public String getServerHostname() {
    return Config.current.fqn;
  }
}
