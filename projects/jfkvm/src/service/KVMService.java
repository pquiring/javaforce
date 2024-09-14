package service;

/** KVM Service
 *
 * Created : Mar 1, 2024
 *
 * @author pquiring
 *
 */

import javaforce.*;
import javaforce.vm.*;
import javaforce.service.*;
import javaforce.webui.tasks.*;

public class KVMService extends Thread {
  public static KVMService kvmService;
  public static ConfigService configService;
  public static WebServerRedir redirService;
  public static Stats stats;

  public static void serviceStart(String args[]) {
    if (kvmService != null) return;
    kvmService = new KVMService();
    kvmService.start();
  }

  public static void serviceStop() {
    if (kvmService == null) return;
    kvmService.cancel();
    kvmService = null;
  }

  public void run() {
    //init paths
    Paths.init();
    //libvirt init
    if (!VirtualMachine.init()) {
      JFLog.log("Failed to load libvirt");
      return;
    }
    //load config
    Config.load();
    //startup services
    new Startup().start();
    //starts tasks service
    Tasks.init();
    //start hosts service
    Hosts.init();
    //start stats timer
    stats = new Stats();
    stats.start();
    //start config service
    configService = new ConfigService();
    configService.start();
    //start redir service
    redirService = new WebServerRedir();
    redirService.start(80, 443);
  }

  public void cancel() {
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
    if (Tasks.tasks != null) {
      try {
        Tasks.tasks.cancel();
      } catch (Exception e) {
        JFLog.log(e);
      }
      Tasks.tasks = null;
    }
    if (Hosts.hosts != null) {
      try {
        Hosts.hosts.cancel();
      } catch (Exception e) {
        JFLog.log(e);
      }
      Hosts.hosts = null;
    }
    if (stats != null) {
      try {
        stats.stop();
      } catch (Exception e) {
        JFLog.log(e);
      }
      stats = null;
    }
  }
}
