/** Notification
 *
 * @author pquiring
 */

import javaforce.*;

public class Notify {
  private static boolean isValid(String str) {
    if (str == null) return false;
    if (str.length() == 0) return false;
    return true;
  }
  public static void notify(String ip, boolean online) {
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
        smtp.from("jfBackup@" + Config.current.server_host);
      }
      String emails[] = Config.current.emails.split(",");
      for(String email : emails) {
        smtp.to(email);
      }
      StringBuilder msg = new StringBuilder();
      if (isValid(Config.current.email_user)) {
        msg.append("From: <" + Config.current.email_user + ">\r\n");
      } else {
        msg.append("From: <jfBackup@" + Config.current.server_host + ">\r\n");
      }
      for(String email : emails) {
        msg.append("To: <" + email + ">\r\n");
      }
      msg.append("Subject:Network Notification");
      msg.append("\r\n\r\n");
      msg.append("Device IP:" + ip + "\r\n");
      msg.append("Online:" + online + "\r\n");
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
  private static void log(Exception e) {
    JFLog.log(3, e);
  }
  private static void log(String msg) {
    JFLog.log(msg);
  }
}
