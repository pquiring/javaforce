/** Paths
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String dataPath;
  public static String logsPath;
  public static String cfgsPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfmonitor";
    } else {
      dataPath = "/var/jfmonitor";
    }
    logsPath = dataPath + "/logs";
    cfgsPath = dataPath + "/configs";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(cfgsPath).mkdirs();
    JFLog.append(logsPath + "/system.log", true);
    JFLog.setRetention(30);
    JFLog.log("jfMonitor starting...");
  }
}
