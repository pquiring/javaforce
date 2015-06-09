package javaforce.utils;

/**
 * Created : July 9, 2012
 *
 * @author pquiring
 */
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

public class service {

  public static void usage() {
    System.out.println("jservice <name> <command>\n --status-all = show all services\n command = start | stop | status | restart\n");
    System.exit(1);
  }

  public static void statusAll() {
    Random r = new Random();
    int id = Math.abs(r.nextInt());
    JBusClient client = new JBusClient("org.jflinux.servicemanager.j" + id, new JBusMethods());
    client.start();
    client.call("org.jflinux.jsystemmgr", "serviceStatusAll", "\"" + client.pack + "\"");
    //wait 4 ever
    while (true) {
      JF.sleep(5000);
    }
  }

  public static void runCommand(String svc, String cmd) {
    Random r = new Random();
    int id = Math.abs(r.nextInt());
    JBusClient client = new JBusClient("org.jflinux.servicemanager.j" + id, new JBusMethods());
    client.start();
    if (cmd.equals("start")) {
      System.out.println("Requested Service:" + svc + ":start");
      client.call("org.jflinux.jsystemmgr", "startService", "\"" + svc + "\"");
    } else if (cmd.equals("stop")) {
      System.out.println("Requested Service:" + svc + ":stop");
      client.call("org.jflinux.jsystemmgr", "stopService", "\"" + svc + "\"");
    } else if (cmd.equals("status")) {
      System.out.println("Requested Service:" + svc + ":status");
      client.call("org.jflinux.service." + svc, "status", "\"" + client.pack + "\"");
      //wait upto 5 secs
      JF.sleep(5000);
      System.out.println("no response");
    } else if (cmd.equals("restart")) {
      runCommand(svc, "stop");
      JF.sleep(2500);
      runCommand(svc, "start");
    } else {
      System.out.println("unknown command:" + cmd);
      usage();
    }
  }

  public static void main(String args[]) {
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

  public static class JBusMethods {

    public void serviceStatusAll(String status) {
      System.out.println(status.replaceAll("[|]", "\n"));
      System.exit(1);
    }

    public void serviceStatus(String status) {
      System.out.println(status.replaceAll("[|]", "\n"));
      System.exit(1);
    }
  }
}
