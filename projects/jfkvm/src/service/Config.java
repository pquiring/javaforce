package service;

/** KVM Config
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
  public String fqn;
  public String token;
  public int auto_start_delay = 60;
  public int stats_days = 3;
  public ArrayList<String> auto_start_vms = new ArrayList<>();

  //storage pools
  public ArrayList<Storage> pools = new ArrayList<>();

  //networking config
  public ArrayList<NetworkVLAN> vlans = new ArrayList<>();  //network vlan groups (port groups)
  public ArrayList<NetworkVirtual> nics = new ArrayList<>();  //vm kernel nics

  //remote hosts
  public HashMap<String, Host> hosts = new HashMap<>();

  //vnc port range (10000 - 60000)
  public int vnc_start  = 10000;
  public int vnc_length = 50000;

  public String vnc_password = "password";

  public static Password passwd;

  public Config() {
    valid();
  }

  private void valid() {
    if (fqn == null) {
      fqn = "localhost.localdomain";
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
    upgradeHosts();
    validateHosts();
    if (auto_start_delay < 30) {
      auto_start_delay = 30;
    }
    if (auto_start_delay > 600) {
      auto_start_delay = 600;
    }
    if (stats_days < 3) {
      stats_days = 3;
    }
    if (stats_days > 365) {
      stats_days = 365;
    }
    if (auto_start_vms == null) {
      auto_start_vms = new ArrayList<>();
    }
    if (token == null) {
      token = JF.generateUUID();
    }
    if (vnc_start < 1024) {
      vnc_start = 10000;
    }
    if (vnc_length <= 0) {
      vnc_length = 20000;
    }
    if (vnc_password == null) {
      vnc_password = generate_password();
    }
    //load password
    passwd = Password.load(Password.TYPE_SYSTEM, "jfkvm");
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
      //TODO : only keep lines that start with |
      //append output to known_hosts
      FileOutputStream known_hosts = new FileOutputStream("/root/.ssh/known_hosts", true);
      known_hosts.write(output.getBytes());
      known_hosts.close();
      Host host = new Host();
      host.host = hostname;
      host.token = token;
      host.online = true;
      host.valid = true;
      host.getVersion();
      host.hostname = host.getHostname();
      hosts.put(hostname, host);
      save();
      //update ocfs2 cluster
      if (Storage.format_supported(Storage.FORMAT_OCFS2)) {
        update_ocfs2();
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public String getHostToken(String hostname) {
    Host host = hosts.get(hostname);
    if (host == null) return null;
    return host.token;
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

  public Host[] getHosts() {
    return hosts.values().toArray(new Host[0]);
  }

  /** Returns all hosts FQN including self. */
  public String[] getHostNames() {
    String[] names = new String[hosts.size() + 1];
    Host[] hosts = getHosts();
    int idx = 0;
    for(Host host : hosts) {
      names[idx++] = host.host;
    }
    names[idx] = fqn;
    return names;
  }

  public boolean gluster_ready() {
    Host[] hosts = getHosts();
    for(Host host : hosts) {
      if (!host.online || !host.gluster) return false;
    }
    return true;
  }

  private void upgradeHosts() {
    //up to version 0.3 the host was a String of token
    String[] keys = hosts.keySet().toArray(JF.StringArrayType);
    int upgraded = 0;
    for(String key : keys) {
      Object value = hosts.get(key);
      if (value instanceof String) {
        Host host = new Host();
        host.host = key;
        host.token = (String)value;
        hosts.put(key, host);  //replace value
        upgraded++;
      }
      if (value instanceof Host) {
        Host host = (Host)value;
        if (host.host == null) {
          host.host = key;
          upgraded++;
        }
      }
    }
    if (upgraded > 0) {
      JFLog.log("NOTE:Upgraded " + upgraded + " hosts");
      save();
    }
  }

  public void validateHosts() {
    //TODO
  }

  public void addNetworkVLAN(NetworkVLAN network) {
    vlans.add(network);
    save();
  }

  public void removeNetworkVLAN(NetworkVLAN network) {
    vlans.remove(network);
    save();
  }

  public NetworkVLAN getNetworkVLAN(String name) {
    for(NetworkVLAN vlan : vlans) {
      if (vlan.name.equalsIgnoreCase(name)) {
        return vlan;
      }
    }
    return null;
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

  public void update_ocfs2() {
    try {
      //TODO
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String generate_password() {
    StringBuilder sb = new StringBuilder();
    Random r = new Random();
    for(int a=0;a<8;a++) {
      int cls = r.nextInt(3);
      switch (cls) {
        case 0:  //number
          sb.append((char)('0' + r.nextInt(10)));
          break;
        case 1:  //lower case
          sb.append((char)('a' + r.nextInt(26)));
          break;
        case 2:  //upper case
          sb.append((char)('A' + r.nextInt(26)));
          break;
      }
    }
    return sb.toString();
  }
}
