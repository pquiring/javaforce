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

public class TransportTCPServer implements SIPTransport {
  protected ServerSocket ss;
  private HashMap<String, Socket> clients = new HashMap<String, Socket>();
  private Object clientsLock = new Object();
  private boolean active;

  public String getName() { return "TCP"; }

  public boolean open(String localhost, int localport) {
    try {
      ss = new ServerSocket(localport);
      active = true;
      new WorkerAccepter().start();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean close() {
    active = false;
    try {
      if (ss != null) {
        ss.close();
        ss = null;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    return true;
  }

  public boolean send(byte[] data, int off, int len, InetAddress host, int port) {
    String id = host.toString() + ":" + port;
    Socket socket;
    synchronized(clientsLock) {
      socket = clients.get(id);
    }
    try {
      if (socket == null) socket = connect(host, port, id);
      OutputStream os = socket.getOutputStream();
      os.write(data, off, len);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  private ArrayList<Packet> packets = new ArrayList<Packet>();
  private Object packetsLock = new Object();

  public boolean receive(Packet packet) {
    while (true) {
      synchronized(packetsLock) {
        if (packets.size() == 0) {
          try{packetsLock.wait();} catch (Exception e) {}
        }
//        if (packets.size() == 0) continue;
        Packet tmp = packets.remove(0);
        packet.data = tmp.data;
        packet.host = tmp.host;
        packet.length = tmp.length;
        packet.port = tmp.port;
        return true;
      }
    }
  }

  private Socket connect(InetAddress host, int port, String id) throws Exception {
    Socket socket = new Socket(host, port);
    synchronized(clientsLock) {
      clients.put(id, socket);
    }
    return socket;
  }

  protected class WorkerAccepter extends Thread {
    public void run() {
      //accepts inbound connections
      while (active) {
        try {
          Socket socket = ss.accept();
          InetAddress host = socket.getInetAddress();
          int port = socket.getPort();
          String id = host.toString() + ":" + port;
          synchronized(clientsLock) {
            clients.put(id, socket);
          }
          new WorkerReader(socket, id, host, port).start();
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }

  private class WorkerReader extends Thread {
    private Socket socket;
    private String id;
    private InputStream is;
    private InetAddress host;
    private int port;
    public WorkerReader(Socket socket, String id, InetAddress host, int port) {
      this.socket = socket;
      this.id = id;
      this.host = host;
      this.port = port;
    }
    public void run() {
      //reads packets from client
      process();
      synchronized(clientsLock) {
        clients.remove(id);
      }
    }
    private byte extra[] = null;
    private int detectLength(byte data[], int off, int len) {
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
        JFLog.log(e);
        return;
      }
      while (active) {
        try {
          byte data[] = new byte[1500];
          Packet packet = new Packet();
          packet.data = data;

          if (extra != null) {
            System.arraycopy(extra, 0, packet.data, 0, extra.length);
            packet.length = extra.length;
            extra = null;
          } else {
            packet.length = is.read(packet.data);
          }
          int plen, tlen;
          do {
            //detect end of packet (double \r\n)
            plen = detectLength(packet.data, 0, packet.length);
            if (plen == -1) {
              //not enough read (frag?)
              packet.length += is.read(packet.data, packet.length, packet.data.length - packet.length);
            }
          } while (plen == -1);
          tlen = plen;
          //now find Content-Length:
          String msg[] = new String(packet.data, 0, plen).split("\r\n");
          String clenstr = SIP.getHeader("Content-Length:", msg);
          if (clenstr == null) SIP.getHeader("l:", msg);
          if (clenstr != null) {
            int clen = JF.atoi(clenstr);
            tlen += clen;
          }
          while (packet.length < tlen) {
            //not enough read (frag?)
            packet.length += is.read(packet.data, packet.length, packet.data.length - packet.length);
          }
          if (packet.length > tlen) {
            //extra read (from next packet)
            extra = new byte[packet.length - tlen];
            System.arraycopy(packet.data, tlen, extra, 0, packet.length - tlen);
            packet.length = tlen;
          }
          //host and port never change
          packet.host = host.getHostAddress();
          packet.port = port;

          synchronized(packetsLock) {
            packets.add(packet);
            packetsLock.notify();
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }
}
