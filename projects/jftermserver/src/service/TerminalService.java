package service;

/** KVM Service
 *
 * Created : Mar 1, 2024
 *
 * @author pquiring
 *
 */

import javaforce.*;
import javaforce.linux.*;
import javaforce.service.*;
import javaforce.webui.tasks.*;

public class TerminalService extends Thread {
  public static TerminalService service;
  public static ConfigService configService;
  public static WebServerRedir redirService;

  public static void serviceStart(String args[]) {
    if (service != null) return;
    service = new TerminalService();
    service.start();
  }

  public static void serviceStop() {
    if (service == null) return;
    service.cancel();
    service = null;
  }

  public void run() {
    //init paths
    Paths.init();
    //detect OS
    if (JF.isUnix()) {
      Linux.detectDistro();
    }
    //load config
    Config.load();
    //starts tasks service
    Tasks.init(Paths.tasksPath);
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
  }

  public static void main(String[] args) {
    serviceStart(args);
  }
}
