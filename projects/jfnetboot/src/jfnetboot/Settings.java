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

  private static String configFile() {
    return Paths.config + "/settings.cfg";
  }

  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(configFile());
      Properties props = new Properties();
      props.setProperty("defaultFileSystem", defaultFileSystem);
      props.store(fos, "jfNetBoot");
      fos.close();
      current.defaultFileSystem = props.getProperty("defaultFileSystem");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void load() {
    try {
      current = new Settings();
      FileInputStream fis = new FileInputStream(configFile());
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      current.defaultFileSystem = props.getProperty("defaultFileSystem");
    } catch (FileNotFoundException e) {
      current = new Settings();
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
    }
  }
}
