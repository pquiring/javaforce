package javaforce.service;

/** STUN (w/ TURN support) server.
 *
 * Created : Dec 24, 2013
 *
 * See RFCs:
 * http://tools.ietf.org/html/rfc3489 - Classic STUN
 * http://tools.ietf.org/html/rfc5389 - STUN
 * http://tools.ietf.org/html/rfc5766 - TURN
 * http://tools.ietf.org/html/rfc5245 - ICE
 *
 * Notes:
 *  - Doesn't support CHANGE_REQUEST w/ Different IP
 *  - bind & alloc use the same timer (refresh one, the other is refreshed too)
 *  - limit one channel per client ip/port
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.*;
import java.nio.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

import javaforce.*;
import javaforce.jbus.JBusClient;
import javaforce.jbus.JBusServer;

public class STUN {
  private static int defaultLifeTime = 600;

  public final static String busPack = "net.sf.jfstun";

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfstun.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfstun.log";
  }

//requests
  private final static short BINDING_REQUEST = 0x0001;
  private final static short ALLOCATE_REQUEST = 0x0003;
  private final static short REFRESH_REQUEST = 0x0004;
  private final static short BIND_REQUEST = 0x0009;

//responses (success)
  private final static short BINDING_RESPONSE = 0x0101;
  private final static short ALLOCATE_RESPONSE = 0x0103;
  private final static short REFRESH_RESPONSE = 0x0104;
  private final static short BIND_RESPONSE = 0x0109;

//responses (failure)
  private final static short BINDING_FAILED = 0x0111;
  private final static short ALLOCATE_FAILED = 0x0113;
  private final static short REFRESH_FAILED = 0x0114;
  private final static short BIND_FAILED = 0x0119;

//attrs
  private final static short MAPPED_ADDRESS = 0x0001;
  private final static short CHANGE_REQUEST = 0x0003;
  private final static short USERNAME = 0x0006;
  private final static short MESSAGE_INTEGRITY = 0x0008;
  private final static short ERROR_CODE = 0x0009;
  private final static short CHANNEL_NUMBER = 0x000c;
  private final static short LIFETIME = 0x000d;
  private final static short XOR_PEER_ADDRESS = 0x0012;
  private final static short REALM = 0x0014;
  private final static short NONCE = 0x0015;
  private final static short XOR_RELAY_ADDRESS = 0x0016;
  private final static short DATA_INDICATION = 0x0017;
  private final static short EVEN_PORT = 0x0018;
  private final static short TRANSPORT_TYPE = 0x0019;
  private final static short XOR_MAPPED_ADDRESS = 0x0020;
  private final static short RESERVATION_TOKEN = 0x0022;

  private volatile boolean active = true;
  private volatile boolean done = false;
  private String user, pass;
  private DatagramSocket ds, ds2;
  private final String realm = "javaforce.service.STUN";
  private String publicip;

  private boolean doStart() {
    try {
      JFLog.log("Starting STUN/TURN Service on ports 3478 and 3479");
      ds = new DatagramSocket(3478);
      ds2 = new DatagramSocket(3479);
      new Worker().start();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Starts a STUN/TURN server, loading config from file. */
  public boolean start() {
    JFLog.init(getLogFile(), true);
    loadConfig();
    busClient = new JBusClient(busPack, new JBusMethods());
    busClient.setPort(getBusPort());
    busClient.start();
    return doStart();
  }

  /** Starts a STUN/TURN server with specific config options. */
  public boolean start(String user, String pass, int min, int max) {
    this.user = user;
    this.pass = pass;
    this.min = min;
    this.max = max;
    this.next = min;
    return doStart();
  }

  enum Section {None, Global};

  private final static String defaultConfig
    = "[global]\r\n"
    + "## remove comments below and change as desired.\r\n"
    + "#user=\r\n"
    + "#pass=\r\n"
    + "#publicip=1.2.3.4\r\n"
    + "## min/max are the UDP port ranges to use\r\n"
    + "#min=10000\r\n"
    + "#max=20000\r\n"
    ;

  private void loadConfig() {
    Section section = Section.None;
    try {
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      StringBuilder cfg = new StringBuilder();
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\r\n");
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
            if (ln.startsWith("user=")) user = ln.substring(5);
            else if (ln.startsWith("pass=")) pass = ln.substring(5);
            else if (ln.startsWith("publicip=")) publicip = ln.substring(9);
            else if (ln.startsWith("min=")) min = JF.atoi(ln.substring(4));
            else if (ln.startsWith("max=")) max = JF.atoi(ln.substring(4));
            break;
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

  public int getLocalPort() {
    return ds.getLocalPort();
  }

  public String getLocalAddr() {
    return ds.getLocalAddress().getHostAddress();
  }

  public void close() {
    active = false;
    if (ds != null) {
      try {
        ds.close();
        ds = null;
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    if (ds2 != null) {
      try {
        ds2.close();
        ds2 = null;
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    while (!done) {
      JF.sleep(10);
    }
  }

  private byte[] calcKey() {
    String msg = user + ":" + realm + ":" + pass;
    MD5 md5 = new MD5();
    md5.init();
    md5.add(msg.getBytes(), 0, msg.length());
    return md5.done();
  }

  private byte[] calcMsgIntegrity(byte data[], int length) {
    byte key[] = calcKey();
    try {
      SecretKeySpec ks = new SecretKeySpec(key, "HmacSHA1");
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(ks);
      return mac.doFinal(Arrays.copyOfRange(data, 0, length));
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private class Alloc {
    String nonce;
    String ip;
    InetAddress addr;  //same as ip
    int port;
    Timer timer;
    DatagramSocket ds;
    Object lock = new Object();
    long timeout;
    String id;
    int relayip, relayport;
    InetAddress relayaddr;
    RelayWorker worker;
    short channel;
    boolean evenPort;
    void setTimeout(int secs) {
      synchronized(lock) {
        if (timer != null) {
          timer.cancel();
        }
        if (ds == null) return;  //already freed
        timer = new Timer();
        timeout = System.currentTimeMillis() + (secs-1) * 1000;
        timer.schedule(new AllocTask(this), secs * 1000);
      }
    }
    void free() {
      log("object released");
      synchronized(lock) {
        if (timer != null) {
          timer.cancel();
          timer = null;
        }
        allocs.remove(id);
        if (worker != null) {
          worker.active = false;
          worker = null;
        }
        if (ds != null) {
          ds.close();
          ds = null;
        }
      }
    }
    void log(String msg) {
      JFLog.log(id + ":" + msg);
    }
  }

  private HashMap<String, Alloc> allocs = new HashMap<String, Alloc>();
  private Object allocsLock = new Object();

  private void relayTurn(String ip, int port, byte data[], ByteBuffer bb) throws Exception {
    String id = ip + ":" + port;
    Alloc alloc = getAlloc(id);
    if (alloc == null) {JFLog.log("Unknown client:" + id); return;}
    short channel = bb.getShort(0);
    short length = bb.getShort(2);
    if (channel != alloc.channel) {alloc.log("relayTurn:channel mismatch"); return;}
    DatagramPacket dp = new DatagramPacket(data, 4, length);
    dp.setAddress(alloc.relayaddr);
    dp.setPort(alloc.relayport);
//    alloc.log("relayTurn to:" + alloc.relayaddr.getHostAddress() + ":" + alloc.relayport);
    alloc.ds.send(dp);
  }

  private Alloc getAlloc(String ip, int port, InetAddress addr) {
    String id = ip + ":" + port;
    Alloc alloc;
    synchronized(allocsLock) {
      alloc = allocs.get(id);
      if (alloc != null) return alloc;
      alloc = new Alloc();
      alloc.ip = ip;
      alloc.port = port;
      alloc.id = id;
      alloc.addr = addr;
      allocs.put(id, alloc);
    }
    return alloc;
  }

  private Alloc getAlloc(String id) {
    Alloc alloc;
    synchronized(allocsLock) {
      alloc = allocs.get(id);
    }
    return alloc;
  }

  private char chars[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8' ,'9', 'a', 'b', 'c', 'd', 'e', 'f'};

  private String nonceRandom() {
    Random r = new Random();
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<20;a++) {
      sb.append(chars[r.nextInt(16)]);
    }
    return sb.toString();
  }

  private int min = 10000, max = 20000, next = 10000;

  public void setPortRange(int min, int max) {
    this.min = min;
    this.max = max;
    this.next = min;
  }

  private synchronized int getNextPort() {
    int port = next++;
    next++;  //odd port to reserve
    if (next >= max) next = min;
    return port;
  }

  private boolean isClassicStun(long id1) {
    return ((id1 >>> 32) != 0x2112a442);
  }

  private HashMap<String, String> tokens = new HashMap<String, String>();
  private int nextToken = 0;

  private synchronized String allocToken(String id) {
    String token = String.format("%08x", nextToken++);  //token MUST be 8 bytes
    if (nextToken < 0) nextToken = 0;
    tokens.put(token, id);
    return token;
  }

  private String getTokenID(String token) {
    String id = tokens.get(token);
    tokens.remove(token);
    return id;
  }

  private class Worker extends Thread {
    public void run() {
      DatagramPacket dp;
      byte data[] = new byte[1500];
      ByteBuffer bb = ByteBuffer.wrap(data);
      bb.order(ByteOrder.BIG_ENDIAN);
      int change_request_flgs;
      String realm, nonce;
      int lifetime;
      InetAddress remoteAddr, localAddr;
      String remoteip, localip;
      int remoteip_int, remotePort;
      int localip_int, localPort;
      boolean auth, evenPort, username;
      String resToken;
      String f[];
      Alloc alloc;
      int relayip, relayport, channel;
      InetAddress relayaddr;
      byte ip4[] = new byte[4];

      if (publicip != null) {
        localip = publicip;
      } else {
        localAddr = ds.getInetAddress();
        if (localAddr != null) {
          localip = localAddr.getHostAddress();
          if (localip.indexOf(":") != -1) localip = "127.0.0.1";  //IP6
        } else {
          localip = "127.0.0.1";
        }
      }
      f = localip.split("[.]");
      localip_int = 0;
      for(int a=0;a<4;a++) {
        localip_int <<= 8;
        localip_int += JF.atoi(f[a]);
      }
      localPort = ds.getPort();

      while (active) {
        try {
          dp = new DatagramPacket(data, 1500);
          ds.receive(dp);
          remoteAddr = dp.getAddress();
          remoteip = remoteAddr.getHostAddress();
          if (remoteip.indexOf(":") != -1) continue;  //IP6
          f= remoteip.split("[.]");
          remoteip_int = 0;
          for(int a=0;a<4;a++) {
            remoteip_int <<= 8;
            remoteip_int += JF.atoi(f[a]);
          }
          remotePort = dp.getPort();
          int packetLength = dp.getLength();
          //reset all values
          change_request_flgs = 0;
          realm = "";
          nonce = "";
          lifetime = defaultLifeTime;
          auth = false;
          alloc = null;
          resToken = null;
          evenPort = false;
          relayip = -1;
          relayport = -1;
          relayaddr = null;
          username = false;
          channel = -1;
          //decode response
          int offset = 0;
          short code = bb.getShort(0);
          if (code >= 0x4000 && code <= 0x7fff) {
            //it's TURN data
            relayTurn(remoteip, remotePort, data, bb);
            continue;
          }
          if (code == DATA_INDICATION) {
            continue;
          }
          offset += 2;
          int lengthOffset = offset;
          short length = bb.getShort(offset);
          if (length + 20 != packetLength) {
            throw new Exception("STUN:bad packet:incorrect length");
          }
          offset += 2;
          long id1 = bb.getLong(offset);
          offset += 8;
          long id2 = bb.getLong(offset);
          offset += 8;
          while (offset < packetLength) {
            short attr = bb.getShort(offset);
            offset += 2;
            length = bb.getShort(offset);
            offset += 2;
            switch (attr) {
              case USERNAME:
                if (user == null) break;
                username = (new String(data, offset, length).equals(user));
                break;
              case REALM:
                realm = new String(data, offset, length);
                break;
              case NONCE:
                nonce = new String(data, offset, length);
                break;
              case LIFETIME:
                lifetime = bb.getInt(offset);
                break;
              case CHANGE_REQUEST:
                change_request_flgs = bb.getInt(offset);
                break;
              case EVEN_PORT:
                evenPort = true;
                break;
              case CHANNEL_NUMBER:
                channel = bb.getShort(offset);  //0x4000 - 0x7fff
                if (channel < 0x4000) channel = -1;
                break;
              case RESERVATION_TOKEN:
                resToken = new String(Arrays.copyOfRange(data, offset, offset + length));
                break;
              case MESSAGE_INTEGRITY:
                alloc = getAlloc(remoteip, remotePort, remoteAddr);
                if (nonce == null) {alloc.log("!nonce"); break;}
                if (realm == null) {alloc.log("!realm"); break;}
                if (alloc.nonce == null) {alloc.log("!alloc.nonce"); break;}
                if (!nonce.equals(alloc.nonce)) {alloc.log("nonce mismatch"); break;}
                if (!username) {alloc.log("!username"); break;}
                bb.putShort(lengthOffset, (short)(offset));  //patch length
                byte correct[] = calcMsgIntegrity(data, offset - 4);
                byte supplied[] = Arrays.copyOfRange(data, offset, offset+20);
                auth = Arrays.equals(correct, supplied);
//                logKey(correct);
//                logKey(supplied);
                break;
              case XOR_PEER_ADDRESS:
                relayport = (bb.getShort(offset + 2) ^ bb.getShort(4)) & 0xffff;
                relayip = (bb.getInt(offset + 4) ^ bb.getInt(4));
                ip4[0] = (byte)((relayip & 0xff000000) >>> 24);
                ip4[1] = (byte)((relayip & 0xff0000) >> 16);
                ip4[2] = (byte)((relayip & 0xff00) >> 8);
                ip4[3] = (byte)(relayip & 0xff);
                relayaddr = InetAddress.getByAddress(ip4);
                break;
            }
            offset += length;
            if ((length & 0x3) > 0) {
              offset += 4 - (length & 0x3);  //padding
            }
          }
          switch (code) {
            case BINDING_REQUEST:
              JFLog.log(remoteip + ":" + remotePort + ":BINDING_REQUEST");
              offset = 0;
              bb.putShort(offset, BINDING_RESPONSE);
              offset += 2;
              bb.putShort(offset, (short)12);  //length
              offset += 2;
              bb.putLong(offset, id1);
              offset += 8;
              bb.putLong(offset, id2);
              offset += 8;

              if (isClassicStun(id1)) {
                bb.putShort(offset, MAPPED_ADDRESS);
                offset += 2;
                bb.putShort(offset, (short)8);  //length of attr
                offset += 2;
                bb.put(offset, (byte)0);  //reserved
                offset++;
                bb.put(offset, (byte)1);  //IP family
                offset++;
                bb.putShort(offset, (short)remotePort);
                offset += 2;
                bb.putInt(offset, remoteip_int);
                offset += 4;
              } else {
                //use XOR_MAPPED_ADDRESS instead
                bb.putShort(offset, XOR_MAPPED_ADDRESS);
                offset += 2;
                bb.putShort(offset, (short)8);  //length of attr
                offset += 2;
                bb.put(offset, (byte)0);  //reserved
                offset++;
                bb.put(offset, (byte)1);  //IP family
                offset++;
                bb.putShort(offset, (short)(remotePort ^ bb.getShort(4)));
                offset += 2;
                bb.putInt(offset, remoteip_int ^ bb.getInt(4));
                offset += 4;
              }

              dp = new DatagramPacket(data, offset);
              dp.setAddress(remoteAddr);
              dp.setPort(remotePort);
              if ((change_request_flgs & 0x02) == 0x02) {
                //different port
                ds2.send(dp);
              } else {
                //same port
                ds.send(dp);
              }
              break;
            case ALLOCATE_REQUEST:
              alloc = getAlloc(remoteip, remotePort, remoteAddr);
              alloc.log("ALLOCATE_REQUEST");
              if (!auth) {
                sendError(alloc, ALLOCATE_FAILED, id1, id2, remoteAddr, remotePort, data, bb);
              } else {
                if (resToken != null) {
                  if (evenPort) throw new Exception("EVEN_PORT with RESERVATION_TOKEN");
                  Alloc evenAlloc = getAlloc(getTokenID(resToken));
                  if (evenAlloc == null) throw new Exception("Reservation not found");
                  if (!evenAlloc.evenPort) throw new Exception("Odd Port was not reserved");
                  alloc.ds = new DatagramSocket(evenAlloc.ds.getLocalPort() + 1);
                } else {
                  alloc.ds = new DatagramSocket(getNextPort());
                }
                alloc.evenPort = evenPort;

                offset = 0;
                bb.putShort(offset, ALLOCATE_RESPONSE);
                offset += 2;
                lengthOffset = offset;
                bb.putShort(offset, (short)0);  //length (patch)
                offset += 2;
                bb.putLong(offset, id1);
                offset += 8;
                bb.putLong(offset, id2);
                offset += 8;

                if (evenPort) {
                  String token = allocToken(remoteip + ":" + remotePort);
                  bb.putShort(offset, RESERVATION_TOKEN);
                  offset += 2;
                  bb.putShort(offset, (short)token.length());  //length of attr
                  offset += 2;
                  System.arraycopy(token.getBytes(), 0, data, offset, token.length());
                  offset += token.length();
                }

                bb.putShort(offset, XOR_RELAY_ADDRESS);
                offset += 2;
                bb.putShort(offset, (short)8);  //length of attr
                offset += 2;
                bb.put(offset, (byte)0);  //reserved
                offset++;
                bb.put(offset, (byte)1);  //IP family
                offset++;
                bb.putShort(offset, (short)(alloc.ds.getLocalPort() ^ bb.getShort(4)));
                offset += 2;
                bb.putInt(offset, localip_int ^ bb.getInt(4));
                offset += 4;

                bb.putShort(lengthOffset, (short)(offset - 20));  //length (patch)

                dp = new DatagramPacket(data, offset);
                dp.setAddress(remoteAddr);
                dp.setPort(remotePort);
                ds.send(dp);

                alloc.setTimeout(defaultLifeTime);
              }
              break;
            case REFRESH_REQUEST:
              alloc = getAlloc(remoteip, remotePort, remoteAddr);
              alloc.log("REFRESH_REQUEST");
              if (!auth) {
                sendError(alloc, REFRESH_FAILED, id1, id2, remoteAddr, remotePort, data, bb);
              } else {
                if (lifetime != 0) {
                  alloc.setTimeout(defaultLifeTime);
                }
              }
              offset = 0;
              bb.putShort(offset, REFRESH_RESPONSE);
              offset += 2;
              bb.putShort(offset, (short)8);  //length
              offset += 2;
              bb.putLong(offset, id1);
              offset += 8;
              bb.putLong(offset, id2);
              offset += 8;

              bb.putShort(offset, LIFETIME);
              offset += 2;
              bb.putShort(offset, (short)4);  //length of attr
              offset += 2;
              bb.putInt(offset, lifetime);
              offset += 4;

              dp = new DatagramPacket(data, offset);
              dp.setAddress(remoteAddr);
              dp.setPort(remotePort);
              ds.send(dp);
              if (lifetime == 0) alloc.free();
              break;
            case BIND_REQUEST:
              alloc = getAlloc(remoteip, remotePort, remoteAddr);
              alloc.log("BIND_REQUEST");
              if (!auth || channel == -1) {
                sendError(alloc, BIND_FAILED, id1, id2, remoteAddr, remotePort, data, bb);
              } else {
                synchronized(alloc.lock) {
                  alloc.setTimeout(defaultLifeTime);  //refresh all
                  alloc.relayip = relayip;
                  alloc.relayaddr = relayaddr;
                  alloc.relayport = relayport;
                  alloc.channel = (short)channel;
                  if (alloc.worker == null) {
                    alloc.worker = new RelayWorker(alloc);
                    alloc.worker.start();
                  }
                }
              }
              offset = 0;
              bb.putShort(offset, BIND_RESPONSE);
              offset += 2;
              bb.putShort(offset, (short)8);  //length
              offset += 2;
              bb.putLong(offset, id1);
              offset += 8;
              bb.putLong(offset, id2);
              offset += 8;

              bb.putShort(offset, LIFETIME);
              offset += 2;
              bb.putShort(offset, (short)4);  //length of attr
              offset += 2;
              bb.putInt(offset, defaultLifeTime);
              offset += 4;

              dp = new DatagramPacket(data, offset);
              dp.setAddress(remoteAddr);
              dp.setPort(remotePort);
              ds.send(dp);
              break;
            default:
              JFLog.log(remoteip + ":" + remotePort + ":Unknown request:" + code);
              break;
          }
        } catch (Exception e) {
          if (active) JFLog.log(e);
        }
      }
      done = true;
    }
  }

  private void logKey(byte key[]) {
    StringBuilder log = new StringBuilder();
    for(int a=0;a<key.length;a++) {
      int b = ((int)key[a]) & 0xff;
      if (b < 0x10) log.append("0");
      log.append(Integer.toString(b, 16));
    }
    JFLog.log("key=" + log);
  }

  private class AllocTask extends TimerTask {
    Alloc alloc;
    public AllocTask(Alloc alloc) {
      this.alloc = alloc;
    }
    public void run() {
      long now = System.currentTimeMillis();
      synchronized(alloc.lock) {
        if (alloc.timeout < now) {
          alloc.log("object expired");
          alloc.free();
        }
      }
    }
  }

  private void sendError(Alloc alloc, short code, long id1, long id2, InetAddress remoteAddr, int remotePort, byte data[], ByteBuffer bb) throws Exception {
    int offset, lengthOffset;
    alloc.nonce = nonceRandom();
    offset = 0;
    bb.putShort(offset, code);
    offset += 2;
    lengthOffset = offset;
    bb.putShort(offset, (short)0);  //length (patch)
    offset += 2;
    bb.putLong(offset, id1);
    offset += 8;
    bb.putLong(offset, id2);
    offset += 8;

    bb.putShort(offset, ERROR_CODE);
    offset += 2;
    bb.putShort(offset, (short)4);  //length of attr
    offset += 2;
    bb.putInt(offset, 0x401);
    offset += 4;

    bb.putShort(offset, REALM);
    offset += 2;
    bb.putShort(offset, (short)realm.length());
    offset += 2;
    System.arraycopy(realm.getBytes(), 0, data, offset, realm.length());
    offset += realm.length();
    if ((offset & 0x3) > 0) {
      offset += 4 - (offset & 0x3);  //padding
    }

    bb.putShort(offset, NONCE);
    offset += 2;
    bb.putShort(offset, (short)alloc.nonce.length());
    offset += 2;
    System.arraycopy(alloc.nonce.getBytes(), 0, data, offset, alloc.nonce.length());
    offset += alloc.nonce.length();

    bb.putShort(lengthOffset, (short)(offset - 20));  //length (patch)

    DatagramPacket dp = new DatagramPacket(data, offset);
    dp.setAddress(remoteAddr);
    dp.setPort(remotePort);
    ds.send(dp);
  }

  private class RelayWorker extends Thread {
    Alloc alloc;
    public volatile boolean active = true;
    private static final int mtu = 1500 - 20 - 8;  //20=IP 8=UDP
    public RelayWorker(Alloc alloc) {
      this.alloc = alloc;
    }
    public void run() {
      //read packets from relay socket and send to owner
      byte data[] = new byte[mtu + 4];
      ByteBuffer bb = ByteBuffer.wrap(data);
      bb.order(ByteOrder.BIG_ENDIAN);
      bb.putShort(0, alloc.channel);
//      alloc.log("Starting Worker:SendTo:" + alloc.addr.getHostAddress() + ":" + alloc.port + ",From:" + alloc.ds.getLocalPort() + ",channel=" + Integer.toString(alloc.channel, 16));
      while (active) {
        try {
          DatagramPacket indp = new DatagramPacket(data, 4, mtu);
          alloc.ds.receive(indp);
          int len = indp.getLength();
          bb.putShort(2, (short)len);
          DatagramPacket outdp = new DatagramPacket(data, 0, len + 4);
          outdp.setAddress(alloc.addr);
          outdp.setPort(alloc.port);
          ds.send(outdp);
        } catch (Exception e) {
          if (active) JFLog.log(e);
        }
      }
    }
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public class JBusMethods {
    public void getConfig(String pack) {
      busClient.call(pack, "getConfig", busClient.quote(busClient.encodeString(config)));
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
      stun.close();
      stun = new STUN();
      stun.start();
    }
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33006;
    } else {
      return 777;
    }
  }

  public static void main(String args[]) {
    serviceStart(args);
  }

  //Win32 Service

  private static STUN stun;

  public static void serviceStart(String args[]) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    stun = new STUN();
    stun.start();
  }

  public static void serviceStop() {
    JFLog.log("Stopping service");
    busServer.close();
    stun.close();
  }
}
