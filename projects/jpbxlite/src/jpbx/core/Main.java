package jpbx.core;

import java.io.*;
import java.net.*;

import javaforce.*;

/** Main
 *
 * @author pquiring
 *
 * Created : Jun 26, 2014
 */

public class Main {
  //linux shutdown function (from command line)
  private static String cfg[][];
  public static void shutdownService() {
    SQL sql = new SQL();
    if (!sql.connect(Service.jdbc)) {
      System.out.println("SQL init failed");
      return;
    }
    cfg = sql.select("SELECT id,value FROM config");
    sql.close();
    //send "SHUTDOWN" command to SIP port (this command is only accepted from localhost)
    try {
      int port = Integer.valueOf(getCfg("port"));
      DatagramSocket sock = new DatagramSocket();  //any port
      byte data[] = "SHUTDOWN SIP/2.0\r\ni: null\r\nt: \"null\" <sip:0@127.0.0.1>\r\nf: \"null\" <sip:0@127.0.0.1>\r\n\r\n".getBytes();
      sock.send(new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), port));
      sock.close();
    } catch (Exception e) {
      System.out.println("Err:" + e);
    }
  }
  private static String getCfg(String id) {
    for(int a=0;a<cfg.length;a++) {
      if (cfg[a][0].equalsIgnoreCase(id)) return cfg[a][1];
    }
    return null;
  }

  public static boolean createDB() {
    if (new File(Service.dbPath + "/jpbxDB/service.properties").exists()) return true;
    JFLog.log("Creating database...");
    SQL sql = new SQL();
    if (!sql.connect(Service.jdbc + ";create=true")) return false;
    sql.execute("CREATE TABLE users (userid VARCHAR(16) PRIMARY KEY NOT NULL, passmd5 VARCHAR(32) NOT NULL)");
    sql.execute("INSERT INTO users (userid, passmd5) VALUES ('admin', '21232f297a57a5a743894a0e4a801fc3')");
    sql.close();
    return true;
  }

  private static Service service;
  private static WebConfig config;

  public static void main(String args[]) {
    serviceStart(args);
  }

  public static void serviceStart(String args[]) {
    Paths.init();
    //load SQL database
    if (!SQL.initClass(SQL.derbySQL)) {
      JFLog.log("Failed to init database");
      return;
    }
    //create SQL database (if needed)
    if (!createDB()) {
      JFLog.log("Failed to create database");
      return;
    }
    //init log files
    JFLog.append(Paths.logs + "jpbx.log", true);
    JFLog.log("jPBXlite/" + Service.getVersion() + " starting...");
//    Service.test();
    service = new Service();
    service.init();
    config = new WebConfig();
    config.start();
  }

  public static void serviceStop() {
    service.uninit();
    config.stop();
  }
}
