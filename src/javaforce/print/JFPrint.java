package javaforce.print;

/** JFPrint
 *
 * Send print jobs to JFPrintServer
 *
 * @author peter.quiring
 */

import javaforce.*;

public class JFPrint {
  public static final String unit_inch = "inch";
  public static final String unit_mm = "mm";

  private static final int port = 33202;

  public static String[] list(String host) {
    try {
      HTTP http = new HTTP();
      if (!http.open(host, port)) return null;
      byte[] data = http.get("/list");
      http.close();
      return new String(data).split("\r\n");
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
  public static String[] query(String host, String printer) {
    try {
      HTTP http = new HTTP();
      if (!http.open(host, port)) return null;
      byte[] data = http.get("/query/" + printer);
      http.close();
      return new String(data).split("\r\n");
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
  public static String[] print(String host, String printer, byte[] png, String width, String height, String unit) {
    try {
      HTTP http = new HTTP();
      if (!http.open(host, port)) return null;
      http.setHeader("width", width);
      http.setHeader("height", height);
      http.setHeader("unit", unit);
      byte[] data = http.post("/print/" + printer, png, "application/png");
      http.close();
      return new String(data).split("\r\n");
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
  public static String[] status(String host, String jobid) {
    try {
      HTTP http = new HTTP();
      if (!http.open(host, port)) return null;
      byte[] data = http.get("/status/" + jobid);
      http.close();
      return new String(data).split("\r\n");
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  public static void main(String[] args) {
    if (args.length != 6) {
      JFLog.log("Usage:JFPrint server printer file.png width height unit");
      return;
    }
    String server = args[0];
    String printer = args[1];
    String file = args[2];
    String width = args[3];
    String height = args[4];
    String unit = args[5];
    byte[] png = JF.readFile(file);
    if (png == null) {
      JFLog.log("unable to open:" + file);
      return;
    }
    String[] ret = print(server, printer, png, width, height, unit);
    for(String r : ret) {
      JFLog.log(r);
    }
  }
}
