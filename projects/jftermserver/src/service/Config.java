package service;

/** Term Server Config
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Config implements Serializable {
  private static final long serialVersionUID = 1L;
  public static Config current;

  //config settings
  public ArrayList<PortSettings> ports = new ArrayList<>();

  public Config() {
    valid();
  }

  private void valid() {
    if (ports == null) {
      ports = new ArrayList<>();
    }
  }

  public void setPortSettings(String port, String name, String baud) {
    //update existing entry
    for(PortSettings cfg : ports) {
      if (cfg.port.equals(port)) {
        cfg.name = name;
        cfg.baud = baud;
        save();
        return;
      }
    }
    //create new entry
    PortSettings cfg = new PortSettings();
    cfg.port = port;
    cfg.name = name;
    cfg.baud = baud;
    ports.add(cfg);
    save();
  }

  public PortSettings getPortSettings(String port) {
    for(PortSettings cfg : ports) {
      if (cfg.port.equals(port)) {
        return cfg;
      }
    }
    return null;
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
