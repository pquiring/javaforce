package javaforce.service;

/**
 * Basic DHCP Server (IP4 only)
 *
 * Only supports subnet mask of 255.255.255.0 for now (max 255 clients)
 *
 * @author pquiring
 *
 * Created : Nov 17, 2013
 */

import java.io.*;
import java.nio.*;
import java.net.*;

import javaforce.*;

public class DHCP extends Thread {
  public static boolean SystemService = false;

  private final static String UserConfigFile = JF.getUserPath() + "/.jdhcp.cfg";
  private final static String SystemConfigFile = "/etc/jdhcp.cfg";

  private DatagramSocket ds;
  private static int maxmtu = 1500 - 20 - 8;  //IP=20 UDP=8
  private static String server_ip;  //dhcp server ip
  private static String pool_ip;  //pool starting ip
  private static int pool_ip_int;  //pool starting ip (as int)
  private static String mask;
  private static int count = 0;
  private static long pool_time[];  //timestamp of issue (0=not in use)
  private static int pool_hwlen[];  //hwaddr len of client
  private static byte pool_hwaddr[][];  //hwaddr of client
  private static int next = 0;  //offset
  private static String router;
  private static String dns = "8.8.8.8";
  private static int leaseTime = 3600 * 24;  //in seconds
  private static InetAddress broadcastAddress;

  public void run() {
    if (SystemService)
      JFLog.init("/var/log/jdhcp.log", false);
    else
      JFLog.init(JF.getUserPath() + "/.jdhcp.log", false);
    try {
      loadConfig();
      if (server_ip == null || router == null || pool_ip == null || mask == null || count <= 0) {
        throw new Exception("invalid config");
      }
      if (!validIP(server_ip)) throw new Exception("invalid server_ip");
      if (!validIP(pool_ip)) throw new Exception("invalid pool_ip");
      if (!validIP(router)) throw new Exception("invalid router");
      if (!validIP(mask)) throw new Exception("invalid mask");
      if (!validIP(dns)) throw new Exception("invalid dns");
      if (!mask.equals("255.255.255.0")) {
        throw new Exception("mask not supported");
      }
      if (count > 255) {
        throw new Exception("pool too large");
      }
      if (leaseTime < 3600 || leaseTime > 3600 * 24) {
        JFLog.log("leaseTime invalid, using 24 hrs");
        leaseTime = 3600 * 24;
      }
      String p[] = pool_ip.split("[.]");
      int lastp = JF.atoi(p[3]) + count;
      if (lastp > 255) {
        throw new Exception("invalid pool");
      }
      pool_time = new long[count];
      pool_hwlen = new int[count];
      pool_hwaddr = new byte[count][16];
      broadcastAddress = InetAddress.getByName("255.255.255.255");
      for(int a=0;a<5;a++) {
        try {
          ds = new DatagramSocket(67);
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
        byte data[] = new byte[maxmtu];
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
    = "[global]\r\n"
    + "#remove all comments below and change as desired.\r\n"
    + "#server_ip=192.168.0.2\r\n"
    + "#pool_ip=192.168.0.100\r\n"
    + "#mask=255.255.255.0\r\n"
    + "#count=100\r\n"
    + "#router=192.168.0.1\r\n"
    + "#dns=8.8.8.8\r\n"
    + "#lease=86400\r\n"
    ;

  private void loadConfig() {
    Section section = Section.None;
    try {
      BufferedReader br = new BufferedReader(new FileReader(SystemService ? SystemConfigFile : UserConfigFile));
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        ln = ln.trim().toLowerCase();
        int idx = ln.indexOf('#');
        if (idx != -1) ln = ln.substring(0, idx).trim();
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        switch (section) {
          case Global:
            if (ln.startsWith("pool_ip=")) pool_ip = ln.substring(8);
            else if (ln.startsWith("server_ip=")) server_ip = ln.substring(10);
            else if (ln.startsWith("mask=")) mask = ln.substring(5);
            else if (ln.startsWith("dns=")) dns = ln.substring(4);
            else if (ln.startsWith("router=")) router = ln.substring(7);
            else if (ln.startsWith("count=")) count = JF.atoi(ln.substring(6));
            else if (ln.startsWith("lease=")) leaseTime = JF.atoi(ln.substring(6));
            //...
            break;
        }
      }
      pool_ip_int = IP4toInt(pool_ip);
    } catch (FileNotFoundException e) {
      //create default config
      try {
        FileOutputStream fos = new FileOutputStream(SystemService ? SystemConfigFile : UserConfigFile);
        fos.write(defaultConfig.getBytes());
        fos.close();
      } catch (Exception e2) {
        JFLog.log(e2);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private boolean validIP(String ip) {
    String o[] = ip.split("[.]");
    for(int a=0;a<4;a++) {
      int v = Integer.valueOf(o[a]);
      if (v < 0) return false;
      if (v > 255) return false;
    }
    return true;
  }

  private int IP4toInt(String ip) {
    String o[] = ip.split("[.]");
    int ret = 0;
    for(int a=0;a<4;a++) {
      ret <<= 8;
      ret += (JF.atoi(o[a]));
    }
    return ret;
  }

  private byte[] IP4toByteArray(String ip) {
    String o[] = ip.split("[.]");
    byte ret[] = new byte[4];
    for(int a=0;a<4;a++) {
      ret[a] = (byte)JF.atoi(o[a]);
    }
    return ret;
  }

  private static final int cookie = 0x63825363;

  private static final int DHCPDISCOVER = 1;
  private static final int DHCPOFFER = 2;
  private static final int DHCPREQUEST = 3;
  private static final int DHCPDECLINE = 4;
  private static final int DHCPACK = 5;
  private static final int DHCPNAK = 6;
  private static final int DHCPRELEASE = 7;
  //private static final int DHCPINFORM = 8;  //???

  private static final byte OPT_PAD = 0;
  private static final byte OPT_SUBNET_MASK = 1;
  private static final byte OPT_ROUTER = 3;
  private static final byte OPT_DNS = 6;
  private static final byte OPT_REQUEST_IP = 50;
  private static final byte OPT_LEASE_TIME = 51;
  private static final byte OPT_DHCP_MSG_TYPE = 53;
  private static final byte OPT_DHCP_SERVER_IP = 54;
  private static final byte OPT_END = -1;  //255

  private static final Object lock = new Object();

  private class Worker extends Thread {
    private DatagramPacket packet;
    private byte data[];

    private byte reply[];
    private int replyOffset;  //offset while encoding name
    private ByteBuffer replyBuffer;

    public Worker(DatagramPacket packet) {
      this.packet = packet;
    }
    public void run() {
      try {
        data = packet.getData();
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);
        byte opcode = data[0];
        if (opcode != 1) throw new Exception("not a request");
        byte hwtype = data[1];
        byte hwlen = data[2];
        byte hop = data[3];
        int id = bb.getInt(4);
        short seconds = bb.getShort(8);
        short flags = bb.getShort(10);
        int cip = bb.getInt(12);
        int yip = bb.getInt(16);
        int yipOffset = -1;
        if ((yip & 0xffffff00) == (pool_ip_int & 0xffffff00)) {
          yipOffset = yip - pool_ip_int;
        }
        int sip = bb.getInt(20);
        int gip = bb.getInt(24);
        int msgType = -1;
        //28 = 16 bytes = client hardware address (ethernet : 6 bytes)
        //44 = 64 bytes = server host name (ignored)
        //108 = 128 bytes = boot filename (ignored)
        //236 = 4 bytes = cookie (0x63825363)
        //240 = options...
        int offset = 240;
        while (true) {
          byte opt = data[offset++];
          if (opt == OPT_PAD) continue;
          byte len = data[offset++];
          switch (opt) {
            case OPT_DHCP_MSG_TYPE:
              if (len != 1) throw new Exception("bad dhcp msg type");
              msgType = data[offset];
              break;
            case OPT_REQUEST_IP:
              if (len != 4) throw new Exception("bad request ip size");
              yip = bb.getInt(offset);
              if ((yip & 0xffffff00) == (pool_ip_int & 0xffffff00)) {
                yipOffset = yip - pool_ip_int;
              }
              break;
          }
          if (opt == OPT_END) break;
          offset += len;
        }
        if (msgType == -1) throw new Exception("no dhcp msg type");
        long now = System.currentTimeMillis();
        switch (msgType) {
          case DHCPDISCOVER:
            synchronized(lock) {
              int i = next++;
              int c = 0;
              while (c < count) {
                if (pool_time[i] != 0) {
                  if (pool_time[i] < now) pool_time[i] = 0;
                }
                if (pool_time[i] == 0) {
                  //check with ping (Java 5 required)
                  byte addr[] = IP4toByteArray(pool_ip);
                  addr[3] += i;
                  InetAddress inet = InetAddress.getByAddress(addr);
                  if (inet.isReachable(1000)) {
                    //IP still in use!
                    //this could happen if DHCP service is restarted since leases are only saved in memory
                    pool_time[i] = now + (leaseTime * 1000);
                  } else {
                    //offer this
                    sendReply(addr, DHCPOFFER, id);
                    next = i+1;
                    if (next == count) next = 0;
                    break;
                  }
                }
                c++;
                i++;
                if (i == count) i = 0;
              }
            }
            //nothing left in pool to send an offer (ignore request)
            break;
          case DHCPREQUEST:
            //mark ip as used and send ack or nak if already in use
            if (yipOffset < 0 || yipOffset >= count) {
              JFLog.log("request out of range");
              break;
            }
            synchronized(lock) {
              byte addr[] = IP4toByteArray(pool_ip);
              addr[3] += yipOffset;
              if (pool_time[yipOffset] != 0) {
                //check if hwaddr is the same
                boolean same = true;
                for(int a=0;a<pool_hwlen[yipOffset];a++) {
                  if (pool_hwaddr[yipOffset][a] != data[28 + a]) {same = false; break;}
                }
                if (same) pool_time[yipOffset] = 0;
              }
              if (pool_time[yipOffset] == 0) {
                //send ACK
                pool_time[yipOffset] = now + (leaseTime * 1000);
                pool_hwlen[yipOffset] = hwlen;
                System.arraycopy(data, 28, pool_hwaddr[yipOffset], 0, 16);
                sendReply(addr, DHCPACK, id);
              } else {
                //send NAK
                sendReply(addr, DHCPNAK, id);
              }
            }
            break;
          case DHCPRELEASE:
            //mark ip as unused
            if (yipOffset < 0 || yipOffset >= count) {
              JFLog.log("release out of range");
              break;
            }
            synchronized(lock) {
              if (pool_time[yipOffset] != 0) {
                //check if hwaddr is the same
                boolean same = true;
                for(int a=0;a<pool_hwlen[yipOffset];a++) {
                  if (pool_hwaddr[yipOffset][a] != data[28 + a]) {same = false; break;}
                }
                if (!same) break;
                pool_time[yipOffset] = 0;
              }
            }
            break;
          default:
            throw new Exception("unsupported dhcp msg type");
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    private void sendReply(byte outData[], int outDataLength) {
      try {
        DatagramPacket out = new DatagramPacket(outData, outDataLength);
        out.setAddress(broadcastAddress);
        out.setPort(packet.getPort());
        ds.send(out);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    private void sendReply(byte yip[], int msgType /*offer,ack,nak*/, int id) {
      reply = new byte[maxmtu];
      replyBuffer = ByteBuffer.wrap(reply);
      replyBuffer.order(ByteOrder.BIG_ENDIAN);
      replyOffset = 0;
      reply[replyOffset++] = 2;  //reply opcode
      reply[replyOffset++] = data[1];  //hwtype
      reply[replyOffset++] = data[2];  //hwlen
      reply[replyOffset++] = 0;  //hops
      putInt(id);
      putShort((short)0);  //seconds
      putShort((short)0);  //flags
      putInt(0);  //client IP
      putByteArray(yip);  //your IP
      putIP4(server_ip);  //server ip
      putIP4(router);  //router ip
      System.arraycopy(data, replyOffset, reply, replyOffset, 16);  //client hwaddr
      replyOffset += 16;
      replyOffset += 64;  //server name
      replyOffset += 128;  //boot filename (legacy BOOTP)
      //add cookie
      putInt(cookie);
      //add options
      reply[replyOffset++] = OPT_DHCP_MSG_TYPE;
      reply[replyOffset++] = 1;
      reply[replyOffset++] = (byte)msgType;

      reply[replyOffset++] = OPT_SUBNET_MASK;
      reply[replyOffset++] = 4;
      putIP4(mask);

      reply[replyOffset++] = OPT_DNS;
      reply[replyOffset++] = 4;
      putIP4(dns);

      reply[replyOffset++] = OPT_ROUTER;
      reply[replyOffset++] = 4;
      putIP4(router);

      reply[replyOffset++] = OPT_DHCP_SERVER_IP;
      reply[replyOffset++] = 4;
      putIP4(server_ip);

      reply[replyOffset++] = OPT_LEASE_TIME;
      reply[replyOffset++] = 4;
      putInt(leaseTime);

      reply[replyOffset++] = OPT_END;

      sendReply(reply, replyOffset);
    }

    private void putByteArray(byte ba[]) {
      for(int a=0;a<ba.length;a++) {
        reply[replyOffset++] = ba[a];
      }
    }

    private void putIP4(String ip) {
      String p[] = ip.split("[.]");
      for(int a=0;a<4;a++) {
        reply[replyOffset++] = (byte)JF.atoi(p[a]);
      }
    }

    private void putIP6(String ip) {
      String p[] = ip.split(":");
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
}
