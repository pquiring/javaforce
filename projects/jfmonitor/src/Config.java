/** Config
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Config implements Serializable {
  public static final long serialVersionUID = 1;

  public static final String AppVersion = "0.18";

  public static final String APIVersion = "V001";

  public static Config current;

  public static boolean pcap;
  public static boolean debug;

  public String mode;  //'install','client','server'
  public String server_host;  //if client
  public String this_host;
  public String password;  //in plain text

  //if client
  //TODO : extra conditions to watch on client (folder size, file size, etc)

  //if server
  private ArrayList<Network> networks;  //ping only
  private ArrayList<Device> devices;  //known devices
  public boolean notify_unknown_device;
  public transient ArrayList<String> hosts;  //clients (file systems)

  //notification settings
  public String email_server;
  public String emails;  //comma seperated list
  public boolean email_secure;
  public String email_user, email_pass;
  public String email_type;

  public Config() {
    mode = "install";
    server_host = null;
    this_host = null;
    hosts = new ArrayList<String>();
    networks = new ArrayList<Network>();
    devices = new ArrayList<Device>();
    email_type = SMTP.AUTH_LOGIN;
  }

  private void validate() {
    if (hosts == null) {
      hosts = new ArrayList<>();
    }
    if (networks == null) {
      networks = new ArrayList<>();
    }
    for(Network nw : networks) {
      nw.validate();
    }
    if (devices == null) {
      devices = new ArrayList<>();
    }
    if (email_type == null) {
      email_type = SMTP.AUTH_LOGIN;
    }
  }

  public static void load() {
    JFLog.log("Loading config...");
    try {
      FileInputStream fis = new FileInputStream(Paths.dataPath + "/config.dat");
      ObjectInputStream ois = new ObjectInputStream(fis);
      current = (Config)ois.readObject();
      fis.close();
    } catch (FileNotFoundException e) {
      current = new Config();
      JFLog.log("No config found!");
    } catch (Exception e) {
      current = new Config();
      JFLog.log(e);
    }
    current.validate();
  }

  public synchronized static void save() {
    JFLog.log("Saving config...");
    try {
      FileOutputStream fos = new FileOutputStream(Paths.dataPath + "/config.dat");
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(current);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public ArrayList<Network> getNetworks() {
    return networks;
  }

  public void addNetwork(Network nw) {
    networks.add(nw);
  }

  public void removeNetwork(Network nw) {
    networks.remove(nw);
  }

  public Device getDevice(String mac) {
    for(Device dev : devices) {
      if (dev.mac.equals(mac)) {
        return dev;
      }
    }
    return null;
  }

  public Device[] getDevices() {
    Device[] list = devices.toArray(new Device[0]);
    Arrays.sort(list);
    return list;
  }

  public void addDevice(Device dev) {
    devices.add(dev);
  }

  public String getmac(String ip) {
    for(Network nw : Config.current.getNetworks()) {
      for(IP nwip : nw.ips) {
        if (nwip.host.equals(ip)) {
          String mac = nwip.mac;
          return mac;
        }
      }
    }
    return null;
  }

  public String getip(String mac) {
    for(Network nw : Config.current.getNetworks()) {
      for(IP nwip : nw.ips) {
        if (nwip.mac.equals(mac)) {
          String ip = nwip.host;
          return ip;
        }
      }
    }
    return null;
  }
}
