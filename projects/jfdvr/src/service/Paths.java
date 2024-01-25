package service;

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
    JFLog.append(logsPath + "/system.log", true);
    JFLog.setRetention(5);
    JFLog.log("jfDVR starting...");
    JFLog.log("pid=" + ProcessHandle.current().pid());
  }
}
