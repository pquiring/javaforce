/** MonitorService
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.service.*;
import javaforce.net.*;

public class MonitorService extends Thread {
  public static ConfigService configService;
  public static WebServerRedir redirService;
  public static MonitorService monitorService;
  public static APIService apiService;
  public static Server server;
  public static QueryClients query;
  public static Client client;

  private static PacketCapture capture;

  public static void serviceStart(String args[]) {
    if (monitorService != null) return;
    monitorService = new MonitorService();
    monitorService.start();
  }
  public static void serviceStop() {
    monitorService.cancel();
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
    if (redirService != null) {
      try {
        redirService.stop();
      } catch (Exception e) {
        JFLog.log(e);
      }
      redirService = null;
    }
  }

  public void run() {
    Status.active = true;
    Paths.init();

    capture = new PacketCapture();
    if (!capture.init()) {
      JFLog.log("pcap init failed");
    } else {
      JFLog.log("pcap init successful");
      Config.pcap = true;
    }

    //load current config
    Config.load();
    //start config service
    configService = new ConfigService();
    configService.start();
    //start redir service
    redirService = new WebServerRedir();
    redirService.start(80, 443);
    //start api service
    apiService = new APIService();
    apiService.start();
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
