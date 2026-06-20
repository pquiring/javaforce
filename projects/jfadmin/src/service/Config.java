package service;

/** Admin Config
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

  public Config() {
    valid();
  }

  private void valid() {
  }

  public static boolean load() {
    String file = Paths.dataPath + "/admin/config.dat";
    try {
      current = (Config)Compression.deserialize(file);
      if (current == null) throw new Exception("failed to load config");
      current.valid();
      return true;
    } catch (FileNotFoundException e) {
      JFLog.log("config not found, using defaults.");
      current = new Config();
      current.valid();
      return false;
    } catch (Exception e) {
      JFLog.log(e);
      current = new Config();
      current.valid();
      return false;
    }
  }

  public synchronized boolean save() {
    valid();
    String file = Paths.dataPath + "/admin/config.dat";
    try {
      return Compression.serialize(file, current);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}
