package javaforce.voip;

import java.net.*;
import java.util.*;

import javaforce.*;

/** RTP (Real-Time Transport Protocol)
 * Handles sending/receiving RTP packets.
 */

public class RTP implements STUN.Listener {
  private static int nextlocalrtpport = 32768;
  private static int rtpmin = 32768;
  private static int rtpmax = 65536;
  protected DatagramSocket sock1, sock2;
  private Worker worker1, worker2;  //inbound Workers
  private int localrtpport;
  public volatile boolean active = false;
  protected RTPInterface iface;
  private int mtu = 1500;  //max size of packet
  protected boolean rawMode;
  public ArrayList<RTPChannel> channels = new ArrayList<RTPChannel>();
  public static long now = 0;  //this is copied into each RTPChannel as it receives packets
  private static boolean hasBouncyCastle;
  public int log;

  //TURN related data
  protected static boolean useTURN = false;
  protected static String turnHost, turnIP, turnUser, turnPass;
  protected STUN stun1, stun2;
  protected byte turnToken[];
  protected long turnAllocExpires;

  public Object userobj;  //free to use
  public static boolean debug = false;  //set to true and recompile to get a lot of output
  public final static Codec CODEC_UNKNOWN = new Codec("?", -1);
  public final static Codec CODEC_G711u = new Codec("PCMU", 0);  //patent expired
  public final static Codec CODEC_GSM = new Codec("GSM", 3);  //patent expired
  public final static Codec CODEC_G711a = new Codec("PCMA", 8);  //patent expired
  public final static Codec CODEC_G722 = new Codec("G722", 9);  //patent expired
  public final static Codec CODEC_G729a = new Codec("G729", 18);  //patent encumbered
  public final static Codec CODEC_JPEG = new Codec("JPEG", 26);  //public domain
  public final static Codec CODEC_H263 = new Codec("H263", 34);  //patent expired
  //dynamic ids (96-127)
  public final static Codec CODEC_VP8 = new Codec("VP8", 96);  //open source (Google!)
  public final static Codec CODEC_H264 = new Codec("H264", 97);  //patent encumbered
  public final static Codec CODEC_H263_1998 = new Codec("H263-1998", 98);  //patent expired
  public final static Codec CODEC_H263_2000 = new Codec("H263-2000", 99);  //patent expired
  public final static Codec CODEC_RFC2833 = new Codec("telephone-event", 100);
  public final static Codec CODEC_H265 = new Codec("H265", 101);  //patent encumbered

  static {
    try {
      Class.forName("org.bouncycastle.tls.TlsServer");
      hasBouncyCastle = true;
    } catch (Exception e) {
      JFLog.log("Warning:BouncyCastle not found, SRTP/DTLS not available");
    }
  }

  public void setLog(int id) {
    log = id;
  }

  public static void enableTURN(String host, String user, String pass) {
    useTURN = true;
    turnHost = host;
    turnUser = user;
    turnPass = pass;
  }

  public static void disableTURN() {
    useTURN = false;
  }

  public static synchronized int getnextlocalrtpport() {
    int ret = nextlocalrtpport;
    nextlocalrtpport += 2;
    if (nextlocalrtpport + 1 > rtpmax) {
      nextlocalrtpport = rtpmin;
    }
    return ret;
  }

  public int getlocalrtpport() {
    if (useTURN) {
      return stun1.getPort();
    } else {
      return localrtpport;
    }
  }

  private boolean turnSuccess;
  private boolean turnFailed;
  protected volatile RTPChannel bindingChannel;
  protected final Object bindLock = new Object();

  //public interface STUN.Listener
  public void stunPublicIP(STUN stun, String ip, int port) {}
  public void turnAlloc(STUN stun, String ip, int port, byte token[], int lifetime) {
    turnToken = token;
    turnSuccess = true;
    turnIP = ip;  //static - should be the same for all allocations
    //lifetime = 600 (typically)
    turnAllocExpires = System.currentTimeMillis() + (lifetime * 1000);
  }
  public void turnBind(STUN stun) {
    turnSuccess = true;
    //not sure why Xlite does rebind early (RFC says 10mins)
    bindingChannel.turnBindExpires = System.currentTimeMillis() + (300 * 1000);
  }
  public void turnRefresh(STUN stun, int lifetime) {
    turnAllocExpires = System.currentTimeMillis() + (lifetime * 1000);
  }
  public void turnFailed(STUN stun) {
    turnFailed = true;
  }
  public void turnData(STUN stun, byte data[], int offset, int length, short turnChannel) {
    RTPChannel channel = findChannel(turnChannel);
    if (channel == null) return;
    if (stun == stun1) {
      channel.processRTP(data, offset, length);
    } else if (stun == stun2) {
      channel.processRTCP(data, offset, length);
    }
  }

  protected void wait4reset() {
    turnSuccess = false;
    turnFailed = false;
  }

  protected void wait4reply() throws Exception {
    for(int a=0;a<100;a++) {
      JF.sleep(10);
      if (turnFailed) throw new Exception("Turn failed");
      if (turnSuccess) return;
    }
    throw new Exception("Turn timeout");
  }

  /** For testing only */
  public boolean init(RTPInterface iface, int port) {
    this.iface = iface;
    try {
      localrtpport = port;
      if (useTURN) {
        //alloc TURN relay
        wait4reset();
        stun1 = new STUN();
        if (!stun1.start(localrtpport, turnHost, turnUser, turnPass, this)) throw new Exception("STUN init failed");
        stun1.requestAlloc(true, null);
        wait4reply();
        if (turnToken == null) throw new Exception("Turn token missing");
        JFLog.log(log, "RTP:TURN:host=" + stun1.getIP());
        JFLog.log(log, "RTP:TURN:port=" + stun1.getPort());
        wait4reset();
        stun2 = new STUN();
        if (!stun2.start(localrtpport + 1, turnHost, turnUser, turnPass, this)) throw new Exception("STUN init failed");
        stun2.requestAlloc(false, turnToken);
        wait4reply();
        JFLog.log(log, "RTP:TURN:host=" + stun2.getIP());
        JFLog.log(log, "RTP:TURN:port=" + stun2.getPort());
      } else {
        sock1 = new DatagramSocket(localrtpport);
        sock2 = new DatagramSocket(localrtpport + 1);
      }
      JFLog.log(log, "RTP:localport=" + localrtpport);
    } catch (Exception e2) {
      JFLog.log(log, e2);
      return false;
    }
    return true;
  }

  public boolean init(RTPInterface iface) {
    this.iface = iface;
    for(int a=0;a<5;a++) {
      try {
        localrtpport = getnextlocalrtpport();
        if (useTURN) {
          //alloc TURN relay
          wait4reset();
          stun1 = new STUN();
          if (!stun1.start(localrtpport, turnHost, turnUser, turnPass, this)) throw new Exception("STUN init failed");
          stun1.requestAlloc(true, null);
          wait4reply();
          if (turnToken == null) throw new Exception("Turn token missing");
          JFLog.log(log, "RTP:TURN:host=" + stun1.getIP());
          JFLog.log(log, "RTP:TURN:port=" + stun1.getPort());
          wait4reset();
          stun2 = new STUN();
          if (!stun2.start(localrtpport + 1, turnHost, turnUser, turnPass, this)) throw new Exception("STUN init failed");
          stun2.requestAlloc(false, turnToken);
          wait4reply();
          JFLog.log(log, "RTP:TURN:host=" + stun2.getIP());
          JFLog.log(log, "RTP:TURN:port=" + stun2.getPort());
        } else {
          sock1 = new DatagramSocket(localrtpport);
          sock2 = new DatagramSocket(localrtpport + 1);
          setReceiveBufferSize(16*1024*1024);  //default 64K drops video packets
        }
        JFLog.log(log, "RTP:localport=" + localrtpport);
      } catch (Exception e2) {
        JFLog.log(log, e2);
        continue;
      }
      return true;
    }
    return false;
  }

  public void setReceiveBufferSize(int bytes) {
    if (sock1 == null) return;
    try {
      sock1.setReceiveBufferSize(bytes);
      sock2.setReceiveBufferSize(bytes);
    } catch (Exception e) {}
  }

  /**
   * Sets maximum packet size. Must call before start(). (default = 1500)
   */
  public void setMTU(int mtu) {
    if (active) {
      return;
    }
    this.mtu = mtu;
  }

  Random r = new Random();
  protected short getNextTURNChannel() {
    return (short)(r.nextInt(0x7ffe - 0x4000) + 0x4000);
  }

  /**
   * Starts RTP session.
   */
  public boolean start() {
    now = System.currentTimeMillis();
    if (active) {
      return true;
    }
    active = true;
    if (!useTURN) {
      worker1 = new Worker(this, sock1, false);
      worker1.start();
      worker2 = new Worker(this, sock2, true);
      worker2.start();
    }
    return true;
  }

  /**
   * Stops RTP session and frees resources.
   */
  public void stop() {
    if (!active) {
      return;
    }
    active = false;
    closeSockets();
    freeWorkers();
    if (useTURN) {
      if (stun1 != null) {
        stun1.requestRefresh(0);
        stun1.close();
        stun1 = null;
      }
      if (stun2 != null) {
        stun2.requestRefresh(0);
        stun2.close();
        stun2 = null;
      }
    }
  }

  private void freeWorkers() {
    if (worker1 != null) {
      try {
        worker1.join();
      } catch (Exception e1) {
      }
      worker1 = null;
    }
    if (worker2 != null) {
      try {
        worker2.join();
      } catch (Exception e2) {
      }
      worker2 = null;
    }
  }

  /**
   * Frees resources.
   */
  public void uninit() {
    stop();
    closeSockets();
    freeWorkers();
  }

  private void closeSockets() {
    if (sock1 != null) {
      try {
        sock1.close();
      } catch (Exception e) {
      }
      sock1 = null;
    }
    if (sock2 != null) {
      try {
        sock2.close();
      } catch (Exception e) {
      }
      sock2 = null;
    }
  }

  /**
   * Create a new RTP channel with a random ssrc id.
   */
  public RTPChannel createChannel(SDP.Stream stream) {
    return createChannel(-1, stream);
  }

  /**
   * Create a new RTP channel with a specified ssrc id.
   */
  public RTPChannel createChannel(int ssrc, SDP.Stream stream) {
    JFLog.log(log, "RTP.createChannel()" + stream.getIP() + ":" + stream.port);
    RTPChannel channel = null;
    switch (stream.profile) {
      case AVP:
      case AVPF:
        channel = new RTPChannel(this, ssrc, stream);
        break;
      case SAVP:
      case SAVPF:
        if (!hasBouncyCastle) {
          JFLog.log(log, "RTP:Couldn't create SRTPChannel");
          return null;
        }
        channel = new SRTPChannel(this, ssrc, stream);
        break;
      case UNKNOWN:
        JFLog.log(log, "RTP:Can not create unknown profile");
        return null;
    }
    channel.setLog(log);
    channels.add(channel);
    return channel;
  }

  /** Remote RTPChannel */
  public void removeChannel(RTPChannel channel) {
    channels.remove(channel);
  }

  /**
   * Returns default RTP channel.
   */
  public RTPChannel getDefaultChannel() {
    if (channels.size() == 0) return null;
    return channels.get(0);
  }

  public RTPChannel findChannel(short turnChannel) {
    for(int a=0;a<channels.size();a++) {
      RTPChannel channel = channels.get(a);
      if (!channel.active) continue;
      if (channel.turn1ch == turnChannel) return channel;
      if (channel.turn2ch == turnChannel) return channel;
    }
    return null;
  }

  public RTPChannel findChannel(String ip, int port) {
    for(int a=0;a<channels.size();a++) {
      RTPChannel channel = channels.get(a);
      if (!channel.active) continue;
      if (channel.stream.getIP().equals(ip) && (channel.stream.port == -1 || channel.stream.port == port)) {
        return channel;
      }
    }
    return null;
  }

  /**
   * Sets global RTP port range to use (should be set before calling init()).
   */
  public static void setPortRange(int min, int max) {
    rtpmin = min;
    rtpmax = max;
    nextlocalrtpport = min;
  }

  /**
   * Reads inbound packets for RTP session.
   */
  private class Worker extends Thread {

    private RTP rtp;
    private DatagramSocket sock;
    private boolean rtcp;

    public Worker(RTP rtp, DatagramSocket sock, boolean rtcp) {
      this.rtp = rtp;
      this.sock = sock;
      this.rtcp = rtcp;
    }

    public void run() {
      byte data[] = new byte[rtp.mtu];
      while (rtp.active) {
        try {
          DatagramPacket pack = new DatagramPacket(data, rtp.mtu);
          sock.receive(pack);
          int len = pack.getLength();
          if (len < 12) {
            continue;
          }
          String remoteip = pack.getAddress().getHostAddress();
          int remoteport = pack.getPort();
          if (rtcp) {
            RTPChannel channel = rtp.findChannel(remoteip, remoteport-1);
            if (channel == null) {
              JFLog.log(log, "RTP:No channel found:" + remoteip + ":" + remoteport);
              continue;
            }
            channel.processRTCP(data, 0, len);
          } else {
            RTPChannel channel = rtp.findChannel(remoteip, remoteport);
            if (channel == null) {
              JFLog.log(log, "RTP:No channel found:" + remoteip + ":" + remoteport);
              continue;
            }
            if (channel.stream.port == -1) {
              channel.stream.port = remoteport;  //NATing
              JFLog.log(log, "RTP : NAT port = " + channel.stream.getPort());
            }
            channel.processRTP(data, 0, len);
          }
        } catch (SocketException e) {
          if (rtp.active) {
            JFLog.log(log, e);
          }
          rtp.active = false;
        } catch (Exception e) {
          JFLog.log(log, e);
          rtp.active = false;
        }
      }
    }
  }

  /** Keep Alive - call every 30 seconds to keep active. */
  public void keepalive() {
    if (!active) return;
    now = System.currentTimeMillis();
    //do refreshes a little sooner (75 seconds) (in case nonce changes)
    if (useTURN && (now + 75 * 1000) > turnAllocExpires) {
      //request another 10 mins
      stun1.requestRefresh(600);
      stun2.requestRefresh(600);
    }
    for(int a=0;a<channels.size();a++) {
      RTPChannel channel = channels.get(a);
      channel.keepalive(now);
    }
  }
  /**
   * Sets raw mode<br> Packets are not decoded, they are passed directly thru
   * RTPInterface.rtpPacket()<br> This is used by PBX to relay RTP packets
   * between call originator and terminator.<br>
   */
  public void setRawMode(boolean state) {
    this.rawMode = state;
  }

  /**
   * Returns current raw mode
   */
  public boolean getRawMode() {
    return rawMode;
  }

  public static String getTurnIP() {
    return turnIP;
  }

  public static String genIceufrag() {
    StringBuilder sb = new StringBuilder();
    Random r = new Random();
    for(int a=0;a<16;a++) {
      sb.append((char)('a' + r.nextInt(26)));
    }
    return sb.toString();
  }

  public static String genIcepwd() {
    StringBuilder sb = new StringBuilder();
    Random r = new Random();
    for(int a=0;a<16;a++) {
      sb.append((char)('a' + r.nextInt(26)));
    }
    return sb.toString();
  }

  public void setInterface(RTPInterface iface) {
    this.iface = iface;
  }

  public String toString() {
    int remoteport = -1;
    RTPChannel channel = getDefaultChannel();
    if (channel != null) {
      remoteport = channel.getremoteport();
    }
    return "RTP:{localport=" + localrtpport + ",remoteport=" + remoteport + ",mode=" + rawMode + "}";
  }
}
