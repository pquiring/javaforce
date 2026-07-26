package javaforce.rdp;

import java.net.*;
import java.io.*;

import javaforce.*;
import javaforce.net.*;

/** RDP Client (WIP)
 *
 * See : https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpbcgr
 *
 * @author pquiring
 */

public class RDP {

  public static boolean debug = true;

  private Socket socket;
  private InputStream is;
  private OutputStream os;

  private static final byte[] MCS_TYPE_CONNECTINITIAL = new byte[] {(byte)0x7f, 0x65};
  private static final byte[] str_one = new byte[] {0x01};

  private static class Connect implements SubPacket {

    //note:protocols is written in reverse endian
    public static final int PROTO_TLS = 0x01000000;
    public static final int PROTO_CREDSSP = 0x02000000;

    public Connect() {}
    public Connect(String user) {
      cookie = ("Cookie: mstshash=" + user + "\r\n").getBytes();
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

  public boolean connect(String client, String server, String user, String passwd, int width, int height) {
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
        //write X.224 Connection Request
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
        //read X.224 Connection Confirm
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
      {
        //write MCS Connect Initial PDU with GCC Conference Create Request
        if (debug) JFLog.log("RDP:sending connection request(2)");
        Packet packet = new Packet();
        TPKT tpkt = new TPKT();
        COTP cotp = new COTP();
        BER ber = new BER();
        cotp.create(COTP.TYPE_DATA, (byte)2);
        cotp.pdata = new byte[] {(byte)0x80};  //EOT
        ber.setType(MCS_TYPE_CONNECTINITIAL);
        ber.appendString(str_one);  //callingDomainSelector
        ber.appendString(str_one);  //calledDomainSelector
        ber.appendBoolean(true);  //upwardFlag
        ber.appendSequence(0x19);  //targetParameters
          ber.appendInteger(0x22);  //maxChannelIds
          ber.appendInteger(0x02);  //maxUserIds
          ber.appendInteger(0x00);  //maxTokenIds
          ber.appendInteger(0x01);  //numPriorities
          ber.appendInteger(0x00);  //minThroughput
          ber.appendInteger(0x01);  //maxHeight
          ber.appendInteger(0xffff);  //maxMCSPDUsize
          ber.appendInteger(0x02);  //protocolVer
        ber.appendSequence(0x19);  //minimumParameters
          ber.appendInteger(0x01);  //maxChannelIds
          ber.appendInteger(0x01);  //maxUserIds
          ber.appendInteger(0x01);  //maxTokenIds
          ber.appendInteger(0x01);  //numPriorities
          ber.appendInteger(0x00);  //minThroughput
          ber.appendInteger(0x01);  //maxHeight
          ber.appendInteger(0x0420);  //maxMCSPDUsize
          ber.appendInteger(0x02);  //protocolVer
        ber.appendSequence(0x1c);  //maximumParameters
          ber.appendInteger(0xffff);  //maxChannelIds
          ber.appendInteger(0xfc17);  //maxUserIds
          ber.appendInteger(0xffff);  //maxTokenIds
          ber.appendInteger(0x01);  //numPriorities
          ber.appendInteger(0x00);  //minThroughput
          ber.appendInteger(0x01);  //maxHeight
          ber.appendInteger(0xffff);  //maxMCSPDUsize
          ber.appendInteger(0x02);  //protocolVer
        PER per = new PER();
          //TODO : clean up to real objects
          per.setEndian(Endian.L);
          per.append(new byte[] {0x00, 0x05});  //CHOICE : 5 bytes
          per.append(new byte[] {0x00, 0x14, 0x7c, 0x00, 0x01});  //object
          per.append(new byte[] {(byte)0x81, 0x2a});  //connectPDU length (298)
          per.append(new byte[] {0x00, 0x08, 0x00, 0x10, 0x00, 0x01, (byte)0xc0, 0x00, 0x44, 0x75, 0x63, 0x61, (byte)0x81, 0x1c});  //lot o bits
          per.append(new byte[] {0x01, (byte)0xc0, (byte)0xd8, 0x00});  //TS_UD_HEADER :: type = CS_CORE (0xc001), length = 216 bytes
          per.append(new byte[] {0x04, 0x00, 0x08, 0x00});  //version = 0x00080004
          per.appendShort((short)1920);  //width (0x780)
          per.appendShort((short)1080);  //height (0x438)
          per.appendShort((short)0xca01);  //colorDepth = RNS_UD_COLOR_8BPP
          per.appendShort((short)0xaa03);  //colorDepth = RNS_UD_COLOR_8BPP
          per.appendInt(0x409);  //keyboardLayout = 0x409 = 1033 = English (US)
          per.appendInt(0xece);  //clientBuild = 3790
          per.appendString16(client.getBytes(), 16);  //client name (15 chars name + 1 NULL in utf16 LE)
          per.appendInt(0x04);  //keyboardType
          per.appendInt(0x00);  //keyboardSubType
          per.appendInt(0x0c);  //keyboardFuncKey
          per.append(new byte[16 * 4]);  //filename
          per.appendShort((short)0xca01);  //postbeta2 colorDepth = RNS_UD_COLOR_8BPP
          per.appendShort((short)0x01);  //clientProdID
          per.appendInt(0x00);  //serial #
          per.appendShort((short)0x18);  //32BPP
          per.appendShort((short)0x07);  //support color depths (0x01=24 0x02=16 0x04=15)
          per.appendShort((short)0x01);  //earlyCapabilityFlags
          per.appendString16("69712-783-0357974-42714".getBytes(), 32);  //clientDigProductId
          per.appendByte((byte)0x00);  //connection type (ignored)
          per.appendByte((byte)0x00);  //padding
          per.appendInt(0x00);  //serverSelectedProtocol
          per.appendInt(0x0cc004);  //type = CS_CLUSTER (0xc004), length = 12 bytes
          per.appendInt(0x0d);  //flags = 0x0d
          per.appendInt(0x00);  //RedirectedSessionID
          per.appendInt(0x0cc002);  //type = SECURITY (0xc002), length = 12 bytes
          per.appendInt(0x1b);  //encryptionMethods (4 selected)
          per.appendInt(0x00);  //extEncryptionMethods
          per.appendInt(0x2cc003);  //type = CS_NET (0xc003), length = 44 bytes
          per.appendInt(0x03);  //channelCount = 3

          per.append(new byte[] {0x72, 0x64, 0x70, 0x64, 0x72, 0x00, 0x00, 0x00});  //rdpdr
          per.appendInt(0x80800000);  //options
          per.append(new byte[] {0x63, 0x6c, 0x69, 0x70, 0x72, 0x64, 0x72, 0x00});  //cliprdr
          per.appendInt(0xc0a00000);  //options
          per.append(new byte[] {0x72, 0x64, 0x70, 0x73, 0x6e, 0x64, 0x00, 0x00});  //rdpsnd
          per.appendInt(0xc0000000);  //options

        ber.appendString(per.toByteArray());
        tpkt.create((short)(tpkt.getSize() + cotp.getSize() + ber.getSize()));
        tpkt.write(packet);
        cotp.write(packet);
        ber.write(packet);
        packet.toFile("test-rdp.dat");
        os.write(packet.toByteArray());
      }
      {
        //read X.224 Connection Confirm
        if (debug) JFLog.log("RDP:reading connection reply(2)");
        Packet packet = new Packet();
        packet.resetWrite();
        int read = is.read(packet.data);
        if (read == -1) throw new Exception("RDP:read error");
        packet.length = read;
        if (read <= 0) {
          throw new Exception("RDP:Connection rejected");
        } else {
          JFLog.log("RDP Accepted");
        }
      }

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
      rdp.connect(args[0], args[1], args[2], args[3], 1920, 1080);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
