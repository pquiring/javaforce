package service;

/** VM Management
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.linux.*;
import javaforce.vm.*;
import javaforce.webui.tasks.*;

public class VMM implements VMProvider {
  public String cleanName(String name) {
    return JF.filter(name, JF.filter_id);
  }

  public String cleanURL(String name) {
    return JF.filterOut(name, JF.filter_url);
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
    int vmx_idx = vmx.indexOf(".vmx");
    String name = vmx.substring(0, vmx_idx);
    Hardware hw = new Hardware();
    hw.pool = pool;
    hw.folder = folder;
    hw.name = name;
    //find disks
    String full_folder = "/volumes/" + pool + "/" + folder;
    File[] files = new File(full_folder).listFiles();
    if (files == null) {
      JFLog.log("Error:files==null:" + full_folder);
      return null;
    }
    for(File file : files) {
      String disk_name = file.getName();
      if (disk_name.endsWith("-flat.vmdk")) continue;
      if (disk_name.endsWith(".vmdk")) {
        int idx = disk_name.indexOf('.');
        Disk disk = new Disk();
        disk.pool = pool;
        disk.folder = folder;
        disk.name = disk_name.substring(0, idx);
        disk.type = Disk.TYPE_VMDK;
        String flat_name = full_folder + "/" + disk.name + "-flat.vmdk";
        File flat_file = new File(flat_name);
        if (flat_file.exists()) {
          disk.size = new Size(flat_file.length());
        } else {
          disk.size = new Size(file.length());
        }
        hw.addDisk(disk);
      }
    }
    return hw;
  }

  public boolean cloneData(VirtualMachine vm, Storage dest, String new_name, Status status) {
    return vm.cloneData(dest, new_name, status, this);
  }

  public boolean migrateData(VirtualMachine vm, Hardware hw, Storage dest, Status status) {
    return vm.migrateData(dest, hw, status, this);
  }

  public boolean migrateCompute(VirtualMachine vm, String remote) {
    return vm.migrateCompute(remote, vm.getState() != VirtualMachine.STATE_OFF, null);
  }

  /** Check if VNC port is in use by local VMs. */
  public boolean vnc_port_inuse_local(int port) {
    VirtualMachine[] vms = VirtualMachine.list();
    for(VirtualMachine vm : vms) {
      if (vm.getVNC() == port) {
        return true;
      }
    }
    return false;
  }

  /** Check if VNC port is in use by VMs within remote host. */
  public boolean vnc_port_inuse_remote(int port) {
    try {
      Host[] hosts = Config.current.getHosts();
      for(Host host : hosts) {
        if (!host.isValid()) continue;
        if (vnc_port_inuse_remote(host, port)) return true;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  /** Check if VNC port is in use by VMs within remote host. */
  public boolean vnc_port_inuse_remote(Host host, int port) {
    try {
      if (!host.isValid(0.4f)) return false;
      HTTPS https = new HTTPS();
      https.open(host.host);
      byte[] res = https.get("/api/checkvncport?port=" + port);
      String str = new String(res);
      if (str.equals("inuse")) {
        return true;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
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
    //return random port not in use by any host
    Random r = new Random();
    int port = Config.current.vnc_start;
    port += r.nextInt(Config.current.vnc_length);
    int vnc_end = Config.current.vnc_start + Config.current.vnc_length;
    while (true) {
      if (port == vnc_end) {
        port = Config.current.vnc_start;
      }
      if (port == 21064) {
        //used by dlm (gfs2)
        port++;
        continue;
      }
      if (vnc_port_inuse_local(port)) {
        port++;
        continue;
      }
      if (vnc_port_inuse_remote(port)) {
        port++;
        continue;
      }
      return port;
    }
  }

  public String getVNCPassword() {
    return Config.current.vnc_password;
  }

  public String getServerHostname() {
    return Linux.getHostname();
  }

  public boolean addsshkey(String hostname, String sshkey) {
    Host[] hosts = Config.current.hosts.values().toArray(new Host[0]);
    for(Host host : hosts) {
      if (host.hostname.equals(hostname)) {
        return host.addsshkey(sshkey);
      }
    }
    return false;
  }

  public boolean any_vm_running() {
    VirtualMachine[] vms = VirtualMachine.list();
    for(VirtualMachine vm : vms) {
      if (vm.getState() == VirtualMachine.STATE_ON) return true;
    }
    return false;
  }
}
