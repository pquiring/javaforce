package service;

/** Config
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.vm.*;

public class Config implements Serializable {
  private static final long serialVersionUID = 1L;
  public static Config current;

  //config settings
  public String password;
  public String fqn;
  public String iqn;

  //storage pools
  public Storage[] pools = new Storage[0];

  //networking config
  public NetworkInterface[] network_ifaces = new NetworkInterface[0];  //physical interfaces (eth0, eth1)
//  public NetworkBridge[] network_bridges = new NetworkBridge[0];  //bridges (virbr0)
  public NetworkVLAN[] networks_vlans = new NetworkVLAN[0];  //network vlan groups (port groups)
  public NetworkVirtual[] networks_virt = new NetworkVirtual[0];  //vm kernel nics

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
      pools = new Storage[0];
    }
    if (network_ifaces == null) {
      network_ifaces = new NetworkInterface[0];
    }
/*
    if (network_bridges == null) {
      network_bridges = new NetworkBridge[0];
    }
*/
    if (networks_vlans == null) {
      networks_vlans = new NetworkVLAN[0];
    }
    if (networks_virt == null) {
      networks_virt = new NetworkVirtual[0];
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
}
