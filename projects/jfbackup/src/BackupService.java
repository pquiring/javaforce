/** BackupService
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.*;
import javaforce.service.*;

public class BackupService extends Thread {
  public static ConfigService configService;
  public static WebServerRedir redirService;
  public static BackupService backupService;
  public static TaskScheduler taskScheduler;
  public static Server server;
  public static Client client;

  public static void serviceStart(String args[]) {
    WinNative.vssInit();
    if (backupService != null) return;
    backupService = new BackupService();
    backupService.start();
  }
  public static void serviceStop() {
    backupService.cancel();
  }
  public static void main(String args[]) {
  }

  public void cancel() {
    JFLog.log("jfBackup shuting down...");
    Status.active = false;
    //TODO : stop any backup/restore jobs
    if (server != null) {
      server.cancel();
    }
    if (client != null) {
      client.close();
    }
    if (configService != null) {
      try {
        configService.stop();
      } catch (Exception e) {
        JFLog.log(e);
      }
      configService = null;
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
    //load current config
    Config.load();
    //load tapes config
    Tapes.load();
    //start config service
    JFLog.log("APPDATA=" + System.getenv("APPDATA"));
    configService = new ConfigService();
    configService.start();
    //start redir service
    redirService = new WebServerRedir();
    redirService.start(80, 443);
    //start client or server thread
    switch (Config.current.mode) {
      case "client": startClient(); break;
      case "server": startServer(); break;
    }
    //start task scheduler
    taskScheduler = new TaskScheduler();
    taskScheduler.init();
  }

  public static void startClient() {
    client = new Client();
    client.start();
  }

  public static void startServer() {
    server = new Server();
    server.start();
  }

}
