package service;

/** VM Management
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.vm.*;

public class VMM implements VMProvider {
  public String[] listVMs() {
    VirtualMachine[] vms = VirtualMachine.list();
    String[] names = new String[vms.length];
    int idx = 0;
    for(VirtualMachine vm : vms) {
      names[idx++] = vm.getName();
    }
    return names;
  }

  public String[] listPools() {
    return Storage.list();
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

  public VirtualMachine getVMByName(String name) {
    return VirtualMachine.get(name);
  }

  public String getVMPool(String vmname) {
    VirtualMachine vm = VirtualMachine.get(vmname);
    if (vm == null) return null;
    return vm.getPool();
  }

  public Storage getPoolByName(String name) {
    for(Storage pool : Config.current.pools) {
      if (pool.name.equals(name)) return pool;
    }
    return null;
  }

  public Hardware loadHardware(String pool, String name) {
    //load hardware from \volumes\pool\name.jfkvm
    String file = "/volumes/" + pool + "/" + name + "/" + name + ".jfkvm";
    return Hardware.load(file);
  }

  public boolean saveHardware(String pool, Hardware hw) {
    String file = "/volumes/" + hw.pool + "/" + hw.name + "/" + hw.name + ".jfkvm";
    return hw.save(file);
  }

  public String cleanName(String name) {
    return JF.filter(name, JF.filter_alpha_numeric);
  }

  public int getVLAN(String network) {
    for(NetworkVLAN vlan : Config.current.networks_vlans) {
      if (vlan.name.equals(network)) {
        return vlan.vlan;
      }
    }
    return 0;
  }

  public NetworkBridge getBridge(String network) {
    String bridge_name = null;
    for(NetworkVLAN vlan : Config.current.networks_vlans) {
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
}
