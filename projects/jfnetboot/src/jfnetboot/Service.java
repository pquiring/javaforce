package jfnetboot;

/** jfPiThinClient service.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.*;
import javaforce.service.*;

public class Service {
  public static TFTP tftp;
  public static Config config;
  public static DHCP dhcp;

  public static void main(String[] args) {
    serviceStart(args);
  }

  public static void serviceStart(String[] args) {
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
    dhcp = new DHCP();
    dhcp.setNotify(tftp);
    dhcp.start();
    config = new Config();
    config.init();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        serviceStop();
      }
    });
  }

  public static void serviceStop() {
    JFLog.log("jfNetBoot stopping...");
    if (dhcp != null) {
      dhcp.close();
      dhcp = null;
    }
    Clients.close();
  }
}
