package service;

/** Config
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.vm.*;

public class Config implements Serializable {
  private static final long serialVersionUID = 1L;
  public static Config current;

  //config settings
  public String password;
  public String fqn;
  public String iqn;
  public int auto_start_delay = 60;
  public ArrayList<String> auto_start_vms = new ArrayList<>();

  //storage pools
  public ArrayList<Storage> pools = new ArrayList<>();

  //networking config
  public ArrayList<NetworkVLAN> vlans = new ArrayList<>();  //network vlan groups (port groups)
  public ArrayList<NetworkVirtual> nics = new ArrayList<>();  //vm kernel nics

  public Config() {
    valid();
  }

  private void valid() {
    if (fqn == null) {
      fqn = "localhost.localdomain";
    }
    if (iqn == null) {
      iqn = IQN.generate(fqn);
    }
    if (pools == null) {
      pools = new ArrayList<>();
    }
    if (vlans == null) {
      vlans = new ArrayList<>();
    }
    if (nics == null) {
      nics = new ArrayList<>();
    }
    if (auto_start_delay < 30) {
      auto_start_delay = 30;
    }
    if (auto_start_delay > 600) {
      auto_start_delay = 600;
    }
    if (auto_start_vms == null) {
      auto_start_vms = new ArrayList<>();
    }
  }

  public static boolean load() {
    String file = Paths.dataPath + "/config.dat";
    try {
      current = (Config)Compression.deserialize(file);
      if (current == null) throw new Exception("failed to load config");
      current.valid();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      current = new Config();
      current.valid();
      return false;
    }
  }

  public synchronized boolean save() {
    valid();
    String file = Paths.dataPath + "/config.dat";
    try {
      return Compression.serialize(file, current);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void addNetworkVLAN(NetworkVLAN network) {
    vlans.add(network);
    save();
  }

  public void removeNetworkVLAN(NetworkVLAN network) {
    vlans.remove(network);
    save();
  }

  public void addNetworkVirtual(NetworkVirtual network) {
    nics.add(network);
    save();
  }

  public void removeNetworkVirtual(NetworkVirtual network) {
    nics.remove(network);
    save();
  }

  public void addStorage(Storage pool) {
    pools.add(pool);
    save();
  }

  public void removeStorage(Storage pool) {
    pools.remove(pool);
    save();
  }

  public void addVirtualMachine(VirtualMachine vm) {
    //no-op
  }

  public void removeVirtualMachine(VirtualMachine vm) {
    //remove from auto start vms
    if (auto_start_vms.contains(vm.name)) {
      auto_start_vms.remove(vm.name);
      save();
    }
  }
}
