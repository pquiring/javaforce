package javaforce.jbus;

/** CLI Client
 *
 * @author pquiring
 */

import javaforce.*;

public class Client {
  public static void usage() {
    JFLog.log("usage : Invoke package [calls...]");
  }
  private static boolean active = true;
  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
      return;
    }
    JBusClient client = new JBusClient(args[0], null);
    client.start();
    while (!client.ready()) {
      JF.sleep(100);
    }
    client.setDispatch((cmd) -> {
      JFLog.log(cmd);
      if (cmd.endsWith(".exit()")) {
        active = false;
      }
    });
    for(int a=1;a<args.length;a++) {
      client.call(args[a]);
    }
    while (active) {
      JF.sleep(1000);
    }
  }
}
