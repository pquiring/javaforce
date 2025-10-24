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
  public static String cfgsPath;
  public static String tasksPath;
  public static String accessPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfmonitor";
    } else {
      dataPath = "/var/jfmonitor";
    }
    logsPath = dataPath + "/logs";
    cfgsPath = dataPath + "/configs";
    tasksPath = dataPath + "/tasks";
    accessPath = dataPath + "/access";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(cfgsPath).mkdirs();
    new File(tasksPath).mkdirs();
    new File(accessPath).mkdirs();
    Linux.chmod(accessPath, 0700);  //must secure it
    JFLog.append(logsPath + "/system.log", true);
    JFLog.setRetention(30);
    JFLog.log("jfMonitor starting...");
  }
}
