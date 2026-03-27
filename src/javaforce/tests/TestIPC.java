package javaforce.tests;

/** Test IPC (DBus)
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.ipc.*;

public class TestIPC {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("usage:TestIPC {server | client}");
      return;
    }
    switch (args[0]) {
      case "server": server(); break;
      case "client": client(); break;
      default: System.out.println("Unknown option:" + args[0]);
    }
  }
  private static void server() {
    try {
      IPC ipc = new DBus(new TestEndPoint("TestIPC.Server"));
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
  private static void client() {
    try {
      Random r = new Random();
      IPC ipc = new DBus(new TestEndPoint(String.format("TestIPC.Client%04x", (r.nextInt() & 0xffff))));
      if (!ipc.connect()) {
        JFLog.log("IPC.connect() failed");
        return;
      }
      while (true) {
        try {
          int value = r.nextInt();
          JFLog.log(String.format("ping(0x%x)", value));
          Object result = ipc.invoke("TestIPC.Server", "ping", new Object[] {value});
          if (result == null) {
            JFLog.log("result == null");
          } else {
            JFLog.log("result = " + result);
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
        JF.sleep(1000);
      }
    } catch (Exception e) {
      JFLog.log(e);
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
        JFLog.log(String.format("ping:0x%x", value));
        return true;
      }
    }
  }
}
