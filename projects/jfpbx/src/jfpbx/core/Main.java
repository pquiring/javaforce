package jfpbx.core;

import jfpbx.db.Database;
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
  public static void shutdownService() {
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
    return Database.getConfig(id);
  }

  private static Service service;
  private static WebConfig config;

  public static void main(String args[]) {
    serviceStart(args);
  }

  public static void serviceStart(String args[]) {
    Paths.init();
    //start database
    Database.start();
    //init log files
    JFLog.append(Paths.logs + "jpbx.log", true);
    JFLog.log("jfPBX/" + Service.getVersion() + " starting...");
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
