/**
 * Basic DNS Client
 *
 * @author pquiring
 *
 * Created : Nov 17, 2013
 */

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

public class DNS extends Thread {
  private NetApp win;
  private DatagramSocket ds;
  private static int maxmtu = 512;  //standard
  private String server, query;
  private int type;
  private short id;

  public DNS(NetApp win, String server, String query, String type) {
    this.win = win;
    this.server = server;
    this.query = query;
    this.type = decodeType(type);
  }

  public void run() {
    if (false) {
      runJava();
    } else {
      runDNS();
    }
  }

  private void runJava() {
    try {
      win.setDNSStatus("Resolving host...");
      InetAddress ia = InetAddress.getByName(query);
      win.setDNSIP(ia.getHostAddress().toString());
    } catch (Exception e) {
      e.printStackTrace();
      win.setDNSStatus(e.toString());
    }
  }

  private void runDNS() {
    try {
      ds = new DatagramSocket();  //bind to next available port
      id = (short)(new Random().nextInt() & 0x7fff);
      //build request
      buildRequest(query, type, id);

      //send request
      DatagramPacket out = new DatagramPacket(data, dataOffset);
      out.setAddress(InetAddress.getByName(server));
      out.setPort(53);
      ds.send(out);

      //wait for reply
      data = new byte[maxmtu];
      DatagramPacket in = new DatagramPacket(data, data.length);
      ds.setSoTimeout(2500);
      ds.receive(in);

      ds.close();
      ds = null;

      //process reply
      processReply(in.getData(), in.getLength());
    } catch (Exception e) {
      e.printStackTrace();
      win.setDNSStatus(e.toString());
    }
    if (ds != null) {
      ds.close();
      ds = null;
    }
  }

  public void close() {
    if (ds != null) {
      ds.close();
      ds = null;
    }
  }

  //flags
  private static final int QR = 0x8000;  //1=response (0=query)
  //4 bits opcode
  private static final int OPCODE_QUERY = 0x0000;
  private static final int OPCODE_IQUERY = 0x4000;
  private static final int AA = 0x0400;  //auth answer
  private static final int TC = 0x0200;  //truncated (if packet > 512 bytes)
  private static final int RD = 0x0100;  //recursive desired
  private static final int RA = 0x0080;  //recursive available
  private static final int Z =  0x0040;  //???
  private static final int AD = 0x0020;  //auth data???
  private static final int CD = 0x0010;  //checking disabled???
  //4 bits result code (0=no error)

  private static final int A = 1;
  private static final int CNAME = 5;
  private static final int MX = 15;
  private static final int AAAA = 28;

  private byte data[];
  private int dataOffset;
  private ByteBuffer buffer;
  private int nameLength;  //bytes used decoding name


  private String getName(byte data[], int offset) {
    StringBuilder name = new StringBuilder();
    boolean jump = false;
    nameLength = 0;
    do {
      if (!jump) nameLength++;
      int length = data[offset++] & 0xff;
      if (length == 0) break;
      if (length >= 0xc0) {
        //pointer
        if (!jump) nameLength++;
        jump = true;
        int newOffset = (length & 0x3f) << 8;
        newOffset += data[offset] & 0xff;
        offset = newOffset;
      } else {
        if (!jump) nameLength += length;
        if (name.length() != 0) name.append(".");
        name.append(new String(data, offset, length));
        offset += length;
      }
    } while (true);
    return name.toString();
  }

  private int encodeName(String domain) {
    //TODO : compression
    String p[] = domain.split("[.]");
    int length = 0;
    int strlen;
    for(int a=0;a<p.length;a++) {
      strlen = p[a].length();
      data[dataOffset++] = (byte)strlen;
      length++;
      System.arraycopy(p[a].getBytes(), 0, data, dataOffset, strlen);
      dataOffset += strlen;
      length += strlen;
    }
    //zero length part ends string
    data[dataOffset++] = 0;
    length++;
    return length;
  }

  private void buildRequest(String query, int type, int id) {
    data = new byte[maxmtu];
    buffer = ByteBuffer.wrap(data);
    buffer.order(ByteOrder.BIG_ENDIAN);
    dataOffset = 0;
    putShort((short)id);
    putShort((short)(RD));  //flags
    putShort((short)1);  //questions
    putShort((short)0);  //answers
    putShort((short)0);  //name servers
    putShort((short)0);  //additionals
    encodeName(query);
    putShort((short)type);
    putShort((short)1);  //class
    switch(type) {
      case A:
        encodeName(query);
        putShort((short)type);
        putShort((short)1);  //class
        break;
      case CNAME:
        encodeName(query);
        putShort((short)type);
        putShort((short)1);  //class
        break;
      case MX:
        encodeName(query);
        putShort((short)type);
        putShort((short)1);  //class
        break;
      case AAAA:
        encodeName(query);
        putShort((short)type);
        putShort((short)1);  //class
        break;
    }
  }

  private int decodeType(String type) {
    if (type.equals("A")) return A;
    if (type.equals("AAAA")) return AAAA;
    if (type.equals("MX")) return MX;
    return -1;
  }

  private String typeToString(int type) {
    switch (type) {
      case MX: return "MX";
      case A: return "A";
      case AAAA: return "AAAA";
      case CNAME: return "CNAME";
      default: return "?" + type;
    }
  }

  private String decodeData(byte data[], int offset, int type) {
    StringBuilder sb = new StringBuilder();
    switch (type) {
      case A:
        sb.append("IP4=");
        for(int a=0;a<4;a++) {
          if (a > 0) sb.append('.');
          sb.append(Integer.toString(data[offset+a] & 0xff));
        }
        break;
      case CNAME:
        sb.append("CNAME=");
        sb.append(getName(data, offset));
        break;
      case MX:
        sb.append("MX=");
        int pref = data[offset++] & 0xff;
        pref <<= 8;
        pref += data[offset++] & 0xff;
        sb.append(getName(data, offset));
        sb.append(":pref=" + pref);
        break;
      case AAAA:
        sb.append("IP6=");
        for(int a=0;a<8;a++) {
          if (a > 0) sb.append(':');
          int o = data[offset+a*2] & 0xff;
          o <<= 8;
          o += data[offset+a*2+1] & 0xff;
          sb.append(String.format("%04x", o));
        }
        break;
      default:
        sb.append("unsupported type:" + type);
        break;
    }
    return sb.toString();
  }

  private void processReply(byte data[], int dataLength) throws Exception {
    ByteBuffer bb = ByteBuffer.wrap(data);
    bb.order(ByteOrder.BIG_ENDIAN);
    short id = bb.getShort(0);
    if (id != this.id) throw new Exception("id does not match:" + Integer.toString(id, 16) + "!=" + Integer.toString(this.id, 16));
    short flgs = bb.getShort(2);
    if ((flgs & QR) == 0) {
      throw new Exception("not a response");
    }
    short cntQ = bb.getShort(4);
    if (cntQ != 1) throw new Exception("only 1 question supported");
    short cntA = bb.getShort(6);
//      if (cndA != 0) throw new Exception("query with answers?");
    short cntS = bb.getShort(8);
//      if (cndS != 0) throw new Exception("query with auth names?");
    short cntAdd = bb.getShort(10);
//      if (cndAdd != 0) throw new Exception("query with adds?");
    int offset = 12;
    StringBuilder sb = new StringBuilder();
    //questions
    for(int a=0;a < cntQ; a++) {
      String query = getName(data, offset);
      offset += nameLength;
      int type = bb.getShort(offset);
      offset += 2;
      int cls = bb.getShort(offset);
      if (cls != 1) throw new Exception("only internet class supported:" + cls);
      offset += 2;
      sb.append("Query:" + query + ":" + typeToString(type) + "\n");
    }
    //anwsers
    for(int a=0;a < cntA; a++) {
      String query = getName(data, offset);
      offset += nameLength;
      int type = bb.getShort(offset);
      offset += 2;
      int cls = bb.getShort(offset);
      if (cls != 1) throw new Exception("only internet class supported:" + cls);
      offset += 2;
      int ttl = bb.getInt(offset);
      offset += 4;
      short rdataLength = bb.getShort(offset);
      offset += 2;
      sb.append("Anwser:" + query + ":" + typeToString(type) + ":TTL=" + ttl + ":" + decodeData(data, offset, type) + "\n");
      offset += rdataLength;
    }
    //auth server
    for(int a=0;a < cntS; a++) {
      String query = getName(data, offset);
      offset += nameLength;
      int type = bb.getShort(offset);
      offset += 2;
      int cls = bb.getShort(offset);
      if (cls != 1) throw new Exception("only internet class supported:" + cls);
      offset += 2;
      int ttl = bb.getInt(offset);
      offset += 4;
      short rdataLength = bb.getShort(offset);
      offset += 2;
      sb.append("AuthServer:" + query + ":" + typeToString(type) + ":TTL=" + ttl + ":" + decodeData(data, offset, type) + "\n");
      offset += rdataLength;
    }
    //additional
    for(int a=0;a < cntAdd; a++) {
      String query = getName(data, offset);
      offset += nameLength;
      int type = bb.getShort(offset);
      offset += 2;
      int cls = bb.getShort(offset);
      if (cls != 1) throw new Exception("only internet class supported:" + cls);
      offset += 2;
      int ttl = bb.getInt(offset);
      offset += 4;
      short rdataLength = bb.getShort(offset);
      offset += 2;
      sb.append("Additional:" + query + ":" + typeToString(type) + ":TTL=" + ttl + ":" + decodeData(data, offset, type) + "\n");
      offset += rdataLength;
    }
    win.setDNSIP(sb.toString());
  }

  private void putIP4(String ip) {
    String p[] = ip.split("[.]");
    for(int a=0;a<4;a++) {
      data[dataOffset++] = (byte)(int)Integer.valueOf(p[a]);
    }
  }

  private void putIP6(String ip) {
    String p[] = ip.split(":");
    for(int a=0;a<8;a++) {
      putShort((short)(int)Integer.valueOf(p[a], 16));
    }
  }

  private void putShort(short value) {
    buffer.putShort(dataOffset, value);
    dataOffset += 2;
  }

  private void putInt(int value) {
    buffer.putInt(dataOffset, value);
    dataOffset += 4;
  }
}

/*
struct Packet {
  short id;
  short flgs;
  short queries_cnt;
  short anwsers_cnt;
  short auth_servers_cnt;
  short additional_cnt;
  Queries[];
  AnwserResources[];
  AuthServerResources[];
  AdditionalResources[];
};
struct Query {
  DNSName query;
  short type;
  short cls;
};
struct Resource {
  DNSName name;
  short type;
  short cls;
  int ttl;
  short rdataLength;
  byte rdata[rdataLength];
};
DNS Names compression:
  [3]www[6]google[3]com[0]
  Any length that starts with 11 binary is a 2 byte (14bits) pointer from the first byte of the packet.
  [4]mail[pointer:33]  -> assuming 33 points to [6]google[3]com[0] this would => mail.google.com
Types: 1=IP4 5=CNAME 15=MX 28=IP6 etc.
Cls : 1=Internet
*/
