package jfcontrols.app;

/** App paths
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String dataPath;
  public static String configPath;
  public static String tagsPath;
  public static String backupPath;
  public static String logsPath;
  public static String imagesPath;
  public static String visionPath;
  public static String visionImagesPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfcontrols";
    } else {
      dataPath = "/var/jfcontrols";
    }
    configPath = dataPath + "/config";
    tagsPath = configPath + "/tags";
    logsPath = dataPath + "/logs";
    backupPath = dataPath + "/backups";
    imagesPath = dataPath + "/images";
    visionPath = dataPath + "/vision";
    visionImagesPath = dataPath + "/vision/images";
    new File(configPath).mkdirs();
    new File(tagsPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(backupPath).mkdirs();
    new File(imagesPath).mkdirs();
    new File(visionPath).mkdirs();
    new File(visionImagesPath).mkdirs();
    JFLog.append(logsPath + "/service.log", true);
    JFLog.log("jfControls starting...");
  }
}
