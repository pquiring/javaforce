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
  public String password = null;
  public String fqn = "localhost.localdomain";
  public String iqn = IQN.generate("localhost.localdomain");

  //public VirtualMachine[] machines = new VirtualMachine[0];
  public Storage[] pools = new Storage[0];
/*
  public NetworkInterface[] networks_phys = new NetworkInterface[0];
  public NetworkVirtual[] networks_virt = new NetworkVirtual[0];
  public NetworkPort[] networks_port = new NetworkPort[0];
*/
  public Network[] networks_host = new Network[0];

  public Config() {
    valid();
  }

  private void valid() {
/*
    if (machines == null) {
      machines = new VirtualMachine[0];
    }
*/
    if (pools == null) {
      pools = new Storage[0];
    }
/*
    if (networks_phys == null) {
      network_phys = new NetworkInterface[0];
    }
    if (networks_virt == null) {
      networks_virt = new NetworkVirtual[0];
    }
    if (networks_port == null) {
      networks_port = new NetworkPort[0];
    }
*/
    if (networks_host == null) {
      networks_host = new Network[0];
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
