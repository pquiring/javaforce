package javaforce.voip;

/** SIP TCP Transport Server
 *
 * This is very complex since this transport needs to track all endpoints.
 *
 * @author pquiring
 *
 * Created : Jan 29, 2014
 */

import java.net.*;
import java.io.*;
import java.util.*;

import javaforce.*;

public class TransportTCPServer implements Transport {
  protected ServerSocket ss;
  private HashMap<String, Socket> clients = new HashMap<String, Socket>();
  private Object clientsLock = new Object();
  private boolean ss_active;
  private boolean ss_error;
  private static final int mtu = 1460;  //max size of packet
  private TransportInterface iface;

  public static boolean debug = true;

  public String getName() { return "TCP"; }

  public boolean open(String localhost, int localport, TransportInterface iface) {
    this.iface = iface;
    try {
      ss = new ServerSocket(localport);
      ss_active = true;
      new WorkerAccepter().start();
    } catch (Exception e) {
      if (debug) JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean close() {
    ss_active = false;
    try {
      if (ss != null) {
        ss.close();
        ss = null;
      }
    } catch (Exception e) {
      if (debug) JFLog.log(e);
    }
    synchronized(packetsLock) {
      packetsLock.notify();
    }
    return true;
  }

  private void removeClient(String host, int port, String id) {
    if (debug) JFLog.log("Transport:removeClient:" + id);
    iface.onDisconnect(host, port);
    synchronized(clientsLock) {
      if (clients.containsKey(id)) {
        clients.remove(id);
      } else {
        JFLog.log("Error:TrannsportTCPServer:removeClient:not found:" + id);
      }
    }
  }

  private void addClient(String host, int port, String id) {
    if (debug) JFLog.log("Transport:addClient:" + id);
    iface.onConnect(host, port);
    synchronized(clientsLock) {
      if (clients.containsKey(id)) {
        JFLog.log("Error:TrannsportTCPServer:addClient:already exists:" + id);
      } else {
        clients.remove(id);
      }
    }
  }

  public boolean send(byte[] data, int off, int len, InetAddress hostaddr, int port) {
    String host = hostaddr.getHostAddress();
    String id = host + ":" + port;
    Socket socket;
    if (debug) JFLog.log("Transport:get:" + id);
    synchronized(clientsLock) {
      socket = clients.get(id);
    }
    try {
      if (socket == null) socket = connect(hostaddr, port, id);
      OutputStream os = socket.getOutputStream();
      os.write(data, off, len);
    } catch (SocketException se) {
      if (debug) JFLog.log("TransportTCPServer:Connection lost");
      removeClient(host, port, id);
      return false;
    } catch (Exception e) {
      if (debug) JFLog.log(e);
      removeClient(host, port, id);
      return false;
    }
    return true;
  }

  private ArrayList<Packet> packets = new ArrayList<Packet>();
  private Object packetsLock = new Object();

  public boolean receive(Packet packet) {
    while (ss_active) {
      synchronized(packetsLock) {
        if (packets.size() == 0) {
          try{packetsLock.wait();} catch (Exception e) {}
        }
        if (packets.size() == 0) continue;
        Packet tmp = packets.remove(0);
        System.arraycopy(tmp.data, 0, packet.data, 0, mtu);
        packet.host = tmp.host;
        packet.length = tmp.length;
        packet.port = tmp.port;
        return true;
      }
    }
    return false;
  }

  private Socket connect(InetAddress hostaddr, int port, String id) throws Exception {
    Socket socket = new Socket(hostaddr, port);
    if (debug) JFLog.log("Transport:put:" + id);
    addClient(hostaddr.getHostAddress(), port, id);
    return socket;
  }

  protected class WorkerAccepter extends Thread {
    public void run() {
      //accepts inbound connections
      while (ss_active) {
        try {
          Socket socket = ss.accept();
          InetAddress hostaddr = socket.getInetAddress();
          String host = hostaddr.getHostAddress();
          int port = socket.getPort();
          String id = host + ":" + port;
          addClient(host, port, id);
          new WorkerReader(socket, id, hostaddr, port).start();
        } catch (SocketException e) {
          if (debug) JFLog.log("TransportTCPServer.WorkerAccepter:disconnected");
        } catch (Exception e) {
          if (debug) JFLog.log(e);
        }
      }
    }
  }

  private class WorkerReader extends Thread {
    private Socket socket;
    private String id;
    private InputStream is;
    private InetAddress hostaddr;
    private String host;
    private int port;
    private boolean worker_active;
    private boolean worker_error;
    public WorkerReader(Socket socket, String id, InetAddress hostaddr, int port) {
      if (debug) JFLog.log("WorkerReader:" + hostaddr.getHostAddress() + ":" + port);
      this.socket = socket;
      this.id = id;
      this.hostaddr = hostaddr;
      this.port = port;
      host = hostaddr.getHostAddress();
    }
    public void run() {
      worker_active = true;
      //reads packets from client
      process();
      removeClient(host, port, id);
    }
    private byte[] extra = null;
    private int detectLength(byte[] data, int off, int len) {
      for(int a=0;a<len-3;a++) {
        if (
          (data[off+a+0] == '\r') &&
          (data[off+a+1] == '\n') &&
          (data[off+a+2] == '\r') &&
          (data[off+a+3] == '\n')
        ) {
          return a+4;
        }
      }
      return -1;
    }
    public void process() {
      try {
        is = socket.getInputStream();
      } catch (Exception e) {
        if (debug) JFLog.log(e);
        return;
      }
      if (debug) JFLog.log("TransportTCPServer:Worker:active");
      while (worker_active) {
        try {
          byte[] data = new byte[mtu];
          Packet packet = new Packet();
          packet.data = data;

          if (extra != null) {
            System.arraycopy(extra, 0, packet.data, 0, extra.length);
            packet.length = extra.length;
            extra = null;
          } else {
            int read = is.read(packet.data);
            if (read == -1) throw new Exception();
            packet.length = read;
          }
          int plen, tlen;
          do {
            //detect end of packet (double \r\n)
            plen = detectLength(packet.data, 0, packet.length);
            if (plen == -1) {
              //not enough read (frag?)
              int read = is.read(packet.data, packet.length, packet.data.length - packet.length);
              if (read == -1) throw new Exception();
              packet.length += read;
            }
          } while (plen == -1);
          tlen = plen;
          //now find Content-Length:
          String[] msg = new String(packet.data, 0, plen).split("\r\n");
          String clenstr = SIP.getHeader("Content-Length:", msg);
          if (clenstr == null) SIP.getHeader("l:", msg);
          if (clenstr != null) {
            int clen = JF.atoi(clenstr);
            tlen += clen;
          }
          while (packet.length < tlen) {
            //not enough read (frag?)
            int read = is.read(packet.data, packet.length, packet.data.length - packet.length);
            if (read == -1) throw new Exception();
            packet.length += read;
          }
          if (packet.length > tlen) {
            //extra read (from next packet)
            extra = new byte[packet.length - tlen];
            System.arraycopy(packet.data, tlen, extra, 0, packet.length - tlen);
            packet.length = tlen;
          }
          //host and port never change
          packet.host = hostaddr.getHostAddress();
          packet.port = port;

          synchronized(packetsLock) {
            packets.add(packet);
            packetsLock.notify();
          }
        } catch (SocketException se) {
          worker_error = true;
          worker_active = false;
          removeClient(host, port, id);
          if (debug) JFLog.log("TransportTCPServer:disconnected");
        } catch (Exception e) {
          worker_error = true;
          worker_active = false;
          removeClient(host, port, id);
          if (debug) JFLog.log(e);
        }
      }
    }
  }
  public boolean error() {
    return ss_error;
  }
}
