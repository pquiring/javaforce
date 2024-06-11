/** API Service
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.service.*;

public class APIService extends Thread implements WebHandler {
  private static WebServer web;

  public void run() {
    web = new WebServer();
    web.start(this, 8080);
  }

  public void cancel() {
    web.stop();
  }

  private void getmac(String[] args, WebResponse res) {
    try {
      String ip = null;
      boolean html = false;
      for(String arg : args) {
        int idx = arg.indexOf('=');
        if (idx == -1) continue;
        String key = arg.substring(0, idx);
        String value = arg.substring(idx + 1);
        switch (key) {
          case "ip":
            ip = value;
            break;
          case "html":
            html = value.equals("true");
            break;
        }
      }
      if (ip == null) throw new Exception("bad args");
      String mac = Config.current.getmac(ip);
      if (mac == null) mac = "00:00:00:00:00:00";
      StringBuilder sb = new StringBuilder();
      if (html) {
        sb.append("<body style='margin: 0px; padding: 0px; overflow: hidden;'>");
      }
      sb.append(mac);
      if (html) {
        sb.append("</body>");
      }
      try {res.write(sb.toString().getBytes());} catch (Exception e) {}
    } catch (Exception e) {
      try {res.write("00:00:00:00:00:00".getBytes());} catch (Exception e2) {}
    }
  }

  //WebInterface

  public void doGet(WebRequest req, WebResponse res) {
    String qs = req.getQueryString();
    String[] args = qs.split("[&]");
    for(String arg : args) {
      int idx = arg.indexOf('=');
      if (idx == -1) continue;
      String key = arg.substring(0, idx);
      String value = arg.substring(idx + 1);
      switch (key) {
        case "verb":
          switch (value) {
            case "getmac": getmac(args, res); break;
          }
          break;
      }
    }
  }

  public void doPost(WebRequest req, WebResponse res) {
    doGet(req,res);
  }
}
