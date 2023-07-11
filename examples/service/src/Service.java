/** Example service
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.controls.*;
import javaforce.service.*;

public class Service extends Thread implements WebHandler {
  public static String version = "0.1";
  public static Service service;
  public static Web web;
  public boolean active;

  public static final String name = "TemplateService";
  public static final int port = 8099;  //service web status port

  public static void serviceStart(String args[]) {
    String logPath = JF.getLogPath() + "/service";
    new File(logPath).mkdir();
    JFLog.init(logPath + "/service.log", true);
    JFLog.setRetention(30);
    JFLog.log(name + " started/" + version);
    service = new Service();
    service.start();
    web = new Web();
    web.start(service, port, false);
  }

  public static void serviceStop() {
    if (service != null) {
      JFLog.log("Service stopped");
      service.cancel();
      web.stop();
    }
  }

  public static void main(String args[]) {
    serviceStart(args);
  }

  public void cancel() {
    active = false;
  }

  public void restart() {
    cancel();
    service = new Service();
    service.start();
  }

  public void doPost(WebRequest req, WebResponse res) {
    doGet(req, res);
  }

  public void doGet(WebRequest req, WebResponse res) {
    String qs = req.getQueryString();
    if (qs.equals("restart=true")) {
      //TODO : add authentication if desired
      service.restart();
      try {
        res.write("OK".getBytes());
      } catch (Exception e) {
      }
      return;
    }
    String server = "http://" + req.getHost();
    StringBuilder sb = new StringBuilder();
    sb.append("<script>function restart() {var xhttp = new XMLHttpRequest(); xhttp.open('GET', '" + server + ":" + port + "?restart=true', true); xhttp.send(); return false;}</script>");
    sb.append("<h1>" + name + " Status/" + version + "</h1>");
    if (service.active) {
      sb.append("<h2><button onclick='restart();'>Restart Service</button> (wait 5 seconds and refresh with F5)</h2><br>");
    } else {
      sb.append("<h2>Restarting...Keep refreshing with F5...</h2><br>");
    }
    sb.append("<br>");
    try {
      res.write(sb.toString().getBytes());
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static SQL getSQL(String database) {
    SQL sql = new SQL();
    if (!sql.connect(SQL.msSQL ,"jdbc:sqlserver://sfsmsmes01:1433;databaseName=" + database + ";user=sfs;password=Manager-10")) {
      JFLog.log(sql.lastException);
      return null;
    }
    return sql;
  }

  public void run() {
    active = true;
    while (active) {
      //nothing to do on main thread but sleep
      JF.sleep(1000);
    }
  }
}
