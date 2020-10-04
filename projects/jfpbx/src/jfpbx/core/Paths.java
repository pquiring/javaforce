package jfpbx.core;

/** Paths.
 *
 * @author pquiring
 *
 * Created : Sept 16, 2013
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String sounds;  //where to find wav files (prompts)
  public static String lang;  //"en" (english)
  public static String logs;  //stores log files
  public static String lib;  //used to store voicemail
  public static String etc;  //used to store WebSecure key
  public static String db;

  public static void init() {
    lang = "en";
    if (JF.isWindows()) {
      //find CommonAppDataFolder MSI property
      //ProgramData = C:\ProgramData (Vista/7/8)
      String base = System.getenv("ProgramData");
      if (base == null) {
        //ProgramData is not available on Win2000/XP/2003
        //AllUsersProfile = C:\Documents and Settings\All Users\Application Data
        base = System.getenv("AllUsersProfile");
        if (base == null) {
          JFLog.log("Unable to find program data");
          System.exit(1);
        }
      }
      base += "/jfpbx";
      sounds = base + "/sounds/";
      logs = base + "/logs/";
      lib = base + "/";
      etc = base + "/";
      db = base + "/db";
    } else {
      //Linux
      sounds = "/usr/share/sounds/jpbx/";
      logs = "/var/log/jpbx/";
      lib = "/var/lib/jpbx/";
      etc = "/etc/";
      db = lib + "/db";
    }
    System.setProperty("derby.system.home", db);
    //ensure some folders exist
    new File(logs).mkdirs();
    new File(lib).mkdirs();
    new File(etc).mkdirs();
    new File(db).mkdirs();
  }

  public static void setLang(String newLang) {
    lang = newLang;
  }
}
