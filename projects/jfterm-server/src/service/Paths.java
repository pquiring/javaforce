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
  public static String logsPath;
  public static String tasksPath;
  public static String accessPath;

  public static int LOG_DEFAULT = 0;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jftermserver";
    } else {
      dataPath = "/var/jfkvm";
    }
    logsPath = dataPath + "/logs";
    tasksPath = dataPath + "/tasks";
    accessPath = dataPath + "/access";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(tasksPath).mkdirs();
    new File(accessPath).mkdirs();
    if (JF.isUnix()) {
      Linux.chmod(accessPath, 0700);  //must secure folder
    }
    JFLog.append(LOG_DEFAULT, logsPath + "/jftermserver.log", true);
    JFLog.setRetention(LOG_DEFAULT, 30);
    JFLog.setShowCause(true);
  }
}
