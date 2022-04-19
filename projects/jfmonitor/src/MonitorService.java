/** MonitorService
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.*;
import javaforce.net.*;

public class MonitorService extends Thread {
  public static ConfigService configService;
  public static MonitorService monitorService;
  public static Server server;
  public static QueryClients query;
  public static Client client;

  private static PacketCapture capture;

  public static void serviceStart(String args[]) {
    main(args);
  }
  public static void serviceStop() {
    monitorService.cancel();
  }
  public static void main(String args[]) {
    if (monitorService != null) return;
    monitorService = new MonitorService();
    monitorService.start();
  }

  public void cancel() {
    JFLog.log("jfMonitor shuting down...");
    Status.active = false;
    if (server != null) {
      server.cancel();
    }
    if (client != null) {
      client.close();
    }
  }

  public void run() {
    JFNative.load();
    capture = new PacketCapture();
    if (!capture.init()) {
      System.out.println("pcap init failed");
      return;
    } else {
      System.out.println("pcap init successful");
    }

    Status.active = true;
    Paths.init();
    //load current config
    Config.load();
    //start config service
    configService = new ConfigService();
    configService.start();
    //start client or server thread
    switch (Config.current.mode) {
      case "client": startClient(); break;
      case "server": startServer(); break;
    }
  }

  public static void startClient() {
    client = new Client();
    client.start();
  }

  public static void startServer() {
    server = new Server();
    server.start();
    query = new QueryClients();
    query.start();
  }
}
