package service;

/** KVM Service
 *
 * Created : Mar 1, 2024
 *
 * @author pquiring
 *
 */

import javaforce.*;
import javaforce.service.*;

public class KVMService extends Thread {
  public static KVMService kvmService;
  public static ConfigService configService;
  public static WebServerRedir redirService;
  public static Tasks tasks;

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
    //load config
    Config.load();
    //starts tasks service
    tasks = new Tasks();
    tasks.start();
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
    if (tasks != null) {
      try {
        tasks.cancel();
      } catch (Exception e) {
        JFLog.log(e);
      }
      tasks = null;
    }
  }
}
