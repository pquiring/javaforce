
/**
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String dataPath;
  public static String logsPath;
  public static String videoPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfdvr";
    } else {
      dataPath = "/var/jfdvr";
    }
    logsPath = dataPath + "/logs";
    videoPath = dataPath + "/cameras";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(videoPath).mkdirs();
    JFLog.append(logsPath + "/service.log", true);
    JFLog.log("jfDVR starting...");
  }
}
