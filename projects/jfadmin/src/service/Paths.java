package service;

/** Paths
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.linux.*;

public class Paths {
  public static String dataPath;
  public static String adminPath;
  public static String tasksPath;
  public static String logsPath;
  public static String accessPath;

  public static int LOG_DEFAULT = 0;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/JavaForce";
    } else {
      dataPath = "/etc/javaforce";
    }
    adminPath = dataPath + "/admin";
    tasksPath = JF.getDataPath() + "/jfadmin/tasks";
    logsPath = JF.getLogPath() + "/jfadmin";
    accessPath = dataPath + "/access";
    new File(dataPath).mkdirs();
    new File(adminPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(accessPath).mkdirs();
    if (JF.isUnix()) {
      Linux.chmod(accessPath, 0700);  //must secure folder
    }
    JFLog.append(LOG_DEFAULT, logsPath + "/jfadmin.log", true);
    JFLog.setRetention(LOG_DEFAULT, 30);
    JFLog.setShowCause(true);
  }
}
