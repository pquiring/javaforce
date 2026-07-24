package javaforce.rdp;

import java.net.*;
import java.io.*;

import javaforce.*;
import javaforce.net.*;

/** RDP Client (WIP)
 *
 * @author pquiring
 */

public class RDP {

  public static boolean debug = true;

  private Socket socket;
  private InputStream is;
  private OutputStream os;

  private static class Connect implements SubPacket {

    //note:protocols is written in reverse endian
    public static final int PROTO_TLS = 0x01000000;
    public static final int PROTO_CREDSSP = 0x02000000;

    public Connect() {}
    public Connect(String user) {
      cookie = ("Cookie:mstshash=" + user + "\r\n").getBytes();
    }
    byte[] cookie;
    byte cmd;  //0x01 = request, 0x02 = reply
    byte flags;  //reserved
    byte length;  //always 8 (cmd to end)
    byte padding;
    int protocols;  //bit-wise fields

    public int getSize() {
      if (cookie == null) return 8;
      return cookie.length + 8;
    }

    public int getDataSize() {
      return -1;
    }

    public void read(Packet packet) throws Exception {
      //TODO : read string\r\n
      cmd = packet.readByte();
      flags = packet.readByte();
      length = packet.readByte();
      padding = packet.readByte();
      protocols = packet.readInt();
    }

    public void write(Packet packet) throws Exception {
      if (cookie != null) {
        packet.write(cookie);
      }
      packet.writeByte(cmd);
      packet.writeByte(flags);
      packet.writeByte(length);
      packet.writeByte(padding);
      packet.writeInt(protocols);
    }

    public void create() {
      cmd = 0x01;
      length = 8;
      protocols = PROTO_CREDSSP + PROTO_TLS;
    }
  }

  public boolean connect(String server, String user, String passwd, int width, int height) {
    int port = 3389;
    try {
      int idx = server.indexOf(":");
      if (idx != -1) {
        port = Integer.valueOf(server.substring(idx + 1));
        server = server.substring(0, idx);
      }
      if (debug) JFLog.log("RDP:connect:" + server + ":" + port);
      socket = new Socket(server, port);
      is = socket.getInputStream();
      os = socket.getOutputStream();
      {
        //send request
        if (debug) JFLog.log("RDP:sending connection request");
        Packet packet = new Packet();
        TPKT tpkt = new TPKT();
        COTP cotp = new COTP();
        Connect connect = new Connect(user);
        connect.create();
        cotp.create(COTP.TYPE_CONNECT, (byte)(1 + 5 + connect.getSize()));
        cotp.pdata = new byte[5];  //padding???
        tpkt.create((byte)(4 + cotp.getSize()));
        tpkt.write(packet);
        cotp.write(packet);
        connect.write(packet);
        os.write(packet.toByteArray());
      }
      {
        //read reply
        if (debug) JFLog.log("RDP:reading connection reply");
        Packet packet = new Packet();
        packet.resetWrite();
        int read = is.read(packet.data);
        if (read == -1) throw new Exception("RDP:read error");
        packet.length = read;
        TPKT tpkt = new TPKT();
        COTP cotp = new COTP();
        Connect connect = new Connect();
        tpkt.read(packet);
        cotp.read(packet);
        connect.read(cotp.getDataAsPacket());
        if (cotp.getPDUType() != COTP.TYPE_CONNECT_ACK) {
          throw new Exception("RDP:Connection rejected");
        } else {
          JFLog.log("RDP Accepted");
        }
      }
      socket = JF.connectSSL(socket, KeyMgmt.getDefaultClient());
      if (socket == null) {
        throw new Exception("RDP:TLS connection failed");
      }
      is = socket.getInputStream();
      os = socket.getOutputStream();

      socket.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static void main(String[] args) {
    try {
      RDP rdp = new RDP();
      rdp.connect(args[0], args[1], args[2], 1920, 1080);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
