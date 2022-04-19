/** Paths
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String dataPath;
  public static String logsPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfmonitor";
    } else {
      dataPath = "/var/jfmonitor";
    }
    logsPath = dataPath + "/logs";
//    tempPath = dataPath + "/temp";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
//    new File(tempPath).mkdirs();
    JFLog.append(logsPath + "/system.log", true);  //0
    JFLog.append(1, logsPath + "/service.log", true);
    JFLog.log("jfMonitor starting...");
  }
}
