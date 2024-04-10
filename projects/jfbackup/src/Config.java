/** Config
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Config implements Serializable {
  public static final long serialVersionUID = 1;

  public static final String AppVersion = "0.24";

  public static final String APIVersion = "V004";
  public static final int APIVersionMin = 4;
  public static final int APIVersionReadFolders = 3;

  public static Config current;

  public String mode;  //'install','client','server'
  public String server_host;  //if client
  public String this_host;
  public String password;  //in plain text

  //if client
//  public boolean authFailed;

  //if server
  public String tapeDevice;  //tape0
  public String changerDevice;  //changer0 (black = no changer)
  public ArrayList<String> exclude;  //exclude barcode from backup
  public ArrayList<String> hosts;  //should be transient
  public String restorePath;
  public String cleanPrefix;  //"CLN"
  public String cleanSuffix;  //"CN"
  public boolean skipBadFiles;  //skip bad files during restore process
  //notification settings
  public String email_server;
  public boolean email_secure;
  public String email_user, email_pass;
  public String email_type;
  public String emails;

  //backup jobs
  public ArrayList<EntryJob> backups;

  public int retention_years;
  public int retention_months;

  public Config() {
    mode = "install";
    server_host = null;
    this_host = null;
    tapeDevice = "tape0";
    changerDevice = "changer0";
    hosts = new ArrayList<String>();
    exclude = new ArrayList<String>();
    restorePath = System.getenv("SystemDrive") + "\\restored";
    backups = new ArrayList<EntryJob>();
    retention_years = 1;
    retention_months = 0;
    cleanPrefix = "CLN";
    cleanSuffix = "CU";
    email_server = "";
    email_user = "";
    email_pass = "";
    email_type = SMTP.AUTH_LOGIN;
  }

  public static void load() {
    try {
      FileInputStream fis = new FileInputStream(Paths.dataPath + "/config.dat");
      ObjectInputStream ois = new ObjectInputStream(fis);
      current = (Config)ois.readObject();
      current.hosts = new ArrayList<>();
      if (current.email_type == null) {
        current.email_type = SMTP.AUTH_LOGIN;
      }
      fis.close();
    } catch (FileNotFoundException e) {
      current = new Config();
      JFLog.log("No config found!");
    } catch (Exception e) {
      current = new Config();
      JFLog.log(e);
    }
  }

  public synchronized static void save() {
    try {
      FileOutputStream fos = new FileOutputStream(Paths.dataPath + "/config.dat");
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(current);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
