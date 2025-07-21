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
  public static String volsPath;
  public static String clusterPath;
  public static String secretPath;
  public static String statsPath;

  public static int LOG_DEFAULT = 0;
  public static int LOG_SYSTEMD = 1;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfkvm";
    } else {
      dataPath = "/var/jfkvm";
    }
    logsPath = dataPath + "/logs";
    volsPath = "/volumes";
    clusterPath = "/root/cluster";
    secretPath = "/root/secret";
    statsPath = dataPath + "/stats";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(volsPath).mkdirs();
    new File(clusterPath).mkdirs();
    new File(secretPath).mkdirs();
    new File(statsPath).mkdirs();
    JFLog.append(LOG_DEFAULT, logsPath + "/jfkvm.log", true);
    JFLog.setRetention(LOG_DEFAULT, 30);
    JFLog.append(LOG_SYSTEMD, logsPath + "/jfkvm-systemd.log", true);
    JFLog.setRetention(LOG_SYSTEMD, 30);
    JFLog.setShowCause(true);
    ServiceControl.setLog(LOG_SYSTEMD);
  }
}
