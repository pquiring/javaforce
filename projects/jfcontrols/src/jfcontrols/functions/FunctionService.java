package jfcontrols.functions;

/** Logic Service
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class FunctionService extends Thread {
  public static String dataPath;
  public static String databaseName = "jfcontrols";
  public static String logsPath;
  public static String derbyURI;

  public static void main() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfcontrols";
    } else {
      dataPath = "/var/jfcontrols";
    }
    logsPath = dataPath + "/logs/service.log";
    derbyURI = "jdbc:derby:jfcontrols";

    new File(logsPath).mkdirs();
    JFLog.init(logsPath, true);
    System.setProperty("derby.system.home", dataPath);
    if (!new File(dataPath + "/" + databaseName + "/service.properties").exists()) {
      //create database
      SQL sql = new SQL();
      sql.connect(derbyURI + ";create=true");
      //create tables
      sql.close();
    } else {
      //update database if required
      SQL sql = new SQL();
      sql.connect(derbyURI);

      sql.close();
    }
    //start executor
    new FunctionService().start();
  }
  public void run() {
    //execute logic
  }
  public static boolean generateFunction(int fid, SQL sql) {
    String code = FunctionCompiler.generateFunction(fid, sql);
    new File("work/java").mkdirs();
    new File("work/class").mkdirs();
    String java_file = "work/java/Code_" + fid + ".java";
    String class_file = "work/class/Code_" + fid + ".class";
    try {
      FileOutputStream fos = new FileOutputStream(java_file);
      fos.write(code.getBytes());
      fos.close();
      return true;
    } catch (Exception e) {
      new File(java_file).delete();
      JFLog.log(e);
      return false;
    }
  }
  public static boolean compileProgram(SQL sql) {
    try {
      //TODO : log output - use javaforce.ShellProcess
      Process p = Runtime.getRuntime().exec(new String[] {"javac" , "work/java/*.java", "-d", "work/class"});
      p.waitFor();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}
