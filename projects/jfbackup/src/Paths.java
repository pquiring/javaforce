/** Paths
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static String dataPath;
  public static String logsPath;
  public static String catalogsPath;
  public static String vssPath;
//  public static String tempPath;

  public static void init() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfbackup";
    } else {
      System.out.println("Please get a real OS");
      System.exit(1);
    }
    logsPath = dataPath + "/logs";
    catalogsPath = dataPath + "/catalogs";
    vssPath = dataPath + "/vss";  //do not create
//    tempPath = dataPath + "/temp";
    new File(dataPath).mkdirs();
    new File(logsPath).mkdirs();
    new File(catalogsPath).mkdirs();
//    new File(tempPath).mkdirs();
    JFLog.append(logsPath + "/system.log", true);  //0
    JFLog.append(1, logsPath + "/service.log", true);
    JFLog.log("jfBackup starting...");
  }
}
