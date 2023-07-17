package javaforce.service;

/** SMTPRelay
 *
 * Logs into POP3 service periodically and re-sends all messages to another SMTP service.
 *
 * Typical usage:
 *   Install SMTP/POP3 service in insecure network configured in digest mode.
 *   Install SMTPRelay service in secure network and relay messages from insecure network to corporate SMTP service.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

public class SMTPRelay extends Thread {

  private static String pop3_host;
  private static int pop3_port;
  private static boolean pop3_secure;
  private static String pop3_user;  //login name (required)
  private static String pop3_pass;  //login pass (required)

  private static String smtp_host;
  private static int smtp_port;
  private static boolean smtp_secure;
  private static String smtp_user;  //login name (optional)
  private static String smtp_pass;  //login pass (optional)
  private static String smtp_from;  //override 'from' email (optional)

  private static long interval = 15;  //how often (minutes) to check for new messages

  //private static SMTPRelay service;
  private static volatile boolean active;
  private static Object lock = new Object();
  private static SMTPRelay service;

  private static boolean debug = false;

  public final static String busPack = "net.sf.jfsmtprelay";

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfsmtprelay.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfsmtprelay.log";
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33011;
    } else {
      return 777;
    }
  }

  public void run() {
    JFLog.append(JF.getLogPath() + "/jfsmtprelay.log", true);
    JFLog.setRetention(30);
    JFLog.log("jfSMTPRelay starting...");
    try {
      loadConfig();
      busClient = new JBusClient(busPack, new JBusMethods());
      busClient.setPort(getBusPort());
      busClient.start();
      if (debug) relay();
      while (active) {
        for(int a=0;a<interval * 60;a++) {
          if (!active) return;
          JF.sleep(1000);
        }
        relay();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void close() {
    active = false;
  }

  private void relay() {
    try {
      javaforce.POP3 pop3 = new javaforce.POP3();  //client
      pop3.debug = debug;
      if (pop3_secure)
        pop3.connectSSL(pop3_host, pop3_port);
      else
        pop3.connect(pop3_host, pop3_port);
      if (!pop3.auth(pop3_user, pop3_pass, javaforce.POP3.AUTH_APOP)) {
        throw new Exception("POP3 auth failed!");
      }
      javaforce.POP3.Message[] list = pop3.list();
      if (list == null || list.length == 0) {
        pop3.disconnect();
        if (debug) JFLog.log("No messages found");
        return;
      }
      if (debug || list.length > 0) JFLog.log("Found " + list.length + " messages to relay");
      for(javaforce.POP3.Message em : list) {
        if (debug) JFLog.log("Relay message:" + em.idx);
        byte[] data = pop3.get(em.idx);
        javaforce.SMTP smtp = new javaforce.SMTP();  //client
        smtp.debug = debug;
        if (smtp_secure)
          smtp.connectSSL(smtp_host, smtp_port);
        else
          smtp.connect(smtp_host, smtp_port);
        if (smtp_user != null && smtp_pass != null) {
          if (!smtp.auth(smtp_user, smtp_pass, javaforce.SMTP.AUTH_LOGIN)) {
            pop3.disconnect();
            throw new Exception("SMTP auth failed!");
          }
        }
        //decode message and re-send
        //MAIL FROM:<...>\r\n
        //RCPT TO:<...>\r\n
        String msg = new String(data);
        String[] lns = msg.split("\r\n");
        String em_from = null;
        ArrayList<String> em_to = new ArrayList<>();
        if (smtp_from != null) {
          em_from = smtp_from;
        } else {
          em_from = getEmail(lns[0]);
        }
        int idx = 1;
        do {
          String ln = lns[idx++];
          if (!ln.startsWith("RCPT TO:")) break;
          em_to.add(getEmail(ln));
        } while (true);

        //send email
        smtp.from(em_from);
        for(String to : em_to) {
          smtp.to(to);
        }
        smtp.data(msg);
        smtp.disconnect();
        pop3.delete(em.idx);
      }
      pop3.disconnect();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private String getEmail(String ln) {
    int i1 = ln.indexOf('<');
    int i2 = ln.indexOf('>');
    if (i1 == -1 || i2 == -1) return ln;
    return ln.substring(i1 + 1, i2);
  }

  enum Section {None, Global};

  private final static String defaultConfig
    = "[global]\n"
    + "#pop3_host=1.2.3.4\n"
    + "#pop3_port=995\n"
    + "#pop3_secure=true #(default = false)\n"
    + "#pop3_user=bob\n"
    + "#pop3_pass=secret\n"
    + "#smtp_host=5.6.7.8\n"
    + "#smtp_port=25\n"
    + "#smtp_secure=false #(default = false)\n"
    + "#smtp_user=bob@example.com #optional\n"
    + "#smtp_pass=secret #optional\n"
    + "#smtp_from=bob@example.com #override from email address (optional)\n"
    + "#interval=15 #how often to check for messages (minutes) (default=15)\n"
    ;

  private void loadConfig() {
    JFLog.log("loadConfig");
    Section section = Section.None;
    try {
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      StringBuilder cfg = new StringBuilder();
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim();
        if (ln.startsWith("#")) continue;
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        int cmt = ln.indexOf('#');
        if (cmt != -1) {
          ln = ln.substring(0, cmt).trim();
        }
        int idx = ln.indexOf("=");
        if (idx == -1) continue;
        String key = ln.substring(0, idx);
        String value = ln.substring(idx + 1);
        switch (section) {
          case None:
          case Global:
            switch (key) {
              case "pop3_host": pop3_host = value; break;
              case "pop3_port": pop3_port = Integer.valueOf(value); break;
              case "pop3_secure": pop3_secure = value.equals("true"); break;
              case "pop3_user": pop3_user = value; break;
              case "pop3_pass": pop3_pass = value; break;

              case "smtp_host": smtp_host = value; break;
              case "smtp_port": smtp_port = Integer.valueOf(value); break;
              case "smtp_secure": smtp_secure = value.equals("true"); break;
              case "smtp_user": smtp_user = value; break;
              case "smtp_pass": smtp_pass = value; break;
              case "smtp_from": smtp_from = value; break;

              case "interval":
                interval = Integer.valueOf(value);
                if (interval < 5) interval = 5;
                if (interval > 1440) interval = 1440;  //one day
              break;
              case "debug": debug = value.equals("true"); break;
            }
            break;
        }
      }
      br.close();
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
      JFLog.log("config not found, creating defaults.");
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(defaultConfig.getBytes());
        fos.close();
        config = defaultConfig;
      } catch (Exception e2) {
        JFLog.log(e2);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    service = new SMTPRelay();
    service.start();
  }

  public static void serviceStop() {
    service.close();
  }

  public static void main(String[] args) {
    serviceStart(args);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        serviceStop();
      }
    });
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static class JBusMethods {
    public void getConfig(String pack) {
      service.busClient.call(pack, "getConfig", service.busClient.quote(service.busClient.encodeString(service.config)));
    }
    public void setConfig(String cfg) {
      //write new file
      JFLog.log("setConfig");
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      JFLog.log("restart");
      service.close();
      service = new SMTPRelay();
      service.start();
    }
  }
}