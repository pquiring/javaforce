package jfnetboot;

/** Settings
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Settings {
  public static String version = "0.2";
  public static Settings current = new Settings();

  public String filesystem_default = "default";
  public int web_port = 80;
  public String password = "21232f297a57a5a743894a0e4a801fc3";  //default = "admin"

  private static String configFile() {
    return Paths.config + "/settings.cfg";
  }

  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(configFile());
      Properties props = new Properties();
      props.setProperty("filesystem.default", filesystem_default);
      props.setProperty("web.port", Integer.toString(web_port));
      props.setProperty("password", password);
      props.store(fos, "jfNetBoot");
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
      current.filesystem_default = getProperty(props, "filesystem.default", "default");
      current.web_port = Integer.valueOf(getProperty(props, "web.port", "80"));
      current.password = getProperty(props, "password", "21232f297a57a5a743894a0e4a801fc3");
    } catch (FileNotFoundException e) {
      current = new Settings();
      current.save();
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
    }
  }

  public static String encodePassword(String pass) {
    MD5 md5 = new MD5();
    md5.init();
    md5.add(pass);
    return md5.toString();
  }
}
