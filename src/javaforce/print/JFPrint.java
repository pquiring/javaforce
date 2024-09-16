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

  public static String[] list(String host) {
    try {
      HTTP http = new HTTP();
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
      byte[] data = http.get("/status/" + jobid);
      http.close();
      return new String(data).split("\r\n");
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
}
