/** Settings
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Settings {
  public static Settings current = new Settings();

  public int web_port = 80;

  private static String configFile() {
    return Paths.dataPath + "/settings.cfg";
  }

  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(configFile());
      Properties props = new Properties();
      props.setProperty("web.port", Integer.toString(web_port));
      props.store(fos, "jfBackup");
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
      current.web_port = Integer.valueOf(getProperty(props, "web.port", "80"));
    } catch (FileNotFoundException e) {
      current = new Settings();
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
    }
  }
}
