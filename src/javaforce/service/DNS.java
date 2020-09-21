package javaforce.service;

/**
 * Basic DNS Server
 *
 * Supports : A,CNAME,MX,AAAA
 *
 * Note : IP6 must be in full notation : xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx
 *
 * @author pquiring
 *
 * Created : Nov 17, 2013
 */

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

public class DNS extends Thread {
  public final static String busPack = "net.sf.jfdns";

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfdns.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfdns.log";
  }

  private DatagramSocket ds;
  private static int maxmtu = 512;  //standard
  private ArrayList<String> uplink = new ArrayList<String>();
  private ArrayList<String> records = new ArrayList<String>();
  private ArrayList<String> allows = new ArrayList<String>();
  private ArrayList<String> denies = new ArrayList<String>();
  private int uplinktimeout = 2000;

  public void run() {
    JFLog.append(getLogFile(), false);
    JFLog.setRetention(30);
    try {
      loadConfig();
      busClient = new JBusClient(busPack, new JBusMethods());
      busClient.setPort(getBusPort());
      busClient.start();
      for(int a=0;a<5;a++) {
        try {
          ds = new DatagramSocket(53);
        } catch (Exception e) {
          if (a == 4) {
            JFLog.log(e);
            return;
          }
          JF.sleep(1000);
          continue;
        }
        break;
      }
      while (true) {
        byte[] data = new byte[maxmtu];
        DatagramPacket packet = new DatagramPacket(data, maxmtu);
        ds.receive(packet);
        new Worker(packet).start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void close() {
    try {
      ds.close();
    } catch (Exception e) {}
  }

  enum Section {None, Global, Records};

  private final static String defaultConfig
    = "[global]\n"
    + "uplink=8.8.8.8\n"
    + "uplink=8.8.4.4\n"
    + "uplinktimeout=1000\n"
    + "[records]\n"
    + "#name,type,ttl,value\n"
    + "#mydomain.com,cname,3600,www.mydomain.com\n"
    + "#www.mydomain.com,a,3600,192.168.0.2\n"
    + "#mydomain.com,mx,3600,50,mail.mydomain.com\n"
    + "#mail.mydomain.com,a,3600,192.168.0.3\n"
    + "#www.mydomain.com,aaaa,3600,1234:1234:1234:1234:1234:1234:1234:1234\n";

  private void loadConfig() {
    JFLog.log("loadConfig");
    Section section = Section.None;
    try {
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      StringBuilder cfg = new StringBuilder();
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim().toLowerCase();
        int idx = ln.indexOf('#');
        if (idx != -1) ln = ln.substring(0, idx).trim();
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        if (ln.equals("[records]")) {
          section = Section.Records;
          continue;
        }
        switch (section) {
          case Global:
            if (ln.startsWith("uplink=")) {
              uplink.add(ln.substring(7));
            }
            if (ln.startsWith("uplinktimeout=")) {
              uplinktimeout = JF.atoi(ln.substring(14));
            }
            if (ln.startsWith("allow=")) {
              String dns = ln.substring(6);
              if (!allows.contains(dns)) {
                allows.add(dns);
              }
            }
            if (ln.startsWith("deny=")) {
              denies.add(ln.substring(5));
            }
            break;
          case Records:
            records.add(ln);
            break;
        }
      }
      br.close();
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
      JFLog.log("config not found, creating defaults.");
      uplink.add("8.8.8.8");
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(defaultConfig.getBytes());
        fos.close();
        config = defaultConfig;
      } catch (Exception e2) {
        JFLog.log(e2);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //flags
  private static final int QR = 0x8000;  //1=response (0=query)
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
  private static final int CD = 0x0010;  //checking disabled???
  //4 bits result code (0=no error)
  private static final int ERR_NO_ERROR     = 0x0000;  //no error
  private static final int ERR_NO_SUCH_NAME = 0x0003;  //404

  private static final int A = 1;
  private static final int CNAME = 5;
  private static final int SOA = 6;  //start of auth
  private static final int PTR = 12;
  private static final int MX = 15;
  private static final int AAAA = 28;

  private class Worker extends Thread {
    private DatagramPacket packet;
    private byte data[];
    private int dataLength;
    private int nameLength;  //bytes used decoding name

    private byte reply[];
    private int replyOffset;
    private ByteBuffer bb;
    private ByteBuffer replyBuffer;
    private short id, flgs;
    private int opcode;

    public Worker(DatagramPacket packet) {
      this.packet = packet;
    }

    public void run() {
      try {
        data = packet.getData();
        dataLength = packet.getLength();
        bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        id = bb.getShort(0);
        flgs = bb.getShort(2);
        if ((flgs & QR) != 0) {
          throw new Exception("response sent to server");
        }
        opcode = flgs & OPCODE_MASK;
        switch (opcode) {
          default:
            throw new Exception("opcode not supported:" + opcode);
          case OPCODE_IQUERY:
            throw new Exception("inverse query not supported");
          case OPCODE_UPDATE:
            throw new Exception("update not supported");
          case OPCODE_QUERY:
            doQuery();
            break;
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    private void doQuery() throws Exception {
      short cndQ = bb.getShort(4);
      if (cndQ != 1) throw new Exception("only 1 question supported");
      short cndA = bb.getShort(6);
      if (cndA != 0) throw new Exception("query with answers?");
      short cndS = bb.getShort(8);
      if (cndS != 0) throw new Exception("query with auth names?");
      short cndAdd = bb.getShort(10);
      if (cndAdd != 0) throw new Exception("query with adds?");
      int offset = 12;
      for(int a=0;a < cndQ; a++) {
        String domain = getName(data, offset);
        offset += nameLength;
        int type = bb.getShort(offset);
        offset += 2;
        int cls = bb.getShort(offset);
        if (cls != 1) throw new Exception("only internet class supported");
        offset += 2;
        if (domain.endsWith(".in-addr.arpa")) {
          //reverse IPv4 query (just send bogus info)
          sendReply(domain, "*.in-addr.arpa,ptr,1440,localdomain", type, id);
          continue;
        }
        if (domain.endsWith(".ip6.arpa")) {
          //reverse IPv6 query (just send bogus info)
          sendReply(domain, "*.ip6.arpa,ptr,1440,localdomain", type, id);
          continue;
        }
        if (queryLocal(domain, type, id)) continue;
        queryRemote(domain, type, id);
      }
    }

    private void doUpdate() throws Exception {

    }

    private String getName(byte data[], int offset) {
      StringBuilder name = new StringBuilder();
      boolean jump = false;
      nameLength = 0;
      do {
        if (!jump) nameLength++;
        int length = ((int)data[offset++]) & 0xff;
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
        reply[replyOffset++] = (byte)strlen;
        length++;
        System.arraycopy(p[a].getBytes(), 0, reply, replyOffset, strlen);
        replyOffset += strlen;
        length += strlen;
      }
      //zero length part ends string
      reply[replyOffset++] = 0;
      length++;
      return length;
    }

    private boolean queryLocal(String domain, int type, int id) {
      //TODO : query derby and add answer if available
      //NOTE : set aa if found in local db and do not add anything to nameservers
      int rc = records.size();
      String match = null;
      switch (type) {
        case A: match = domain + ",a,"; break;
        case CNAME: match = domain + ",cname,"; break;
        case MX: match = domain + ",mx,"; break;
        case AAAA: match = domain + ",aaaa,"; break;
      }
      if (match == null) return false;
      match = match.toLowerCase();
      for(int a=0;a<rc;a++) {
        String record = records.get(a);
        if (record.startsWith(match)) {
          //name,type,ttl,values...
          sendReply(domain, record, type, id);
          return true;
        }
      }
      return false;
    }

    private boolean isAllowed(String domain) {
      int cnt = allows.size();
      for(int a=0;a<cnt;a++) {
        if (domain.matches(allows.get(a))) return true;
      }
      cnt = denies.size();
      for(int a=0;a<cnt;a++) {
        if (domain.matches(denies.get(a))) return false;
      }
      return true;
    }

    private String typeToString(int type) {
      switch (type) {
        case A: return "A";
        case CNAME: return "CNAME";
        case MX: return "MX";
        case AAAA: return "AAAA";
        case SOA: return "SOA";
        case PTR: return "PTR";
      }
      return "A";
    }

    private void queryRemote(String domain, int type, int id) {
      JFLog.log("queryRemote:domain=" + domain + ",type=" + type);
      //query remote DNS server and simple relay the reply "as is"
      //TODO : need to actually remove AA flag if present and fill in other sections as needed
      if (!isAllowed(domain)) {
        JFLog.log("not allowed:" + domain);
        //send "example.com" which will give a 404 error
        if (type == AAAA)
          sendReply(domain, domain + "," + typeToString(type) + ",3600,0:0:0:0:0:FFFF:5DB8:D822", type, id);
        else
          sendReply(domain, domain + "," + typeToString(type) + ",3600,93.184.216.34", type, id);
        return;
      }
      int cnt = uplink.size();
      for(int idx=0;idx<cnt;idx++) {
        try {
          DatagramPacket out = new DatagramPacket(data, dataLength);
          out.setAddress(InetAddress.getByName(uplink.get(idx)));
          out.setPort(53);
          DatagramSocket sock = new DatagramSocket();  //bind to anything
          sock.setSoTimeout(uplinktimeout);
          sock.send(out);
          reply = new byte[maxmtu];
          DatagramPacket in = new DatagramPacket(reply, reply.length);
          sock.receive(in);
          sendReply(domain, reply, in.getLength());
          return;
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
      JFLog.log("Query Remote failed for domain=" + domain);
    }

    private void sendReply(String domain, byte outData[], int outDataLength) {
      JFLog.log("sendReply:" + domain + " to " + packet.getAddress() + ":" + packet.getPort());
      try {
        DatagramPacket out = new DatagramPacket(outData, outDataLength);
        out.setAddress(packet.getAddress());
        out.setPort(packet.getPort());
        ds.send(out);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    private void sendReply(String query, String record, int type, int id) {
      JFLog.log("sendReply:query=" + query + ",record=" + record + ",type=" + type);
      int rdataOffset, rdataLength;
      //record = type,name,ttl,value
      String f[] = record.split(",");
      int ttl = JF.atoi(f[2]);
      reply = new byte[maxmtu];
      replyBuffer = ByteBuffer.wrap(reply);
      replyBuffer.order(ByteOrder.BIG_ENDIAN);
      replyOffset = 0;
      putShort((short)id);
      putShort((short)(QR | AA | RA));
      putShort((short)1);  //questions
      putShort((short)1);  //answers
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
          putInt(ttl);
          putShort((short)4);  //Rdata length
          putIP4(f[3]);  //Rdata
          break;
        case CNAME:
        case PTR:
          encodeName(query);
          putShort((short)type);
          putShort((short)1);  //class
          putInt(ttl);
          rdataOffset = replyOffset;
          putShort((short)0);  //Rdata length (patch 2 lines down)
          rdataLength = encodeName(f[3]);
          replyBuffer.putShort(rdataOffset, (short)rdataLength);
          break;
        case MX:
          encodeName(query);
          putShort((short)type);
          putShort((short)1);  //class
          putInt(ttl);
          rdataOffset = replyOffset;
          putShort((short)0);  //Rdata length (patch 3 lines down)
          putShort((short)JF.atoi(f[3]));  //preference (1-99)
          rdataLength = encodeName(f[4]);  //cname
          replyBuffer.putShort(rdataOffset, (short)(rdataLength + 2));
          break;
        case AAAA:
          encodeName(query);
          putShort((short)type);
          putShort((short)1);  //class
          putInt(ttl);
          putShort((short)16);  //Rdata length
          putIP6(f[3]);  //Rdata
          break;
      }
      sendReply(query, reply, replyOffset);
    }

    private void putIP4(String ip) {
      String p[] = ip.split("[.]", -1);
/*
      if (p.length != 4) {
        p = new String[] {"0", "0", "0", "0"};
      }
*/
      for(int a=0;a<4;a++) {
        reply[replyOffset++] = (byte)JF.atoi(p[a]);
      }
    }

    private void putIP6(String ip) {
      String p[] = ip.split(":", -1);
/*
      if (p.length != 8) {
        p = new String[] {"0", "0", "0", "0", "0", "0", "0", "0"};
      }
*/
      for(int a=0;a<8;a++) {
        putShort((short)JF.atox(p[a]));
      }
    }

    private void putShort(short value) {
      replyBuffer.putShort(replyOffset, value);
      replyOffset += 2;
    }

    private void putInt(int value) {
      replyBuffer.putInt(replyOffset, value);
      replyOffset += 4;
    }
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static class JBusMethods {
    public void getConfig(String pack) {
      dns.busClient.call(pack, "getConfig", dns.busClient.quote(dns.busClient.encodeString(dns.config)));
    }
    public void setConfig(String cfg) {
      //write new file
      JFLog.log("setConfig");
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      JFLog.log("restart");
      dns.close();
      dns = new DNS();
      dns.start();
    }
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33005;
    } else {
      return 777;
    }
  }

  public static void main(String args[]) {
    serviceStart(args);
  }

  //Win32 Service

  private static DNS dns;

  public static void serviceStart(String args[]) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    dns = new DNS();
    dns.start();
  }

  public static void serviceStop() {
    JFLog.log("Stopping service");
    busServer.close();
    dns.close();
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
