package javaforce.service.servlet;

/** Paths
 *
 * @author peter.quiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String dataPath;
  public static String logsPath;
  public static String deployPath;
  public static String workingPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfservlets";
    } else {
      dataPath = "/var/jfservlets";
    }
    logsPath = dataPath + "/logs";
    new File(logsPath).mkdirs();
    deployPath = dataPath + "/deploy";
    new File(deployPath).mkdirs();
    workingPath = dataPath + "/working";
    new File(workingPath).mkdirs();
  }
}
