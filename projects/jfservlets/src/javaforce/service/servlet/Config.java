package javaforce.service.servlet;

/** Config
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Config implements Serializable {
  private static final long serialVersionUID = 1L;
  public static Config current;

  public static final String version = "0.1";

  public Config() {
    valid();
  }

  private void valid() {
  }

  public static boolean load() {
    String file = Paths.dataPath + "/config.dat";
    try {
      current = (Config)Compression.deserialize(file);
      if (current == null) throw new Exception("failed to load config");
      current.valid();
      return true;
    } catch (FileNotFoundException e) {
      current = new Config();
      current.valid();
      current.save();
      return false;
    } catch (Exception e) {
      JFLog.log(e);
      current = new Config();
      current.valid();
      current.save();
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
