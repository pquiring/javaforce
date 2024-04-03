package jfnetboot;

/** jfPiThinClient service.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.service.*;

public class Service {
  public static TFTP tftp;
  public static ConfigService configService;
  public static WebServerRedir redirService;
  public static DHCPServer dhcp;

  public static void main(String[] args) {
    serviceStart(args);
  }

  public static void serviceStart(String[] args) {
    System.setProperty("java.net.preferIPv4Addresses", "true");
    Paths.init();
    JFLog.append(Paths.logs + "/jfnetboot.log", true);
    JFLog.log("jfNetBoot " + Settings.version + " starting...");
    Settings.load();
    JF.exec(new String[] {"exportfs", "-ua"});
    FileSystems.init();
    Commands.init();
    Clients.init();
    tftp = new TFTP();
    tftp.start();
    dhcp = new DHCPServer();
    dhcp.setNotify(tftp);
    dhcp.start();
    //start config service
    configService = new ConfigService();
    configService.start();
    //start redir service
    redirService = new WebServerRedir();
    redirService.start(80, 443);
  }

  public static void serviceStop() {
    JFLog.log("jfNetBoot stopping...");
    if (dhcp != null) {
      dhcp.close();
      dhcp = null;
    }
    Clients.close();
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
}
