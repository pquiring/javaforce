package javaforce.voip;

import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Handles the client end of a RTSP link.
 */

public class RTSPClient extends RTSP implements RTSPInterface, STUN.Listener {

  public enum NAT {None, STUN, TURN, ICE};

  private String remotehost, remoteip;
  private InetAddress remoteaddr;
  private int remoteport;
  private String user;  //username
  private String pass;  //password
  private RTSPClientInterface iface;
  private String localhost;
  private int localport;
  private static NAT nat = NAT.None;
  private static boolean useNATOnPrivateNetwork = false;  //do not use NATing techniques on private network servers
  private static String stunHost, stunUser, stunPass;
  private RTSPSession sess;

  public Object userobj;  //user definable
  public int expires;  //expires

  /**
   * Returns the registered user name.
   */
  public String getUser() {
    return user;
  }

  /**
   * Returns the remote host.
   */
  public String getRemoteHost() {
    return remotehost;
  }

  public String getRemoteIP() {
    return remoteip;
  }

  /**
   * Initialize this instance for RTSP.<br>
   *
   * @param remotehost,remoteport is the RTSP Server/Proxy address.<br>
   * @param localport is the UDP port to bind to locally.<br>
   * @param iface must be a RTSPClientInterface where RTSP events are dispatched
   * to.<br>
   */
  public boolean init(String remotehost, int remoteport, int localport, RTSPClientInterface iface, TransportType type) {
    this.iface = iface;
    this.localport = localport;
    this.remoteport = remoteport;
    this.remotehost = remotehost;
    this.remoteip = resolve(remotehost);
    try {
      this.remoteaddr = InetAddress.getByName(remoteip);
      if (nat == NAT.STUN || nat == NAT.ICE) {
        if (!startSTUN()) return false;
      }
      findlocalhost();
      JFLog.log(log, "localhost = " + localhost + " for remotehost = " + remotehost);
      if (this.remotehost.equals("127.0.0.1")) {
        this.remotehost = localhost;
        remoteip = resolve(this.remotehost);
        JFLog.log(log, "changed 127.0.0.1 to " + this.remotehost + " " + this.remoteip);
      }
      if (nat == NAT.STUN || nat == NAT.ICE) {
        stopSTUN();
      }
      sess = new RTSPSession(localhost);
      return super.init(localhost, localport, this, false, type);
    } catch (Exception e) {
      if (stun != null) stopSTUN();
      JFLog.log(log, e);
      return false;
    }
  }

  /**
   * Free all resources.
   */
  public void uninit() {
    super.uninit();
  }

  /**
   * Sets the type of NAT traversal type (global setting).
   */
  public static void setNAT(NAT nat, String host, String user, String pass) {
    RTSPClient.nat = nat;
    stunHost = host;
    stunUser = user;
    stunPass = pass;
  }

  /**
   * Disable/enable use of NAT traversal on private networks (global setting)
   * Private networks : 192.168.x.x , 10.x.x.x , 172.[16-31].x.x
   */

  public static void useNATOnPrivateNetwork(boolean state) {
    useNATOnPrivateNetwork = state;
  }

  private String cleanString(String in) {
    return in.replaceAll("\"", "");
  }

  /**
   * Send an empty RTSP message to server. This should be done periodically to
   * keep firewalls open. Most routers close UDP connections after 60 seconds.
   * Not sure if needed with TCP/TLS but is done anyways.
   */
  public void keepalive(String url) {
    get_parameter(RTSPURL.cleanURL(url), null);
  }

  /**
   * Determine if server is on a local private network.
   */
  public static boolean isPrivateNetwork(String ip) {
    //in case your PBX is on your own local IP network
    //see http://en.wikipedia.org/wiki/Private_network
    if (ip.startsWith("192.168.")) {
      return true;
    }
    if (ip.startsWith("10.")) {
      return true;
    }
    if (ip.startsWith("169.254.")) {
      return true;
    }
    for(int a=16;a<=31;a++) {
      if (ip.startsWith("172." + a + ".")) return true;
    }
    return false;
  }

  /**
   * Returns local RTP IP address.
   */
  public String getlocalRTPhost(RTSPSession sess) {
    if (RTP.useTURN)
      return RTP.getTurnIP();
    else
      return sess.localhost;
  }

  private boolean findlocalhost_webserver(String host) {
    //this returns your local ip (more accurate on multi-homed systems than java api)
    Socket s;
    try {
      s = new Socket();
      s.connect(new InetSocketAddress(host, 80), 1000);
      localhost = s.getLocalAddress().getHostAddress();
      try { s.close(); } catch (Exception e) {}
      JFLog.log(log, "Detected IP connecting to WebServer at " + host);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private STUN stun;
  private final Object stunLock = new Object();
  private volatile boolean stunWaiting = false;
  private volatile boolean stunResponse = false;
  private boolean startSTUN() {
    stun = new STUN();
    if (!stun.start(localport, stunHost, stunUser, stunPass, this)) return false;
    return true;
  }

  private void stopSTUN() {
    if (stun == null) return;
    stun.close();
    stun = null;
  }

  //interface STUN.Listener
  public void stunPublicIP(STUN stun, String ip, int port) {
    synchronized(stunLock) {
      if (stunWaiting) {
//        localhost = ip;
        stunResponse = true;
        stunLock.notify();
      }
    }
  }
  public void turnAlloc(STUN stun, String ip, int port, byte[] token, int lifetime) {}
  public void turnBind(STUN stun) {}
  public void turnRefresh(STUN stun, int lifetime) {}
  public void turnFailed(STUN stun) {}
  public void turnData(STUN stun, byte[] data, int offset, int length, short channel) {}

  private boolean findlocalhost_stun() {
    stunResponse = false;
    stunWaiting = true;
    synchronized(stunLock) {
      stun.requestPublicIP();
      try {stunLock.wait(1000);} catch (Exception e) {JFLog.log(log, e);}
      stunWaiting = false;
    }
    if (stunResponse) {
      JFLog.log(log, "Detected IP using STUN");
    }
    return stunResponse;
  }

  private boolean findlocalhost_java() {
    //this only detects your local IP, not your internet ip
    //not accurate on multi-homed systems
    try {
      InetAddress local = InetAddress.getLocalHost();
      localhost = local.getHostAddress();
      JFLog.log(log, "Detected IP using Java:" + localhost);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Determines local IP address. Method depends on NAT traversal type selected.
   */
  private void findlocalhost() {
    JFLog.log(log, "Detecting localhost for remotehost = " + remotehost);
    if (useNATOnPrivateNetwork || !isPrivateNetwork(remoteip)) {
      if (nat == NAT.STUN || nat == NAT.ICE) {
        if (findlocalhost_stun()) return;
        JFLog.log(log, "RTSP:STUN:Failed");
      }
    }
    //try connecting to remotehost on webserver port
    if (findlocalhost_webserver(remotehost)) return;
    //use java (returns local ip, not internet ip) (not reliable on multi-homed systems)
    if (findlocalhost_java()) return;
    //if all else fails (use a dummy DHCP failure IP)
    Random r = new Random();
    localhost = "169.254." + r.nextInt(256) + "." + r.nextInt(256);
  }

  /**
   * Issues a command to the RTSP server.
   */
  private boolean issue(RTSPSession sess, String cmd) {
    JFLog.log(log, "issue command : " + cmd + " from : " + user + " to : " + remotehost + ":" + sess);
    sess.remotehost = remoteip;
    sess.remoteport = remoteport;
    sess.cmd = cmd;
    StringBuilder req = new StringBuilder();
    StringBuilder post = new StringBuilder();
    req.append(cmd + " " + sess.uri + sess.extra + " RTSP/1.0\r\n");
    req.append("Cseq: " + sess.cseq++ + "\r\n");
    req.append("User-Agent: " + useragent + "\r\n");
    if (sess.epass != null) {
      req.append(sess.epass);
    }
    if (sess.transport != null) {
      req.append(sess.transport);
    }
    if (sess.accept != null) {
      req.append("Accept: " + sess.accept + "\r\n");
    }
    if (sess.id != -1) {
      req.append("Session: " + sess.id + "\r\n");
    }
    if (sess.params != null) {
      for(String param : sess.params) {
        post.append(param);
        post.append("\r\n");
      }
      req.append("Content-Type: text/parameters\r\n");
      req.append("Content-Length: " + post.length() + "\r\n");
    }
    req.append("\r\n");
    if (sess.params != null) {
      req.append(post);
    }
    return send(remoteaddr, remoteport, req.toString());
  }

  /*
   * Set user/pass used for RTSP only.
   */
  public void setUserPass(String user, String pass) {
    this.user = user;
    this.pass = pass;
  }

  /**
   * Send OPTIONS request to server.
   */
  public boolean options(String url) {
    sess.uri = RTSPURL.cleanURL(url);
    sess.extra = "";
    return issue(sess, "OPTIONS");
  }

  /**
   * Send DESCRIBE request to server.
   */
  public boolean describe(String url) {
    sess.uri = RTSPURL.cleanURL(url);
    sess.extra = "";
    sess.accept = "application/sdp";
    boolean result = issue(sess, "DESCRIBE");
    sess.accept = null;
    return result;
  }

  /**
   * Send SETUP request to server (RTSP).
   */
  public boolean setup(String url, int localrtpport, int trackid) {
    sess.transport = "Transport: RTP/AVP;unicast;client_port=" + localrtpport + "-" + (localrtpport+1) + "\r\n";
    sess.uri = RTSPURL.cleanURL(url);
    sess.extra = "/trackid=" + trackid;
    boolean result = issue(sess, "SETUP");
    sess.transport = null;
    return result;
  }

  /**
   * Send PLAY request to server (RTSP).
   */
  public boolean play(String url) {
    sess.uri = RTSPURL.cleanURL(url);
    sess.extra = "/";
    return issue(sess, "PLAY");
  }

  /**
   * Send TEARDOWN request to server (RTSP).
   */
  public boolean teardown(String url) {
    if (sess == null) return false;
    sess.uri = RTSPURL.cleanURL(url);
    sess.extra = "/";
    return issue(sess, "TEARDOWN");
  }

  /**
   * GET_PARAMETER (RTSP) Used as a keepalive.
   */
  public boolean get_parameter(String url, String[] params) {
    sess.uri = RTSPURL.cleanURL(url);
    sess.extra = "/";
    sess.params = params;
    boolean result = issue(sess, "GET_PARAMETER");
    sess.params = null;
    return result;
  }

  // other unsupported commands: ANNOUNCE, PAUSE, SET_PARAMETER, REDIRECT, RECORD

  /**
   * Processes RTSP messages sent from the RTSP server.
   */
  public void onPacket(RTSP rtsp, String[] msg, String remoteip, int remoteport) {
    try {
      if (!remoteip.equals(this.remoteip) || remoteport != this.remoteport) {
        JFLog.log(log, "Ignoring packet from unknown host:" + remoteip + ":" + remoteport);
        return;
      }
      String req = null;
      if (remoteip.equals("127.0.0.1")) {
        remoteip = sess.localhost;
      }
      sess.remotehost = remoteip;
      sess.remoteport = remoteport;
      sess.remotecseq = getcseq(msg);
      String sid = getHeader("Session:", msg);
      if (sid != null) {
        int idx = sid.indexOf(';');
        if (idx != -1) {
          sid = sid.substring(0, idx);
        }
        sess.id = Long.valueOf(sid);
      }

      int type = getResponseType(msg);
      if (type != -1) {
        JFLog.log(log, "reply=" + type + ":" + sess);
      } else {
        req = getRequest(msg);
        sess.uri = getURI(msg);
        JFLog.log(log, "request=" + req + ":" + sess);
      }
      switch (type) {
        case 200:
          if (sess.cmd.equals("OPTIONS")) {
            iface.onOptions(this);
          } else if (sess.cmd.equals("DESCRIBE")) {
            iface.onDescribe(this, getSDP(msg));
          } else if (sess.cmd.equals("SETUP")) {
            iface.onSetup(this);
          } else if (sess.cmd.equals("PLAY")) {
            iface.onPlay(this);
          } else if (sess.cmd.equals("TEARDOWN")) {
            iface.onTeardown(this);
          } else if (sess.cmd.equals("GET_PARAMETER")) {
            iface.onGetParameter(this, HTTP.getContent(msg));
          }
          break;
        case 401:
        case 407:
          if (sess.authsent) {
            JFLog.log(log, "Server Error : Double " + type);
          } else {
            sess.authstr = getHeader("WWW-Authenticate:", msg);
            if (sess.authstr == null) {
              sess.authstr = getHeader("Proxy-Authenticate:", msg);
            }
            if (sess.authstr == null) {
              JFLog.log(log, "err:401/407 without Authenticate tag");
              break;
            }
            sess.epass = getAuthResponse(sess, user, pass, remotehost, sess.cmd, (type == 401 ? "Authorization:" : "Proxy-Authorization:"));
            if (sess.epass == null) {
              JFLog.log(log, "err:gen auth failed");
              break;
            }
            issue(sess, sess.cmd);
            sess.authsent = true;
          }
          break;
        default:
          //treat all other codes as a cancel
          sess.epass = null;
          break;
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public void onConnect(RTSP rtsp, String remoteip, int remoteport) {
  }

  public void onDisconnect(RTSP rtsp, String remoteip, int remoteport) {
  }

  public String toString() {
    return "RTSPClient:{remote=" + remotehost + ":" + remoteport + "}";
  }
}
