package javaforce.service.servlet;

/** Settings
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Settings {
  public static Settings current = new Settings();

  public int http_port = 8080;
  public int https_port = 8443;

  private static String configFile() {
    return Paths.dataPath + "/settings.cfg";
  }

  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(configFile());
      Properties props = new Properties();
      props.setProperty("http.port", Integer.toString(http_port));
      props.setProperty("https.port", Integer.toString(https_port));
      props.store(fos, "jfServlets");
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static String getProperty(Properties p, String k, String def) {
    String v = p.getProperty(k);
    if (v == null) v = def;
    return v;
  }

  public static void load() {
    try {
      current = new Settings();
      FileInputStream fis = new FileInputStream(configFile());
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      current.http_port = Integer.valueOf(getProperty(props, "http.port", "8080"));
      current.https_port = Integer.valueOf(getProperty(props, "https.port", "8443"));
    } catch (FileNotFoundException e) {
      current = new Settings();
      current.save();
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
    }
  }
}
