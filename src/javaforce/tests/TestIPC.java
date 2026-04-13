package javaforce.tests;

/** Test IPC (DBus)
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.ipc.*;
import javaforce.ipc.transport.*;

public class TestIPC {

  private static boolean debug = false;

  private static boolean use_tcp = false;
  private static int tcp_port = 8001;

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("usage:TestIPC {server | client} [options]");
      System.out.println(" client options : [--threads=#] [--delay=#]");
      return;
    }
    switch (args[0]) {
      case "server": server(); break;
      case "client": client(args); break;
      default: System.out.println("Unknown option:" + args[0]);
    }
  }
  private static void server() {
    DBusTransport transport;
    try {
      if (use_tcp) {
        transport = new TCPTransport(tcp_port);
      } else {
        transport = DBus.createTransport();
      }
      IPC ipc = new DBus(new TestEndPoint("javaforce.TestIPC.Server"), transport);
      if (!ipc.connect()) {
        JFLog.log("IPC.connect() failed");
        return;
      }
      while (true) {
        JF.sleep(1000);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static long success = 0;
  private static long error = 0;

  private static void client(String[] args) {
    int threads = 1;
    int delay = 1000;
    for(String arg : args) {
      if (arg.startsWith("--threads=")) {
        threads = Integer.valueOf(arg.substring(10));
      } else if (arg.startsWith("--delay=")) {
        delay = Integer.valueOf(arg.substring(8));
      }
    }
    for(int a=0;a<threads;a++) {
      Client client = new Client(delay);
      client.start();
    }
    while (true) {
      JF.sleep(1000);
      System.out.print(String.format("\rsuccess = %d : error = %d", success, error));
    }
  }

  private static class Client extends Thread {
    private int delay;
    private Random r = new Random();
    private IPC ipc;
    public Client(int delay) {
      this.delay = delay;
    }
    public void run() {
      DBusTransport transport;
      try {
        if (use_tcp) {
          transport = new TCPTransport(tcp_port);
        } else {
          transport = DBus.createTransport();
        }
        ipc = new DBus(new TestEndPoint(null), transport);
        if (!ipc.connect()) {
          JFLog.log("IPC.connect() failed");
          return;
        }
        while (true) {
          try {
            ping();
            modify();
          } catch (Exception e) {
            JFLog.log(e);
          }
          JF.sleep(delay);
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void ping() throws Exception {
      int value = r.nextInt();
      if (debug) JFLog.log(String.format("ping(0x%x)", value));
      Object result = ipc.invoke("javaforce.TestIPC.Server", "ping", value);
      if (result == null) {
        if (debug) JFLog.log("result == null");
        error++;
      } else {
        if (debug) JFLog.log("result = " + result);
        success++;
      }
    }
    public void modify() throws Exception {
      byte[] data = new byte[3];
      data[0] = 0x11;
      data[1] = 0x22;
      data[2] = 0x33;
      if (debug) JFLog.log(String.format("modify(byte[] {%x %x %x})", data[0], data[1], data[2]));
      byte[] result = (byte[])ipc.invoke("javaforce.TestIPC.Server", "modify", data);
      if (result == null) {
        if (debug) JFLog.log("result == null");
        error++;
      } else {
        if (debug) JFLog.log(String.format("result = %x %x %x", result[0], result[1], result[2]));
        success++;
      }
    }
  }

  public static class TestEndPoint implements EndPoint {
    public TestEndPoint(String name) {
      this.name = name;
      dispatcher = new Dispatcher(new Methods());
    }

    private String name;
    private Dispatcher dispatcher;

    public String getEndPointName() {
      return name;
    }

    public Object dispatch(String method, Object[] args) throws Exception {
      return dispatcher.dispatch(method, args);
    }

    public static class Methods {
      public boolean ping(int value) {
        if (debug) JFLog.log(String.format("ping(0x%x)", value));
        return true;
      }
      public byte[] modify(byte[] data) {
        if (debug) JFLog.log(String.format("modify(byte[] {%x %x %x})", data[0], data[1], data[2]));
        data[0] = 0x44;
        data[1] = 0x55;
        data[2] = 0x66;
        return data;
      }
    }
  }
}
