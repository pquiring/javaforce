package service;

/** VM Management
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.vm.*;

public class VMM implements VMProvider {
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
    JFLog.log("Error:pool not found:" + name);
    return null;
  }

  /** Convert vmx to jfvm. */
  public Hardware convertVMX(String full, String pool, String folder, String vmx) {
    JFLog.log("convertVMX:" + pool + "," + folder + "," + vmx);
    if (folder.indexOf('/') != -1) {
      JFLog.log("Please place VM into one deep folder in storage pool.");
      return null;
    }
    String name = folder;
    Hardware hw = new Hardware();
    hw.pool = pool;
    hw.name = name;
    //find disks
    String full_folder = pool + "/" + folder;
    File[] files = new File(full_folder).listFiles();
    if (files == null) {
      JFLog.log("Error:files==null:" + full_folder);
      return null;
    }
    for(File file : files) {
      String disk_name = file.getName();
      if (disk_name.endsWith("-flat.vmdk")) continue;
      if (disk_name.endsWith(".vmdk")) {
        Disk disk = new Disk();
        disk.pool = pool;
        disk.folder = folder;
        disk.name = disk_name;
        disk.type = Disk.TYPE_VMDK;
        disk.size = new Size(file.length());
        hw.addDisk(disk);
      }
    }
    return hw;
  }

  public int getVLAN(String network) {
    for(NetworkVLAN vlan : Config.current.vlans) {
      if (vlan.name.equals(network)) {
        return vlan.vlan;
      }
    }
    JFLog.log("Error:network not found:" + network);
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
