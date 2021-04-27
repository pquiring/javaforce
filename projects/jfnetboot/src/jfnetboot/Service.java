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
  public static RPC rpc;
  public static Config config;
  public static DHCP dhcp;

  public static void main(String[] args) {
    serviceStart(args);
  }

  public static void serviceStart(String[] args) {
    JFLog.append("boot.log", true);  //test
    JFNative.load();
    Paths.init();
    FileSystems.init();
    Commands.init();
    Clients.init();
    tftp = new TFTP();
    tftp.start();
    rpc = new RPC();
    rpc.start();
    dhcp = new DHCP();
    dhcp.setNotify(tftp);
    dhcp.start();
    config = new Config();
    config.init();
  }

  public static void serviceStop() {
    //TODO
  }
}
