package jfnetboot;

/** jfPiThinClient service.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.*;

public class Service {
  public static TFTP tftp;
  public static RPC rpc;
  public static Config config;

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
    config = new Config();
    config.init();
  }

  public static void serviceStop() {

  }
}
