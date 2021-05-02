package jfnetboot;

/** Settings
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Settings {
  public static String version = "0.1";
  public static Settings current = new Settings();

  public String defaultFileSystem = "default";
  public int webPort = 80;

  private static String configFile() {
    return Paths.config + "/settings.cfg";
  }

  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(configFile());
      Properties props = new Properties();
      props.setProperty("defaultFileSystem", defaultFileSystem);
      props.setProperty("webPort", Integer.toString(webPort));
      props.store(fos, "jfNetBoot");
      fos.close();
      current.defaultFileSystem = props.getProperty("defaultFileSystem");
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
      current.defaultFileSystem = getProperty(props, "defaultFileSystem", "default");
      current.webPort = Integer.valueOf(getProperty(props, "webPort", "80"));
    } catch (FileNotFoundException e) {
      current = new Settings();
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
    }
  }
}
