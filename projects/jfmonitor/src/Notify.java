/** Notification
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class Notify {
  private static boolean isValid(String str) {
    if (str == null) return false;
    if (str.length() == 0) return false;
    return true;
  }
  private static void notify_email(String subject, String message) {
    if (!isValid(Config.current.email_server)) return;
    if (!isValid(Config.current.emails)) return;
    log("Sending email notification");
    SMTP smtp = new SMTP();
    try {
      int port = -1;
      String server = Config.current.email_server;
      int idx = server.indexOf(':');
      if (idx != -1) {
        port = Integer.valueOf(server.substring(idx+1));
        server = server.substring(0, idx);
      }
      if (Config.current.email_secure) {
        if (port == -1) port = 465;
        smtp.connect(server, port);
      } else {
        if (port == -1) port = 25;
        smtp.connect(server, port);
      }
      if (!smtp.login()) {
        throw new Exception("SMTP HELLO failed");
      }
      if (isValid(Config.current.email_user) && isValid(Config.current.email_pass) && isValid(Config.current.email_type)) {
        if (!smtp.auth(Config.current.email_user, Config.current.email_pass, Config.current.email_type)) {
          throw new Exception("SMTP auth failed");
        }
      }
      if (isValid(Config.current.email_user)) {
        smtp.from(Config.current.email_user);
      } else {
        smtp.from("jfMonitor@" + Config.current.server_host);
      }
      String emails[] = Config.current.emails.split(",");
      for(String email : emails) {
        smtp.to(email);
      }
      StringBuilder msg = new StringBuilder();
      if (isValid(Config.current.email_user)) {
        msg.append("From: <" + Config.current.email_user + ">\r\n");
      } else {
        msg.append("From: <jfMonitor@" + Config.current.server_host + ">\r\n");
      }
      for(String email : emails) {
        msg.append("To: <" + email + ">\r\n");
      }
      msg.append("Subject:");
      msg.append(subject);
      msg.append("\r\n\r\n");
      msg.append(message);
      msg.append("Date:");
      msg.append(ConfigService.toDateTime(System.currentTimeMillis()));
      msg.append("\r\n");
      if (!smtp.data(msg.toString())) {
        throw new Exception("Send email message failed");
      }
      smtp.logout();
      smtp.disconnect();
    } catch (Exception e) {
      log("Email notification failed");
      log("SMTP response=" + smtp.getLastResponse());
      log(e);
    }
  }
  public static void notify_ip(String ip, boolean online) {
    StringBuilder msg = new StringBuilder();
    msg.append("Device IP:" + ip + "\r\n");
    msg.append("Online:" + online + "\r\n");
    notify_email("Network Event", msg.toString());
  }
  public static void notify_storage(String host, String volume, float percent) {
    StringBuilder msg = new StringBuilder();
    msg.append("Host/Storage:" + host + "/" + volume + "\r\n");
    msg.append("Percent Full:" + percent + "\r\n");
    notify_email("Storage Event", msg.toString());
  }
  private static long one_day = (24 * 60 * 60 * 1000);
  public static void notify_unknowns() {
    if (unknown_ts == 0) return;
    StringBuilder msg = new StringBuilder();
    synchronized(unknown_devs) {
      //send out unknown devices report every 24 hours
      long now = System.currentTimeMillis();
      if ((now - unknown_ts) < one_day) return;
      msg.append("Unknown Devices Report:\r\n");
      for(Unknown u : unknown_devs) {
        msg.append("MAC=" + u.mac + ":Network=" + u.net + "\r\n");
      }
      unknown_ts = 0;
    }
    notify_email("Daily Unknown Devices Report", msg.toString());
  }

  private static class Unknown {
    public String mac;
    public String net;
  }

  private static long unknown_ts;
  private static ArrayList<Unknown> unknown_devs = new ArrayList<>();
  public static void add_unknown_device(String mac, String net) {
    synchronized(unknown_devs) {
      if (unknown_ts == 0) {
        unknown_ts = System.currentTimeMillis();
      }
      for(Unknown u : unknown_devs) {
        if (u.mac.equals(mac) && u.net.equals(net)) {
          return;
        }
      }
      Unknown u = new Unknown();
      unknown_devs.add(u);
    }
  }
  private static void log(Exception e) {
    JFLog.log(3, e);
  }
  private static void log(String msg) {
    JFLog.log(msg);
  }
}
