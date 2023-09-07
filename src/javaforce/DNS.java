package javaforce;

/** DNS Client
 *
 * Transports Supported : UDP, DNS over HTTPS (DOH)
 * Records Supported : A,AAAA,CNAME,MX,NS,SOA,SRV,NAPTR
 *
 * TODO : Fix DNS over TLS (DOT)
 *
 * @author pquiring
 */

import java.net.*;
import java.io.*;
import java.util.*;

public class DNS {
  private InetSocketAddress server;
  private int transport;

  public static final int TRANSPORT_UDP = 0;  //DNS (UDP:53)
  public static final int TRANSPORT_DOH = 1;  //DNS over HTTPS (TCP:443)
  public static final int TRANSPORT_DOT = 2;  //DNS over TLS (TCP:853)

  public DNS(String server) {
    this.server = new InetSocketAddress(server, 53);
    this.transport = TRANSPORT_UDP;
  }
  public DNS(String server, int port) {
    this.server = new InetSocketAddress(server, port);
    this.transport = TRANSPORT_UDP;
  }
  public DNS(int transport, String server) {
    this.server = new InetSocketAddress(server, getPort(transport));
    this.transport = transport;
  }
  public DNS(int transport, String server, int port) {
    this.server = new InetSocketAddress(server, port);
    this.transport = transport;
  }

  private int getPort(int transport) {
    int port = 53;
    switch (transport) {
      case TRANSPORT_UDP: port = 53; break;
      case TRANSPORT_DOH: port = 443; break;
      case TRANSPORT_DOT: port = 853; break;
    }
    return port;
  }

  //flags
  private static final int REPLY = 0x8000;  //1=response (0=query)
  //4 bits opcode
  private static final int OPCODE_MASK = 0x7800;
  private static final int OPCODE_QUERY = 0x0000;
  private static final int OPCODE_IQUERY = 0x4000;  //(8)
  private static final int OPCODE_UPDATE = 0x2800;  //(5)
  private static final int AA = 0x0400;  //auth answer
  private static final int TC = 0x0200;  //truncated (if packet > 512 bytes)
  private static final int RD = 0x0100;  //recursive desired
  private static final int RA = 0x0080;  //recursive available
  private static final int Z =  0x0040;  //???
  private static final int AD = 0x0020;  //auth data???
  private static final int CD = 0x0010;  //allow un-auth data???
  //4 bits result code (0=no error)
  private static final int ERR_NO_ERROR     = 0x0000;  //no error
  private static final int ERR_NO_SUCH_NAME = 0x0003;  //404

  //DNS record types : see https://en.wikipedia.org/wiki/List_of_DNS_record_types
  public static final int TYPE_A = 1;  //IP4
  public static final int TYPE_NS = 2;  //name server
  public static final int TYPE_CNAME = 5;  //canonical name
  public static final int TYPE_SOA = 6;  //start of auth
  public static final int TYPE_PTR = 12;  //pointer
  public static final int TYPE_MX = 15;  //mail exchange
  public static final int TYPE_TXT = 16;  //text
  public static final int TYPE_AAAA = 28;  //IP6
  public static final int TYPE_LOC = 29;  //location
  public static final int TYPE_SRV = 33;  //service
  public static final int TYPE_NAPTR = 35;  //naming auth pointer
  public static final int TYPE_ANY = 255;  //ANY and ALL

  private static class Packet {
    public Packet(byte[] data) {
      this.data = data;
    }
    public Packet(byte[] data, int length) {
      if (data.length != length) {
        this.data = Arrays.copyOf(data, length);
      } else {
        this.data = data;
      }
    }
    private byte[] data;
    private int offset;

    //buffer
    public int getLength() {
      return data.length;
    }
    public byte[] getData() {
      return data;
    }
    public int getSize() {
      return offset;
    }
    public int getOffset() {
      return offset;
    }

    //readers
    public int readByte() throws Exception {
      int value = data[offset++];
      return value;
    }
    public int readShort() throws Exception {
      int value = BE.getuint16(data, offset);
      offset += 2;
      return value;
    }
    public int readInt() throws Exception {
      int value = BE.getuint32(data, offset);
      offset += 4;
      return value;
    }
    public String readName() {
      StringBuilder str = new StringBuilder();
      int ptr = offset;
      boolean compressed = false;
      do {
        int len = data[ptr++] & 0xff;
        if (!compressed) {
          offset++;
        }
        if (len == 0) break;
        if (len >= 0xc0) {
          //pointer
            if (!compressed) {
            offset++;
          }
          compressed = true;
          ptr = BE.getuint16(data, ptr - 1) & 0x3fff;
          continue;
        }
        String p = new String(data, ptr, len);
        if (str.length() > 0) {
          str.append('.');
        }
        str.append(p);
        ptr += len;
        if (!compressed) {
          offset += len;
        }
      } while (true);
      return str.toString();
    }
    public byte[] readBytes(int len) {
      byte[] buf = new byte[len];
      System.arraycopy(data, offset, buf, 0, len);
      offset += len;
      return buf;
    }

    //writers
    public void writeShort(int value) {
      BE.setuint16(data, offset, value);
      offset += 2;
    }
    public void writeName(String name) {
      String[] ps = name.split("[.]");
      for(int i=0;i<ps.length;i++) {
        String p = ps[i];
        int len = p.length();
        data[offset++] = (byte)len;
        System.arraycopy(p.getBytes(), 0, data, offset, len);
        offset += len;
      }
      data[offset++] = 0;  //zero term string
    }
  }

  public String[] resolve(int type, String name) {
    try {
      Packet request = new Packet(new byte[1500]);
      //header
      int id = new Random().nextInt(0xffff);
      request.writeShort(id);
      int flgs = (RD | CD);
      request.writeShort(flgs);
      request.writeShort(1);  //query count
      request.writeShort(0);  //answer count
      request.writeShort(0);  //auth server count
      request.writeShort(0);  //additional count
      //query
      request.writeName(name);
      request.writeShort(type);  //type
      request.writeShort(1);  //class (1=internet)
      Packet reply;
      switch (transport) {
        case TRANSPORT_UDP: reply = transportUDP(request); break;
        case TRANSPORT_DOH: reply = transportDOH(request); break;
        case TRANSPORT_DOT: reply = transportDOT(request); break;
        default: return null;
      }
      if (reply == null) return null;
      //decode reply
      if (reply.getLength() < 12) {
        throw new Exception("DNS:Reply too short");
      }

      int rid = reply.readShort();
      if (rid != id) {
        throw new Exception(String.format("DNS:Response id(0x%x) != Request id(0x%x)", rid, id));
      }
      int rflgs = reply.readShort();
      if ((rflgs & REPLY) == 0) {
        throw new Exception("DNS:Response is not a reply");
      }
      int rqueries = reply.readShort();
      int ranswers = reply.readShort();
      int rauth = reply.readShort();
      int radditional = reply.readShort();
      String[] queries = new String[rqueries];
      for(int i=0;i<rqueries;i++) {
        queries[i] = reply.readName();
        int qtype = reply.readShort();
        int qclass = reply.readShort();
      }
      int totalres = ranswers + rauth + radditional;
      String[] resname = new String[totalres];
      String[] results = new String[totalres];
      for(int i=0;i<totalres;i++) {
        resname[i] = reply.readName();
        int qtype = reply.readShort();
        int qclass = reply.readShort();
        int qttl = reply.readInt();
        int qdatalen = reply.readShort();
        int nextOffset = reply.getOffset() + qdatalen;
        switch (qtype) {
          case TYPE_A: {
            if (qdatalen != 4) throw new Exception("invalid A data");
            //IP4 = d.d.d.d
            byte[] oct4x8 = reply.readBytes(qdatalen);
            results[i] = String.format("%d.%d.%d.%d",
              oct4x8[0] & 0xff,
              oct4x8[1] & 0xff,
              oct4x8[2] & 0xff,
              oct4x8[3] & 0xff);
            break;
          }
          case TYPE_AAAA: {
            if (qdatalen != 16) throw new Exception("invalid AAAA data");
            //IP6 = x:x:x:x:x:x:x:x
            byte[] oct16x8 = reply.readBytes(qdatalen);
            short[] oct8x16 = new short[8];
            for(int o=0;o<8;o++) {
              oct8x16[o] = (short)BE.getuint16(oct16x8, o * 2);
            }
            results[i] = String.format("%04x:%04x:%04x:%04x:%04x:%04x:%04x:%04x",
              oct8x16[0] & 0xffff,
              oct8x16[1] & 0xffff,
              oct8x16[2] & 0xffff,
              oct8x16[3] & 0xffff,
              oct8x16[4] & 0xffff,
              oct8x16[5] & 0xffff,
              oct8x16[6] & 0xffff,
              oct8x16[7] & 0xffff);
            break;
          }
          case TYPE_CNAME: {
            results[i] = reply.readName();
            break;
          }
          case TYPE_SOA: {
            String pri_ns = reply.readName();
            String mailbox = reply.readName();
            int serial = reply.readInt();
            int refresh_interval = reply.readInt();
            int retry_interval = reply.readInt();
            int expire_limit = reply.readInt();
            int min_ttl = reply.readInt();
            results[i] = pri_ns;
            break;
          }
          case TYPE_NS: {
            results[i] = reply.readName();
            break;
          }
          case TYPE_MX: {
            int pri = reply.readShort();
            String mx = reply.readName();
            results[i] = mx + ":" + pri;
            break;
          }
          case TYPE_SRV: {
            int pri = reply.readShort();
            int weight = reply.readShort();
            int port = reply.readShort();
            String target = reply.readName();
            results[i] = target + ":" + port;
            break;
          }
          case TYPE_NAPTR: {
            int order = reply.readShort();
            int pref = reply.readShort();
            int flag_len = reply.readByte();
            byte[] flags = reply.readBytes(flag_len);
            int svc_len = reply.readByte();
            byte[] service = reply.readBytes(svc_len);
            int regex_len = reply.readByte();
            byte[] regex = reply.readBytes(regex_len);
            String replacement = reply.readName();
            results[i] = replacement;
            break;
          }
          default: {
            results[i] = "unknown type:" + qtype;
            break;
          }
        }
        if (reply.getOffset() != nextOffset) {
          throw new Exception("Invalid DNS record");
        }
      }
      return results;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private Packet transportUDP(Packet request) {
    try {
      DatagramSocket socket = new DatagramSocket();
      DatagramPacket udprequest = new DatagramPacket(request.getData(), request.getSize(), server);
      socket.send(udprequest);
      DatagramPacket reply = new DatagramPacket(new byte[1500], 1500);
      socket.receive(reply);
      return new Packet(reply.getData(), reply.getLength());
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private Packet transportDOH(Packet request) {
    //https://en.wikipedia.org/wiki/DNS_over_HTTPS
    try {
      HTTPS https = new HTTPS();
      https.open(server.getHostString(), server.getPort());
      byte[] reply = https.post("/dns-query", Arrays.copyOf(request.getData(), request.getSize()), "application/dns-message");
      return new Packet(reply);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private Packet transportDOT(Packet request) {
    //https://en.wikipedia.org/wiki/DNS_over_TLS
    JFLog.log("DNS over TLS is not working yet!");
    try {
      JF.initHttps();
      Socket socket = JF.connectSSL(server.getHostString(), server.getPort());
      OutputStream os = socket.getOutputStream();
      InputStream is = socket.getInputStream();
      os.write(request.getData(), 0, request.getSize());
      byte[] buf = new byte[1500];
      int total = 0;
      while (total == 0) {
        int read = is.read(buf, total, buf.length - total);
        if (read == -1) {
          break;
        }
        JFLog.log("read=" + read);
        if (read > 0) {
          total += read;
        }
      }
      socket.close();
      if (total == 0) {
        throw new Exception("null reply");
      }
      return new Packet(buf, total);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private static void test(DNS dns, int type, String domain, boolean recursive) {
    JFLog.log("Request=" + domain);
    String[] reply = dns.resolve(type, domain);
    if (reply == null) {
      JFLog.log("  Reply == null");
    } else {
      for(int i=0;i<reply.length;i++) {
        JFLog.log("  Reply[]=" + reply[i]);
      }
    }
  }

  public static void main(String[] args) {
    DNS dns = new DNS(TRANSPORT_DOH, "8.8.8.8");
    test(dns, TYPE_A, "google.com", true);
    test(dns, TYPE_AAAA, "google.com", true);
    test(dns, TYPE_CNAME, "google.com", true);
    test(dns, TYPE_MX, "gmail.com", true);
    test(dns, TYPE_NS, "google.com", true);
    test(dns, TYPE_SRV, "_ldap._tcp.google.com", true);
  }
}
