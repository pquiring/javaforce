package service;

/** Config
 *
 * @author pquiring
 */

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import javaforce.*;
import javaforce.vm.*;

public class Config implements Serializable {
  private static final long serialVersionUID = 1L;
  public static Config current;

  //config settings
  public String password;
  public String fqn;
  public String iqn;
  public String token;
  public int auto_start_delay = 60;
  public ArrayList<String> auto_start_vms = new ArrayList<>();

  //storage pools
  public ArrayList<Storage> pools = new ArrayList<>();

  //networking config
  public ArrayList<NetworkVLAN> vlans = new ArrayList<>();  //network vlan groups (port groups)
  public ArrayList<NetworkVirtual> nics = new ArrayList<>();  //vm kernel nics

  //remove host tokens : host, token
  public HashMap<String, String> hosts = new HashMap<>();

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
    if (hosts == null) {
      hosts = new HashMap<>();
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
    if (token == null) {
      token = UUID.generate();
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

  public String getKeyStatus() {
    if (isKeyValid()) return "Valid"; else return "Not found";
  }

  public boolean isKeyValid() {
    File file1 = new File(Paths.clusterPath + "/localhost");
    File file2 = new File("/root/.ssh/authorized_keys");
    return file1.exists() && file2.exists();
  }

  public String getToken() {
    return token;
  }

  public static String[] getRemoteHosts() {
    ArrayList<String> hosts = new ArrayList<>();
    File file = new File(Paths.clusterPath);
    File[] files = file.listFiles();
    if (files == null) files = new File[0];
    for(File host_file : files) {
      String host = host_file.getName();
      if (host.equals("localhost")) continue;
      hosts.add(host);
    }
    return hosts.toArray(JF.StringArrayType);
  }

  public boolean saveHost(String hostname, byte[] key, String token) {
    if (key == null || key.length == 0) return false;
    String keyfile = Paths.clusterPath + "/" + hostname;
    try {
      File file = new File(keyfile);
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(key);
      fos.close();
      //adjust permissions
      ShellProcess sp = new ShellProcess();
      sp.run(new String[] {"/usr/bin/chmod", "600", keyfile}, false);
      //remove any previous entries from known_hosts
      sp.run(new String[] {"/usr/bin/ssh-keygen", "-R", hostname}, false);
      //add host to known_hosts
      String output = sp.run(new String[] {"/usr/bin/ssh-keyscan", "-H", hostname}, false);
      //append output to known_hosts
      FileOutputStream known_hosts = new FileOutputStream("/root/.ssh/known_hosts", true);
      known_hosts.write(output.getBytes());
      known_hosts.close();
      hosts.put(hostname, token);
      save();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public String getHostToken(String hostname) {
    return hosts.get(hostname);
  }

  public void removeHost(String hostname) {
    try {
      File file = new File(Paths.clusterPath + "/" + hostname);
      file.delete();
      hosts.remove(hostname);
      save();
    } catch (Exception e) {
      JFLog.log(e);
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
