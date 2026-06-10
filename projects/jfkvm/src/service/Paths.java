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
  public static String tasksPath;
  public static String accessPath;

  public static int LOG_DEFAULT = 0;
  public static int LOG_SYSTEMD = 1;

  public static void init(boolean servlet) {
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
    tasksPath = dataPath + "/tasks";
    if (servlet) {
      accessPath = JF.getConfigPath() + "/javaforce/access";
    } else {
      accessPath = dataPath + "/access";
    }
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(volsPath).mkdirs();
    new File(clusterPath).mkdirs();
    new File(secretPath).mkdirs();
    new File(statsPath).mkdirs();
    new File(tasksPath).mkdirs();
    new File(accessPath).mkdirs();
    Linux.chmod(accessPath, 0700);  //must secure folder
    String log_kvm;
    if (!servlet) {
      log_kvm = logsPath + "/jfkvm.log";
    } else {
      log_kvm = logsPath + "/jfkvm-webui.log";
    }
    JFLog.append(LOG_DEFAULT, log_kvm, true);
    JFLog.setRetention(LOG_DEFAULT, 30);
    String log_systemd;
    if (!servlet) {
      log_systemd = logsPath + "/systemd.log";
    } else {
      log_systemd = logsPath + "/systemd-webui.log";
    }
    JFLog.append(LOG_SYSTEMD, log_systemd, true);
    JFLog.setRetention(LOG_SYSTEMD, 30);
    JFLog.setShowCause(true);
    ServiceControl.setLog(LOG_SYSTEMD);
  }
}
