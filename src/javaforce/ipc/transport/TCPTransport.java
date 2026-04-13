package javaforce.ipc.transport;

/** TCP Server/Client Transport for DBus
 *
 * DBus packets are encapsulated in packet with small header.
 *
 * @author pquiring
 */

import java.net.*;
import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.ipc.*;

public class TCPTransport extends DBusTransport {

  private static boolean debug = false;

  private final int CMD_NAME = 0x01;
  private final int CMD_PACKET_TO_SERVER = 0x02;
  private final int CMD_PACKET_TO_CLIENT = 0x03;
  private final int CMD_CLOSE = 0x0f;

  private int port;
  private boolean active;
  private String busName;
  private boolean localonly = true;

  private ServerSocket server;
  private ServerWorker server_worker;
  private Object clients_lock = new Object();
  private ArrayList<ServerClientWorker> clients = new ArrayList<>();

  private Object packets_lock = new Object();
  private ArrayList<byte[]> packets = new ArrayList<>();

  private Socket client;
  private ClientWorker client_worker;

  public TCPTransport(int port) {
    this.port = port;
  }

  /** Specify if only localhost clients are allowed (default = true) */
  public void setLocalOnly(boolean state) {
    localonly = state;
  }

  public boolean connect(String name, DBus bus, Runnable start_reader) {
    active = true;
    if (name != null) {
      busName = name;
      try {
        if (localonly) {
          server = new ServerSocket(port, 1024, InetAddress.getByName("127.0.0.1"));
        } else {
          server = new ServerSocket(port);
        }
        server_worker = new ServerWorker();
        server_worker.start();
      } catch (Exception e) {
        JFLog.log(e);
        disconnect();
        return false;
      }
    } else {
      Random r = new Random();
      name = String.format("javaforce.client.r%x.t%x", r.nextInt(0xfffffff), System.currentTimeMillis());
      busName = name;
      try {
        client = new Socket("localhost", port);
        client_worker = new ClientWorker();
        client_worker.start();
      } catch (Exception e) {
        JFLog.log(e);
        disconnect();
        return false;
      }
    }
    if (start_reader != null) {
      start_reader.run();
    }
    return true;
  }

  public boolean disconnect() {
    if (debug) JFLog.log("TCPTransport.disconnect()");
    active = false;
    if (server != null) {
      try {server.close();} catch (Exception e) {}
      server = null;
      if (server_worker != null) {
        try {server_worker.join();} catch (Exception e) {}
        server_worker = null;
      }
    }
    if (client != null) {
      client_worker.write_close();
      try {client.close();} catch (Exception e) {}
      client = null;
      if (client_worker != null) {
        try {client_worker.join();} catch (Exception e) {}
        client_worker = null;
      }
    }
    return true;
  }

  /** Read DBus packet. */
  public int read(byte[] data) {
    byte[] pkt;
    synchronized (packets_lock) {
      try {packets_lock.wait(100);} catch (Exception e) {return 0;}
      if (packets.size() == 0) return 0;
      pkt = packets.remove(0);
    }
    if (pkt.length > data.length) {
      JFLog.log("TCPTransport:Error:pkt.length > data.length");
      return -1;
    }
    System.arraycopy(pkt, 0, data, 0, pkt.length);
    return pkt.length;
  }

  /** Write DBus packet. */
  public boolean write(String name, byte[] data, int offset, int length) {
    if (server != null) {
      //need to send to client with name
      if (debug) JFLog.log("write:" + name);
      synchronized (clients) {
        for(ServerClientWorker client : clients) {
          if (client.name == null) continue;
          if (client.name.equals(name)) {
            return client.write_packet(data, offset, length);
          }
        }
      }
      JFLog.log("TCPTransport:Error:client not found:" + name);
      return false;
    } else if (client != null) {
      //send to server
      return client_worker.write_packet_to_server(name, data, offset, length);
    } else {
      JFLog.log("TCPTransport:server/client not active");
      return false;
    }
  }

  public boolean isAlive() {
    return active;
  }

  public String getBusName() {
    return busName;
  }

  private class ServerWorker extends Thread {
    public void run() {
      while (active) {
        try {
          Socket socket = server.accept();
          if (socket != null) {
            String ip = socket.getInetAddress().toString().substring(1);  //strip leading '/'
            if (!localonly || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {  //ip4 || ip6 - localhost
              ServerClientWorker client = new ServerClientWorker();
              client.client = socket;
              synchronized (clients_lock) {
                clients.add(client);
              }
              client.start();
            } else {
              JFLog.log("TCPTransport : Unauthorized client : ip=" + ip);
            }
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
      if (debug) JFLog.log("ServerWorker.done");
    }
  }

  private class ServerClientWorker extends Thread {
    public String name;  //client busName
    public Socket client;  //server side socket
    public InputStream is;
    public OutputStream os;
    private byte[] header = new byte[8];
    private byte[] pkt;

    public void run() {
      try {
        is = client.getInputStream();
        os = client.getOutputStream();
      } catch (Exception e) {
        JFLog.log(e);
        return;
      }
      while (active) {
        try {
          //read packets from client
          if (debug) JFLog.log("Server.reading");
          int read_header = is.read(header, 0, 8);
          if (read_header == -1) break;
          if (read_header != 8) {
            JFLog.log("TCPTransport.ServerClientWorker:read != 8:" + read_header);
            JF.sleep(100);
            continue;
          }
          int cmd = BE.getuint32(header, 0);
          int pkt_len = BE.getuint32(header, 4);
          if (debug) JFLog.log("TCPTransport.ServerClientWorker.read=" + (8 + pkt_len));
          pkt = new byte[pkt_len];
          int read_pkt = is.readNBytes(pkt, 0, pkt_len);
          if (read_pkt != pkt_len) {
            throw new Exception("incomplete packet");
          }
          switch (cmd) {
            case CMD_NAME: name = new String(pkt); break;
            case CMD_PACKET_TO_SERVER: dispatch(pkt); break;
            case CMD_CLOSE:
              disconnect_client();
              return;
            default:
              JFLog.log("TCPTransport:ServerClient:Unknown cmd:" + cmd);
              disconnect_client();
              return;
          }
        } catch (Exception e) {
          JFLog.log(e);
          disconnect_client();
          return;
        }
      }
      if (debug) JFLog.log("ServerClientWorker.done");
    }

    private boolean dispatch(byte[] epkt) {
      //epkt = enclosing packet
      //ipkt = inner packet (DBus)
      int epkt_length = epkt.length;
      int strlen = BE.getuint32(epkt, 0);
      if (strlen > epkt_length) return false;
      int header_size = 4 + strlen;
      String dest = new String(epkt, 4, strlen);
      int ipkt_offset = 4 + strlen;
      int ipkt_length = epkt_length - header_size;
      if (dest.equals(busName)) {
        //packet to server
        byte[] ipkt = Arrays.copyOfRange(epkt, ipkt_offset, ipkt_offset + ipkt_length);
        synchronized (packets_lock) {
          packets.add(ipkt);
          packets_lock.notify();
        }
        return true;
      } else {
        return write(dest, epkt, ipkt_offset, ipkt_length);
      }
    }

    public boolean write_data(byte[] data) {
      if (debug) JFLog.log("write_data(client):" + data.length);
      try {
        os.write(data);
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }

    public boolean write_packet(byte[] data, int offset, int length) {
      byte[] pkt = new byte[8 + length];
      BE.setuint32(pkt, 0, CMD_PACKET_TO_CLIENT);
      BE.setuint32(pkt, 4, length);
      System.arraycopy(data, offset, pkt, 8, length);
      return write_data(pkt);
    }

    public boolean disconnect_client() {
      if (debug) JFLog.log("TCPTransport.disconnect_client()");
      if (client != null) {
        try {client.close();} catch (Exception e) {}
        client = null;
      }
      return true;
    }
  }

  private class ClientWorker extends Thread {
    private byte[] header = new byte[8];
    private byte[] pkt;
    private InputStream is;
    private OutputStream os;
    public void run() {
      try {
        is = client.getInputStream();
        os = client.getOutputStream();
      } catch (Exception e) {
        return;
      }
      if (!write_name()) {
        disconnect_client();
      }
      while (active) {
        try {
          //read packets from server
          if (debug) JFLog.log("Client.reading");
          int read_header = is.read(header, 0, 8);
          if (read_header == -1) break;
          if (read_header != 8) {
            JFLog.log("TCPTransport.ClientWorker:read != 8:" + read_header);
            JF.sleep(100);
            continue;
          }
          int cmd = BE.getuint32(header, 0);
          int pkt_len = BE.getuint32(header, 4);
          if (debug) JFLog.log("TCPTransport.ClientWorker.read=" + (8 + pkt_len));
          pkt = new byte[pkt_len];
          int read_pkt = is.readNBytes(pkt, 0, pkt_len);
          if (read_pkt != pkt_len) {
            throw new Exception("TCPTransport.ClientWorker:Error:incomplete packet");
          }
          if (debug) JFLog.log("TCPTransport.ClientWorker.dispatch");
          switch (cmd) {
            case CMD_PACKET_TO_CLIENT:
              synchronized (packets_lock) {
                packets.add(pkt);
                packets_lock.notify();
              }
              break;
            case CMD_CLOSE:
              return;
            default:
              JFLog.log("TCPTransport:ClientWorker:Unknown cmd:" + cmd);
              return;
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
      if (debug) JFLog.log("ClientWorker.done");
    }
    public boolean disconnect_client() {
      if (debug) JFLog.log("TCPTransport.disconnect_client()");
      if (client != null) {
        try {client.close();} catch (Exception e) {}
        client = null;
      }
      return true;
    }

    private boolean write_name() {
      byte[] pkt = new byte[8 + busName.length()];
      int strlen = busName.length();
      BE.setuint32(pkt, 0, CMD_NAME);
      BE.setuint32(pkt, 4, strlen);
      BE.setString(pkt, 8, strlen, busName);
      return write_data(pkt);
    }

    private boolean write_data(byte[] data) {
      if (debug) JFLog.log("write_data():" + data.length);
      try {
        os.write(data);
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }

    private boolean write_packet_to_server(String name, byte[] data, int offset, int length) {
      int strlen = name.length();
      byte[] pkt = new byte[12 + strlen + length];
      BE.setuint32(pkt, 0, CMD_PACKET_TO_SERVER);
      BE.setuint32(pkt, 4, 4 + strlen + length);
      BE.setuint32(pkt, 8, strlen);
      System.arraycopy(name.getBytes(), 0, pkt, 12, strlen);
      int pkt_dest_offset = 12 + strlen;
      //remainder = data_length
      System.arraycopy(data, offset, pkt, pkt_dest_offset, length);
      return write_data(pkt);
    }

    private boolean write_close() {
      byte[] pkt = new byte[8];
      BE.setuint32(pkt, 0, CMD_CLOSE);
      BE.setuint32(pkt, 4, 0);
      return write_data(pkt);
    }
  }
}
