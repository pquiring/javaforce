package javaforce.jbus;

/** CLI Invoke
 *
 * @author pquiring
 */

import javaforce.*;

public class Invoke {
  public static void usage() {
    JFLog.log("usage : Invoke package.func(args)");
    JFLog.log(" args : \"strings\", 123, ...");
  }
  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
      return;
    }
    JBusClient client = new JBusClient(null, null);
    client.start();
    while (!client.ready()) {
      JF.sleep(100);
    }
    client.call(args[0]);
  }
}
