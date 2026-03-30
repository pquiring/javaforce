package javaforce.utils;

/**
 * Created : July 9, 2012
 *
 * @author pquiring
 */
import javaforce.*;
import javaforce.bus.*;

public class service {

  private static final String systemBus = "javaforce.jflinux.system";

  public static void usage() {
    System.out.println("jfservice <command> <service>\n --status-all = show all services\n command = start | stop | status | restart\n");
    System.exit(1);
  }

  public static void statusAll() {
    JBusClient client = new JBusClient(null);
    client.connect();
    String status = (String)client.invoke(systemBus, "serviceStatusAll", null);
    System.out.println(status.replaceAll("[|]", "\n"));
  }

  public static void runCommand(String cmd, String svc) {
    JBusClient client = new JBusClient(null);
    client.connect();
    if (cmd.equals("start")) {
      System.out.println("Requested Service:" + svc + ":start");
      boolean res = (boolean)client.invoke(systemBus, "startService", new Object[] {svc});
      if (!res ) {
        JFLog.log("Error:Failed to start service");
      }
    } else if (cmd.equals("stop")) {
      System.out.println("Requested Service:" + svc + ":stop");
      boolean res = (boolean)client.invoke(systemBus, "stopService", new Object[] {svc});
      if (!res ) {
        JFLog.log("Error:Failed to start service");
      }
    } else if (cmd.equals("status")) {
      System.out.println("Requested Service:" + svc + ":status");
      String status = (String)client.invoke("javaforce.jflinux.service." + svc, "status", null);
      System.out.println(status.replaceAll("[|]", "\n"));
    } else if (cmd.equals("restart")) {
      runCommand(svc, "stop");
      JF.sleep(2500);
      runCommand(svc, "start");
    } else {
      System.out.println("unknown command:" + cmd);
      usage();
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
    }
    if (args[0].equals("--help")) {
      usage();
    }
    if (args[0].equals("--status-all")) {
      statusAll();
    }
    if (args.length != 2) {
      usage();
    }
    runCommand(args[0], args[1]);
    System.exit(0);
  }
}
