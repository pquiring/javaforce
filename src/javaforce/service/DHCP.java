package javaforce.service;

/**
 * DHCP Server
 *
 * Supports:
 *  - IP4 pools
 *  - PXE options
 *  - PXE proxy
 *
 * @author pquiring
 *
 * Created : Nov 17, 2013
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

public class DHCP extends Thread {

  public final static String busPack = "net.sf.jfdhcp";

  /** DHCP Notification interface. */
  public static interface Notify {
    /** DHCP Event.
     * @param type = message type : DHCPDISCOVER, DHCPREQUEST, DHCPRELEASE
     * @param mac = client MAC address in hex (123456)
     * @param ip = client IP address (x.x.x.x)
     * @param arch = client arch type : ARCH_...
     */
    public void dhcpEvent(int type, String mac, String ip, short arch);
  }

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfdhcp.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfdhcp.log";
  }

  private static int maxmtu = 1500 - 20 - 8;  //IP=20 UDP=8

  private static enum OptionType {string, ip4, int8, int16, int32};

  private static class Option {
    public int num;
    public OptionType type;
    public String data;
  }

  private static class Pool {
    public Object lock = new Object();
    public String name;
    public String server_ip;  //dhcp server ip
    public int server_ip_int;
    public String bind_ip;  //bind ip (0.0.0.0 = all interfaces)
    public int bind_ip_int;
    public String pool_first;  //pool first ip
    public int pool_first_int;  //pool first ip (as int)
    public String pool_last;  //pool last ip
    public int pool_last_int;  //pool last ip (as int)
    public String mask;  //subnet mask
    public int mask_int;  //subnet mask (as int)
    public int host_mask_int;  //inverse subnet mask (host bits)
    public int count = 0;  //# of IPs in pool
    public long pool_time[];  //timestamp of issue (0=not in use)
    public int pool_hwlen[];  //hwaddr len of client
    public byte pool_hwaddr[][];  //hwaddr of client
    public int next = 0;  //offset
    public String router;
    public String dns;
    public int leaseTime = 3600 * 24;  //in seconds
    public boolean pxe_proxy;
    public String pxe_server;
    public String pxe_bootfile;
    public ArrayList<Option> options = new ArrayList<Option>();
  }
  private ArrayList<Pool> pools = new ArrayList<Pool>();
  private Pool global = new Pool();
  private Inet4Address broadcastAddress;
  private Notify notify;
  private static class Host {
    public String ip;
    public int ip_int;
    public DatagramSocket ds;
  }
  private ArrayList<Host> hosts = new ArrayList<Host>();
  private Object close = new Object();

  public static enum State {Loading, Running, Error, Stopped};
  public State state = State.Loading;
  public Object stateMonitor = new Object();

  public void run() {
    JFLog.append(getLogFile(), true);
    JFLog.setRetention(30);
    try {
      loadConfig();
      busClient = new JBusClient(busPack, new JBusMethods());
      busClient.setPort(getBusPort());
      busClient.start();
      if (!validConfig()) {
        throw new Exception("invalid config");
      }
      broadcastAddress = (Inet4Address)Inet4Address.getByName("255.255.255.255");
      for(int a=0;a<hosts.size();a++) {
        new HostWorker(hosts.get(a)).start();
      }
      setState(State.Running);
      //wait for close request
      synchronized(close) {
        close.wait();
      }
      setState(State.Stopped);
    } catch (Exception e) {
      JFLog.log(e);
      setState(State.Error);
    }
  }

  public void setState(State newState) {
    synchronized(stateMonitor) {
      state = newState;
      stateMonitor.notify();
    }
  }

  /** Set DHCP Notify interface. */
  public void setNotify(Notify notify) {
    this.notify = notify;
  }

  /** Stop the DHCP Service. */
  public void close() {
    int cnt = hosts.size();
    for(int a=0;a<cnt;a++) {
      DatagramSocket ds;
      ds = hosts.get(a).ds;
      if (ds != null) ds.close();
    }
    if (busClient != null) busClient.close();
    synchronized(close) {
      close.notify();
    }
  }

  private class HostWorker extends Thread {
    private Host host;
    public HostWorker(Host host) {
      this.host = host;
    }
    public void run() {
      JFLog.log("Starting HostWorker on : " + host.ip);
      try {
        host.ds = new DatagramSocket(67, Inet4Address.getByName(host.ip));
        while (true) {
          byte data[] = new byte[maxmtu];
          DatagramPacket packet = new DatagramPacket(data, maxmtu);
          host.ds.receive(packet);
          new RequestWorker(packet, host).start();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void close() {
      try {
        host.ds.close();
        host.ds = null;
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  enum Section {None, Global, Pool};

  private final static String defaultConfig
    = "#comments start with a # symbol\n"
    + "[global]\n"
    + "#dns=8.8.8.8\n"
    + "\n"
    + "#[pool_192_168_0_x]\n"
    + "#server_ip=192.168.0.2\n"
    + "#bind_ip=192.168.0.2\n"
    + "#pool_first=192.168.0.100\n"
    + "#pool_last=192.168.0.199\n"
    + "#mask=255.255.255.0\n"
    + "#router=192.168.0.1\n"
    + "#dns=8.8.8.8\n"
    + "#lease=86400  #24 hrs (1hr=min 24hrs=max)\n"
    + "#option=128:string:your_string\n"
    + "#option=129:ip4:1.2.3.4\n"
    + "#option=130:int:12345678\n"
    + "#option=131:short:1234\n"
    + "#option=132:byte:12\n"
    + "\n"
    + "#[pool_192_168_1_x]\n"
    + "#server_ip=192.168.1.2\n"
    + "#bind_ip=192.168.1.2\n"
    + "#pool_first=192.168.1.100\n"
    + "#pool_last=192.168.1.250\n"
    + "#mask=255.255.255.0\n"
    + "#router=192.168.1.1\n"
    + "#dns=8.8.8.8\n"
    + "#lease=7200  #2 hrs\n"
    + "\n"
    + "#[pool_10_1_1_x_for_relay_agents_only]\n"
    + "#server_ip=192.168.2.2\n"
    + "#bind_ip=0.0.0.0  #bind to all interfaces\n"
    + "#pool_first=10.1.1.100\n"
    + "#pool_last=10.1.1.250\n"
    + "#mask=255.255.255.0\n"
    + "#router=10.1.1.1\n"
    + "\n"
    + "#[pool_192_168_3_x_pxe]\n"
    + "#server_ip=192.168.3.2\n"
    + "#bind_ip=192.168.3.2\n"
    + "#pool_first=192.168.3.100\n"
    + "#pool_last=192.168.3.250\n"
    + "#mask=255.255.255.0\n"
    + "#router=192.168.3.1\n"
    + "#pxe_server=192.168.3.50\n"
    + "#pxe_bootfile=boot/pxelinux\n"
    + "\n"
    + "#[pool_192_168_4_x_pxe_proxy]\n"
    + "#server_ip=192.168.4.2\n"
    + "#bind_ip=192.168.4.2\n"
    + "#pxe_proxy=true\n"
    + "#pxe_server=192.168.4.50\n"
    + "#pxe_bootfile=boot/pxelinux\n"
  ;

  private void addOption(Pool pool, String str) {
    int i1 = str.indexOf(":");
    if (i1 == -1) {
      JFLog.log("bad option:" + str);
      return;
    }
    int i2 = str.substring(i1+1).indexOf(":");
    if (i2 == -1) {
      JFLog.log("bad option:" + str);
      return;
    }
    i2 += i1 + 1;
    String num = str.substring(0, i1);
    String type = str.substring(i1+1, i2);
    String data = str.substring(i2+1);
    Option opt = new Option();
    opt.num = Integer.valueOf(num);
    if (type.equals("int")) {
      opt.type = OptionType.int32;
    } else if (type.equals("short")) {
      opt.type = OptionType.int16;
    } else if (type.equals("byte")) {
      opt.type = OptionType.int8;
    } else if (type.equals("ip4")) {
      opt.type = OptionType.ip4;
    } else {
      opt.type = OptionType.string;
    }
    opt.data = data;
    pool.options.add(opt);
  }

  private int clampLeaseTime(int value) {
    if (value < 3600) value = 3600;  //1hr min
    if (value > 86400) value = 86400;  //1 day max
    return value;
  }

  private void loadConfig() {
    pools.clear();
    hosts.clear();
    Pool pool = null;
    try {
      StringBuilder cfg = new StringBuilder();
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim();
        int idx = ln.indexOf('#');
        if (idx != -1) ln = ln.substring(0, idx).trim();
        if (ln.length() == 0) continue;
        if (ln.startsWith("[") && ln.endsWith("]")) {
          String sectionName = ln.substring(1,ln.length() - 1);
          if (sectionName.equals("global")) {
            pool = global;
          } else {
            pool = new Pool();
            pool.name = sectionName;
            pools.add(pool);
          }
          continue;
        }
        idx = ln.indexOf("=");
        if (idx == -1) continue;
        String key = ln.substring(0, idx).toLowerCase().trim();
        String value = ln.substring(idx+1).trim();
        switch (key) {
          case "pool_first": pool.pool_first = value; break;
          case "pool_last": pool.pool_last = value; break;
          case "server_ip": pool.server_ip = value; break;
          case "bind_ip": pool.bind_ip = value; break;
          case "mask": pool.mask = value; break;
          case "dns": pool.dns = value; break;
          case "router": pool.router = value; break;
          case "lease": pool.leaseTime = clampLeaseTime(JF.atoi(value)); break;
          case "option": addOption(pool, value); break;
          case "pxe_server": pool.pxe_server = value; break;
          case "pxe_bootfile": pool.pxe_bootfile = value; break;
          case "pxe_proxy": pool.pxe_proxy = value.equals("true"); break;
        }
      }
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
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

  private boolean validConfig() {
    try {
      if (pools.size() == 0) throw new Exception("no pools defined");
      if (global.server_ip != null) {
        if (!validIP4(global.server_ip)) throw new Exception("global : invalid server_ip");
        global.server_ip_int = IP4toInt(global.server_ip);
      }
      if (global.bind_ip == null) global.bind_ip = "0.0.0.0";
      global.bind_ip_int = IP4toInt(global.bind_ip);
      int cnt = pools.size();
      for(int a=0;a<cnt;a++) {
        Pool pool = pools.get(a);
        if (pool.server_ip == null) pool.server_ip = global.server_ip;
        if (pool.router == null) pool.router = global.router;
        if (pool.dns == null) pool.dns = global.dns;
        if (pool.bind_ip == null) pool.bind_ip = global.bind_ip;
        if (!validIP4(pool.server_ip)) throw new Exception(pool.name + " : invalid server_ip");
        if (!validIP4(pool.pool_first)) throw new Exception(pool.name + " : invalid pool_first");
        if (!validIP4(pool.pool_last)) throw new Exception(pool.name + " : invalid pool_last");
        if (!validIP4(pool.router)) throw new Exception(pool.name + " : invalid router");
        if (!validIP4(pool.mask)) throw new Exception(pool.name + " : invalid mask");
        if (!validIP4(pool.dns)) throw new Exception(pool.name + " : invalid dns");
        if (pool.leaseTime < 3600 || pool.leaseTime > 3600 * 24) {
          JFLog.log(pool.name + " : leaseTime invalid, using 24 hrs");
          pool.leaseTime = 3600 * 24;
        }
        pool.server_ip_int = IP4toInt(pool.server_ip);
        pool.bind_ip_int = IP4toInt(pool.bind_ip);
        pool.pool_first_int = IP4toInt(pool.pool_first);
        pool.pool_last_int = IP4toInt(pool.pool_last);
        pool.mask_int = IP4toInt(pool.mask);
        if ((pool.pool_first_int & pool.mask_int) != (pool.pool_last_int & pool.mask_int)) {
          throw new Exception(pool.name + " : invalid pool range : " + pool.pool_first + "-" + pool.pool_last + ",mask=" + pool.mask);
        }
        pool.host_mask_int = pool.mask_int ^ 0xffffffff;
        pool.count = (pool.pool_last_int & pool.host_mask_int) - (pool.pool_first_int & pool.host_mask_int) + 1;
        pool.pool_time = new long[pool.count];
        pool.pool_hwlen = new int[pool.count];
        pool.pool_hwaddr = new byte[pool.count][16];
        JFLog.log("pool:" + IP4toString(pool.pool_first_int) + "-" + IP4toString(pool.pool_last_int) + ":" + pool.count + " IPs");
      }
      for(int a=0;a<cnt;a++) {
        Pool poola = pools.get(a);
        for(int b=0;b<cnt;b++) {
          if (b == a) continue;
          Pool poolb = pools.get(b);
          if ((poola.pool_first_int & poola.mask_int) == (poolb.pool_first_int & poolb.mask_int)) {
            throw new Exception("multiple pools overlap");
          }
        }
      }
      for(int a=0;a<cnt;a++) {
        Pool pool = pools.get(a);
        boolean hasHost = false;
        for(int b=0;b<hosts.size();b++) {
          Host host = hosts.get(b);
          if (host.ip == pool.bind_ip) {hasHost = true; break;}
        }
        if (!hasHost) {
          Host host = new Host();
          host.ip = pool.bind_ip;
          host.ip_int = IP4toInt(host.ip);
          hosts.add(host);
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  private boolean validIP4(String ip) {
    if (ip == null) return true;
    String o[] = ip.split("[.]");
    if (o.length != 4) return false;
    for(int a=0;a<4;a++) {
      int v = Integer.valueOf(o[a]);
      if (v < 0) return false;
      if (v > 255) return false;
    }
    return true;
  }

  private static int IP4toInt(String ip) {
    String o[] = ip.split("[.]");
    int ret = 0;
    for(int a=0;a<4;a++) {
      ret <<= 8;
      ret += (JF.atoi(o[a]));
    }
    return ret;
  }

  private static byte[] IP4toByteArray(String ip) {
    String o[] = ip.split("[.]");
    byte ret[] = new byte[4];
    for(int a=0;a<4;a++) {
      ret[a] = (byte)JF.atoi(o[a]);
    }
    return ret;
  }

  private static byte[] IP4toByteArray(int ip) {
    return IP4toByteArray(IP4toString(ip));
  }

  private static String IP4toString(int ip) {
    return String.format("%d.%d.%d.%d", ip >>> 24, (ip >> 16) & 0xff, (ip >> 8) & 0xff, ip & 0xff);
  }

  private static String IP4toString(byte ip[]) {
    return IP4toString(BE.getuint32(ip, 0));
  }

  public static String IP4toString(byte ip[], int offset) {
    return IP4toString(BE.getuint32(ip, offset));
  }

  private static String MACtoString(byte[] mac) {
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<6;a++) {
      sb.append(String.format("%02x", mac[a]));
    }
    return sb.toString();
  }

  private static final int cookie = 0x63825363;

  public static final int DHCP_OPCODE_REQUEST = 1;
  public static final int DHCP_OPCODE_REPLY = 2;

  public static final int DHCPDISCOVER = 1;
  public static final int DHCPOFFER = 2;
  public static final int DHCPREQUEST = 3;
  public static final int DHCPDECLINE = 4;
  public static final int DHCPACK = 5;
  public static final int DHCPNAK = 6;
  public static final int DHCPRELEASE = 7;
  public static final int DHCPINFORM = 8;

  private static String getMsgType(int type) {
    switch (type) {
      case DHCPDISCOVER: return "DHCPDISCOVER";
      case DHCPOFFER: return "DHCPOFFER";
      case DHCPREQUEST: return "DHCPREQUEST";
      case DHCPDECLINE: return "DHCPDECLINE";
      case DHCPACK: return "DHCPACK";
      case DHCPNAK: return "DHCPNAK";
      case DHCPRELEASE: return "DHCPRELEASE";
      case DHCPINFORM: return "DHCPINFORM";
    }
    return "???";
  }

  private static final byte OPT_PAD = 0;
  private static final byte OPT_SUBNET_MASK = 1;
  private static final byte OPT_ROUTER = 3;
  private static final byte OPT_DNS = 6;
  private static final byte OPT_REQUEST_IP = 50;
  private static final byte OPT_LEASE_TIME = 51;
  private static final byte OPT_DHCP_MSG_TYPE = 53;
  private static final byte OPT_DHCP_SERVER_IP = 54;
  private static final byte OPT_PARAM_REQ_LIST = 55;
  private static final byte OPT_MAX_MSG_SIZE = 57;
  private static final byte OPT_RENEWAL_TIME = 58;
  private static final byte OPT_VENDOR_CLASS = 60;
  private static final byte OPT_PXE_SERVER = 66;
  private static final byte OPT_PXE_BOOTFILE = 67;
  private static final byte OPT_REBINDING_TIME = 59;
  private static final byte OPT_CLIENT_ARCH = 93;
  private static final byte OPT_CLIENT_NET_DEV_INT = 94;
  private static final byte OPT_CLIENT_ID = 97;
  private static final byte OPT_END = -1;  //255

  public static final short ARCH_BIOS_X86 = 0;
  public static final short ARCH_UEFI_X86 = 7;
  public static final short ARCH_UEFI_ARM = 11;

  public static final String RASPBERRY_PI_MAC = "dca632";

  private class RequestWorker extends Thread {
    private DatagramPacket packet;
    private Host host;

    private byte req[];
    private int reqOffset;
    private byte reply[];
    private int replyOffset;

    private Pool pool;

    public RequestWorker(DatagramPacket packet, Host host) {
      this.packet = packet;
      this.host = host;
    }
    public void run() {
      try {
        JFLog.log("Received request from:" + packet.getSocketAddress());
        req = packet.getData();
        reqOffset = 0;

        byte opcode = getByte();
        if (opcode != DHCP_OPCODE_REQUEST) throw new Exception("not a request");
        byte hwtype = getByte();
//        if (hwtype != 1) throw new Exception("not ethernet");
        byte hwlen = getByte();
//        if (hwlen != 6) throw new Exception("bad hardware length");
        byte hop = req[3];
        int id = getInt();
        short seconds = getShort();
        short flags = getShort();
        int cip = getInt();  //client ip
        int yip = getInt();  //your ip
        int sip = getInt();  //server ip
        int rip = getInt();  //relay ip
        int msgType = -1;
        int yipOffset = -1;
        //detect pool
        int cnt = pools.size();
        int from_ip = rip;
        short pxe_arch = -1;
        byte[] req_list = new byte[0];
        if (from_ip == 0) {
          String src = packet.getAddress().getHostAddress();
          from_ip = IP4toInt(src);
        }
        if (from_ip == 0) {
          from_ip = host.ip_int;
        }
        if (from_ip == 0 && pools.size() == 1) {
          from_ip = pools.get(0).server_ip_int;
        }
        if (from_ip == 0) {
          throw new Exception("can not determine pool for request");
        }
        JFLog.log("ip=" + IP4toString(from_ip));
        for(int a=0;a<cnt;a++) {
          pool = pools.get(a);
          if ((pool.pool_first_int & pool.mask_int) == (from_ip & pool.mask_int)) {
            break;
          }
          pool = null;
        }
        if (pool == null) throw new Exception("no pool for request");
        if (yip != 0 && (yip & pool.mask_int) == (pool.pool_first_int & pool.mask_int)) {
          yipOffset = yip - pool.pool_first_int;
        }
        //28 = 16 bytes = client hardware address (ethernet : 6 bytes)
        byte[] mac = new byte[6];
        getBytes(mac);
        //44 = 64 bytes = server host name (ignored)
        //108 = 128 bytes = boot filename (ignored)
        //236 = 4 bytes = cookie (0x63825363)
        //240 = options...
        reqOffset = 240;
        while (true) {
          byte opt = getByte();
          if (opt == OPT_PAD) continue;
          int len = getByte() & 0xff;
          switch (opt) {
            case OPT_DHCP_MSG_TYPE:
              if (len != 1) throw new Exception("bad dhcp msg type (size != 1)");
              msgType = getByte();
              break;
            case OPT_REQUEST_IP:
              if (len != 4) throw new Exception("bad request ip (size != 4)");
              yip = getInt();
              if ((yip & pool.mask_int) == (pool.pool_first_int & pool.mask_int)) {
                yipOffset = yip - pool.pool_first_int;
              } else {
                JFLog.log("invalid requested ip:" + IP4toString(yip));
              }
              break;
            case OPT_CLIENT_ARCH:
              if (len != 2) throw new Exception("bad arch id");
              pxe_arch = getShort();
              break;
            case OPT_PARAM_REQ_LIST:
              req_list = new byte[len];
              System.arraycopy(req, reqOffset, req_list, 0, len);
              reqOffset += len;
              break;
            default:
              reqOffset += len;
              break;
          }
          if (opt == OPT_END) break;
        }
        if (msgType == -1) throw new Exception("no dhcp msg type");
        JFLog.log("MsgType=" + getMsgType(msgType));
        long now = System.currentTimeMillis();
        switch (msgType) {
          case DHCPDISCOVER:
            if (pool.pxe_proxy) {
              //only send PXE info, no IP address
              if (notify != null) {
                notify.dhcpEvent(DHCPDISCOVER, MACtoString(mac), IP4toString(yip), pxe_arch);
              }
              sendReply(new byte[4], DHCPOFFER, id, pool, rip, req_list);
              break;
            }
            synchronized(pool.lock) {
              int i = pool.next++;
              int c = 0;
              while (c < pool.count) {
                if (pool.pool_time[i] != 0) {
                  if (pool.pool_time[i] < now) pool.pool_time[i] = 0;
                }
                if (pool.pool_time[i] == 0) {
                  //check with ping (Java 5 required)
                  byte addr[] = IP4toByteArray(pool.pool_first_int + i);
                  Inet4Address inet = (Inet4Address)Inet4Address.getByAddress(addr);
                  if (inet.isReachable(1000)) {
                    //IP still in use!
                    //this could happen if DHCP service is restarted since leases are only saved in memory
                    pool.pool_time[i] = now + (pool.leaseTime * 1000);
                    JFLog.log("warning:IP in use but not in database:" + IP4toString(addr));
                  } else {
                    //offer this
                    if (notify != null) {
                      notify.dhcpEvent(DHCPDISCOVER, MACtoString(mac), IP4toString(yip), pxe_arch);
                    }
                    sendReply(addr, DHCPOFFER, id, pool, rip, req_list);
                    pool.next = i+1;
                    if (pool.next == pool.count) pool.next = 0;
                    return;
                  }
                }
                c++;
                i++;
                if (i == pool.count) i = 0;
              }
            }
            //nothing left in pool to send an offer (ignore request)
            JFLog.log("no free IPs in pool for request");
            break;
          case DHCPREQUEST:
            if (pool.pxe_proxy) {
              if (notify != null) {
                notify.dhcpEvent(DHCPREQUEST, MACtoString(mac), IP4toString(yip), pxe_arch);
              }
              sendReply(new byte[4], DHCPACK, id, pool, rip, req_list);
              break;
            }
            //mark ip as used and send ack or nak if already in use
            if (yipOffset < 0 || yipOffset >= pool.count) {
              JFLog.log("request out of range:" + yipOffset);
              break;
            }
            synchronized(pool.lock) {
              byte addr[] = IP4toByteArray(pool.pool_first);
              addr[3] += yipOffset;
              if (pool.pool_time[yipOffset] != 0) {
                //check if hwaddr is the same
                boolean same = true;
                for(int a=0;a<pool.pool_hwlen[yipOffset];a++) {
                  if (pool.pool_hwaddr[yipOffset][a] != req[28 + a]) {same = false; break;}
                }
                if (same) pool.pool_time[yipOffset] = 0;
              }
              if (pool.pool_time[yipOffset] == 0) {
                //send ACK
                pool.pool_time[yipOffset] = now + (pool.leaseTime * 1000);
                pool.pool_hwlen[yipOffset] = hwlen;
                System.arraycopy(req, 28, pool.pool_hwaddr[yipOffset], 0, 16);
                if (notify != null) {
                  notify.dhcpEvent(DHCPREQUEST, MACtoString(mac), IP4toString(yip), pxe_arch);
                }
                sendReply(addr, DHCPACK, id, pool, rip, req_list);
              } else {
                //send NAK
                sendReply(addr, DHCPNAK, id, pool, rip, req_list);
              }
            }
            break;
          case DHCPRELEASE:
            //mark ip as unused
            if (notify != null) {
              notify.dhcpEvent(DHCPRELEASE, MACtoString(mac), IP4toString(yip), pxe_arch);
            }
            if (yipOffset < 0 || yipOffset >= pool.count) {
              JFLog.log("release out of range");
              break;
            }
            synchronized(pool.lock) {
              if (pool.pool_time[yipOffset] != 0) {
                //check if hwaddr is the same
                boolean same = true;
                for(int a=0;a<pool.pool_hwlen[yipOffset];a++) {
                  if (pool.pool_hwaddr[yipOffset][a] != req[28 + a]) {same = false; break;}
                }
                if (!same) break;
                pool.pool_time[yipOffset] = 0;
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

    private void sendReply(byte outData[], int outDataLength, int rip) {
      try {
        DatagramPacket out = new DatagramPacket(outData, outDataLength);
        if (rip == 0)
          out.setAddress(broadcastAddress);
        else
          out.setAddress(Inet4Address.getByAddress(new byte[] {(byte)(rip >> 24), (byte)((rip >> 16) & 0xff), (byte)((rip >> 8) & 0xff), (byte)(rip & 0xff)}));
        int port = packet.getPort();
        out.setPort(port);
        host.ds.send(out);
        JFLog.log("ReplyTo:" + IP4toString(rip) + ":" + port);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    private boolean req_opt(byte[] req_list, byte req) {
      for(int a=0;a<req_list.length;a++) {
        if (req_list[a] == req) return true;
      }
      return false;
    }

    private void sendReply(byte yip[], int msgType /*offer,ack,nak*/, int id, Pool pool, int rip, byte[] req_list) {
      JFLog.log("ReplyFor:" + IP4toString(yip) + ":" + getMsgType(msgType));
      reply = new byte[maxmtu];
      replyOffset = 0;
      reply[replyOffset++] = DHCP_OPCODE_REPLY;  //reply opcode
      reply[replyOffset++] = req[1];  //hwtype
      reply[replyOffset++] = req[2];  //hwlen
      reply[replyOffset++] = 0;  //hops
      putInt(id);
      putShort((short)0);  //seconds
      putShort((short)0x8000);  //flags  (bit 15 = broadcast)
      putInt(0);  //client IP
      putByteArray(yip);  //your IP
      putIP4(pool.server_ip);  //server ip
      putInt(rip);  //relay ip
      System.arraycopy(req, replyOffset, reply, replyOffset, 16);  //client hwaddr
      replyOffset += 16;
      replyOffset += 64;  //server name
      replyOffset += 128;  //boot filename (legacy BOOTP)
      //add cookie
      putInt(cookie);
      //add options
      reply[replyOffset++] = OPT_DHCP_MSG_TYPE;
      reply[replyOffset++] = 1;
      reply[replyOffset++] = (byte)msgType;

      if (pool.mask != null && !pool.pxe_proxy) {
        reply[replyOffset++] = OPT_SUBNET_MASK;
        reply[replyOffset++] = 4;
        putIP4(pool.mask);
      }

      if (pool.dns != null && !pool.pxe_proxy) {
        reply[replyOffset++] = OPT_DNS;
        reply[replyOffset++] = 4;
        putIP4(pool.dns);
      }

      if (pool.router != null && !pool.pxe_proxy) {
        reply[replyOffset++] = OPT_ROUTER;
        reply[replyOffset++] = 4;
        putIP4(pool.router);
      }

      if (!pool.pxe_proxy) {
        reply[replyOffset++] = OPT_REQUEST_IP;
        reply[replyOffset++] = 4;
        putByteArray(yip);  //your IP
      }

      if (!pool.pxe_proxy) {
        reply[replyOffset++] = OPT_DHCP_SERVER_IP;
        reply[replyOffset++] = 4;
        putIP4(pool.server_ip);
      }

      if (!pool.pxe_proxy) {
        reply[replyOffset++] = OPT_RENEWAL_TIME;
        reply[replyOffset++] = 4;
        putInt(pool.leaseTime - 1800);  //30 mins early
      }

      if (!pool.pxe_proxy) {
        reply[replyOffset++] = OPT_REBINDING_TIME;
        reply[replyOffset++] = 4;
        putInt(pool.leaseTime - 900);  //15 mins early
      }

      if (!pool.pxe_proxy) {
        reply[replyOffset++] = OPT_LEASE_TIME;
        reply[replyOffset++] = 4;
        putInt(pool.leaseTime);
      }

      if (pool.pxe_server != null && req_opt(req_list, OPT_PXE_SERVER)) {
        reply[replyOffset++] = OPT_PXE_SERVER;
        reply[replyOffset++] = (byte)pool.pxe_server.length();
        putByteArray(pool.pxe_server.getBytes());
      }

      if (pool.pxe_bootfile != null && req_opt(req_list, OPT_PXE_BOOTFILE)) {
        reply[replyOffset++] = OPT_PXE_BOOTFILE;
        reply[replyOffset++] = (byte)pool.pxe_bootfile.length();
        putByteArray(pool.pxe_bootfile.getBytes());
      }

      //add custom options
      for(int a=0;a<pool.options.size();a++) {
        Option opt = pool.options.get(a);
        reply[replyOffset++] = (byte)opt.num;
        switch (opt.type) {
          case string:
            reply[replyOffset++] = (byte)opt.data.length();
            putByteArray(opt.data.getBytes());
            break;
          case int32:
            reply[replyOffset++] = 4;
            putInt(Integer.valueOf(opt.data));
            break;
          case int16:
            reply[replyOffset++] = 2;
            putShort(Short.valueOf(opt.data));
            break;
          case int8:
            reply[replyOffset++] = 1;
            putByte(Byte.valueOf(opt.data));
            break;
          case ip4:
            reply[replyOffset++] = 4;
            putIP4(opt.data);
            break;
        }
      }

      reply[replyOffset++] = OPT_END;

      sendReply(reply, replyOffset, rip);
    }

    private byte getByte() {
      return req[reqOffset++];
    }

    private short getShort() {
      short value = (short)BE.getuint16(req, reqOffset);
      reqOffset += 2;
      return value;
    }

    private int getInt() {
      int value = BE.getuint32(req, reqOffset);
      reqOffset += 4;
      return value;
    }

    private void getBytes(byte[] out) {
      for(int a=0;a<out.length;a++) {
        out[a] = getByte();
      }
    }

    private void putByteArray(byte[] ba) {
      for(int a=0;a<ba.length;a++) {
        reply[replyOffset++] = ba[a];
      }
    }

    private void putIP4(String ip) {
      String[] p = ip.split("[.]");
      for(int a=0;a<4;a++) {
        reply[replyOffset++] = (byte)JF.atoi(p[a]);
      }
    }

    private void putIP6(String ip) {
      String[] p = ip.split(":");
      for(int a=0;a<8;a++) {
        putShort((short)JF.atox(p[a]));
      }
    }

    private void putByte(byte value) {
      reply[replyOffset++] = value;
    }

    private void putShort(short value) {
      BE.setuint16(reply, replyOffset, value);
      replyOffset += 2;
    }

    private void putInt(int value) {
      BE.setuint32(reply, replyOffset, value);
      replyOffset += 4;
    }
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static class JBusMethods {
    public void getConfig(String pack) {
      dhcp.busClient.call(pack, "getConfig", dhcp.busClient.quote(dhcp.busClient.encodeString(dhcp.config)));
    }
    public void setConfig(String cfg) {
      //write new file
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      dhcp.close();
      dhcp = new DHCP();
      dhcp.start();
    }
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33004;
    } else {
      return 777;
    }
  }

  public static void main(String[] args) {
    serviceStart(args);
  }

  //Win32 Service

  private static DHCP dhcp;

  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    dhcp = new DHCP();
    dhcp.start();
  }

  public static void serviceStop() {
    JFLog.log("Stopping service");
    dhcp.close();
  }
}
