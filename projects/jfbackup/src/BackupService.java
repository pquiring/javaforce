/** BackupService
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.*;

public class BackupService extends Thread {
  public static ConfigService configService;
  public static BackupService backupService;
  public static TaskScheduler taskScheduler;
  public static Server server;
  public static Client client;

  public static void serviceStart(String args[]) {
    main(args);
  }
  public static void serviceStop() {
    backupService.cancel();
  }
  public static void main(String args[]) {
    JFNative.load();
    if (backupService != null) return;
    backupService = new BackupService();
    backupService.start();
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
