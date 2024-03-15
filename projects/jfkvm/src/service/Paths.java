package service;

/** Paths
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String dataPath;
  public static String logsPath;
  public static String volsPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfkvm";
    } else {
      dataPath = "/var/jfkvm";
    }
    logsPath = dataPath + "/logs";
    volsPath = "/volumes";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(volsPath).mkdirs();
  }
}
