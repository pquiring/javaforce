package javaforce.jbus;

/**
 * JBusServer is a server for inter-process communications (RPC).
 *
 * Created : Apr 9, 2012
 *
 * @author pquiring
 */
import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;

public class JBusServer extends Thread {

  public static int port;
  public static volatile boolean ready = false;
  private Vector<Client> clients = new Vector<Client>();
  private boolean active = true;
  private ServerSocket ss;
  private Object lock = new Object();

  /** Create new server on port 777. */
  public JBusServer() {
    port = 777;
  }

  /** Create new server on specified port. */
  public JBusServer(int port) {
    this.port = port;
  }

  public int getClientCount() {
    synchronized(lock) {
      return clients.size();
    }
  }

  public void run() {
    JFLog.append(port, JF.getLogPath() + "/jfbusserver.log", false);
    JFLog.setRetention(port, 30);
    try {
      ss = new ServerSocket(port, 1024, InetAddress.getByName("127.0.0.1"));
      JFLog.log(port, "JBusServer starting on port " + port);
      ready = true;
      while (active) {
        try {
          Socket s = ss.accept();
          //reject s if it's not localhost
          String ip = s.getInetAddress().toString().substring(1);  //strip leading '/'
          if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {  //ip4 || ip6 - localhost
            Client client = new Client(s);
            client.start();
          } else {
            JFLog.log(port, "JBus : Unauthorized client:" + ip);
          }
        } catch (Exception e1) {
          JFLog.log(port, e1);
        }
      }
      ss.close();
    } catch (Exception e2) {
      JFLog.log(port, e2);
    }
  }

  /** Close down server. */
  public void close() {
    if (ss == null) {
      return;
    }
    active = false;
    try {
      ss.close();
    } catch (Exception e) {
    }
    ss = null;
  }

  /** Returns server ready state. */
  public boolean ready() {
    return ready;
  }

  private class Client extends Thread {

    private String pack;
    private Socket s;
    private InputStream is;
    private OutputStream os;

    public Client(Socket s) {
      this.s = s;
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
      } catch (Exception e) {
        JFLog.log(port, e);
      }
    }

    public void run() {
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while (s.isConnected()) {
          String cmd = br.readLine();
          if (cmd == null) {
            break;
          }
          doCmd(cmd);
        }
      } catch (Exception e) {
        JFLog.log(port, e);
      }
      synchronized (lock) {
        clients.remove(this);
      }
      if (pack != null) {
        JFLog.log(port, "JBus : package unregistered:" + pack);
      }
    }

    private void doCmd(String cmd) throws Exception {
      boolean broadcast = false;
      if (cmd.startsWith("cmd.")) {
        if (cmd.startsWith("cmd.package=")) {
          if (pack != null) {
            return;  //ignore
          }
          pack = cmd.substring(12);
          JFLog.log(port, "JBus : package registered:" + pack);
          synchronized (lock) {
            clients.add(this);
          }
          return;
        }
        else if (cmd.startsWith("cmd.broadcast=")) {
          broadcast = true;
          cmd = cmd.substring(14);
        }
        else {
          //unknown cmd
          JFLog.log(port, "JBus : unknown cmd:" + cmd);
          return;
        }
      }
      //must be a remote function call
      //general format : org.package.func(args)
      //supported args : "String", int
      int b1 = cmd.indexOf('(');
      int b2 = cmd.length() - 1;
      String call_pack_func = cmd.substring(0, b1);
      int idx = call_pack_func.lastIndexOf('.');
      if (idx == -1) {
        return;
      }
      String call_pack = call_pack_func.substring(0, idx);
      String call_func = call_pack_func.substring(idx + 1);
      String call_args = cmd.substring(b1);
      cmd += "\n";
      boolean sent = false;
      synchronized (lock) {
        for (int a = 0; a < clients.size(); a++) {
          Client client = clients.get(a);
          if (client.pack.equals(call_pack)) {
            client.os.write(cmd.getBytes());
            client.os.flush();
            sent = true;
          } else if ((broadcast && client.pack.startsWith(call_pack))) {
            //need to use full package name
            String fullcmd = client.pack + "." + call_func + call_args + "\n";
            client.os.write(fullcmd.getBytes());
            client.os.flush();
            sent = true;
          }
        }
      }
      if (!sent) {
        JFLog.log(port, "JBus : call to unregistered package.func:" + call_pack + "." + call_func);
      }
    }

    public boolean call(String pack, String func, String args) {
      return call(pack + "." + func + "(" + args + ")\n");
    }

    public boolean call(String pfa) {
      try {
        os.write(pfa.getBytes());
        os.flush();
        return true;
      } catch (Exception e) {
        JFLog.log(port, e);
        return false;
      }
    }
  }

  /**
   * Broadcasts to call clients that <b>start</b> with the <i>pack</i>.
   */
  public void broadcast(String pack, String func, String args) {
    synchronized (lock) {
      for (int a = 0; a < clients.size(); a++) {
        Client client = clients.get(a);
        if (client.pack != null) {
          if (client.pack.startsWith(pack)) {
            client.call(client.pack, func, args);
          }
        }
      }
    }
  }

  /** Checks if a JBusServer is running. */
  public static boolean present() {
    JBusClient client = new JBusClient(null, null);
    client.setDebug(true);
    client.start();
    boolean present = client.ready();
    if (present) {
      client.close();
    }
    return present;
  }


  public static void main(String args[]) {
    serviceStart(args);
  }

  //Win32 Service

  private static JBusServer svr;

  public static void serviceStart(String args[]) {
    svr = new JBusServer();
    svr.start();
  }

  public static void serviceStop() {
    JFLog.log(port, "Stopping service");
    svr.close();
  }

}
