package javaforce.voip;

import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Handles the client end of a SIP link.
 */

public class RTSPClient extends RTSP implements RTSPInterface, STUN.Listener {

  public enum NAT {None, STUN, TURN, ICE};

  private String remotehost, remoteip;
  private InetAddress remoteaddr;
  private int remoteport;
  private String name;  //display name (usually same as user)
  private String user;  //acct name (usually a phone number)
  private String auth;  //auth name (usually same as user)
  private String pass;  //password
  private RTSPClientInterface iface;
  private String localhost;
  private int localport, rport = -1;
  private static boolean use_received = true;
  private static boolean use_rport = true;
  private static NAT nat = NAT.None;
  private static boolean useNATOnPrivateNetwork = false;  //do not use NATing techniques on private network servers
  private static String stunHost, stunUser, stunPass;
  private RTSPSession sess;

  public Object rtmp;  //used by RTMP2SIPServer
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

  /**
   * Initialize this instance for RTSP.<br>
   *
   * @param remotehost,remoteport is the RTSP Server/Proxy address.<br>
   * @param localport is the UDP port to bind to locally.<br>
   * @param iface must be a RTSPClientInterface where RTSP events are dispatched
   * to.<br>
   */
  public boolean init(String remotehost, int remoteport, int localport, RTSPClientInterface iface, Transport type) {
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
      JFLog.log("localhost = " + localhost + " for remotehost = " + remotehost);
      if (this.remotehost.equals("127.0.0.1")) {
        this.remotehost = localhost;
        remoteip = resolve(this.remotehost);
        JFLog.log("changed 127.0.0.1 to " + this.remotehost + " " + this.remoteip);
      }
      if (nat == NAT.STUN || nat == NAT.ICE) {
        stopSTUN();
      }
      sess = new RTSPSession(localhost);
      return super.init(localport, this, false, type);
    } catch (Exception e) {
      if (stun != null) stopSTUN();
      JFLog.log(e);
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
   * Send an empty SIP message to server. This should be done periodically to
   * keep firewalls open. Most routers close UDP connections after 60 seconds.
   * Not sure if needed with TCP/TLS but is done anyways.
   */
  public void keepalive() {
    send(remoteaddr, remoteport, "\r\n\r\n");  //must resemble a complete packet
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
      JFLog.log("Detected IP connecting to WebServer at " + host);
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
        rport = port;  //NOTE : this port may be wrong if router is symmetrical
        stunResponse = true;
        stunLock.notify();
      }
    }
  }
  public void turnAlloc(STUN stun, String ip, int port, byte token[], int lifetime) {}
  public void turnBind(STUN stun) {}
  public void turnRefresh(STUN stun, int lifetime) {}
  public void turnFailed(STUN stun) {}
  public void turnData(STUN stun, byte data[], int offset, int length, short channel) {}

  private boolean findlocalhost_stun() {
    stunResponse = false;
    stunWaiting = true;
    synchronized(stunLock) {
      stun.requestPublicIP();
      try {stunLock.wait(1000);} catch (Exception e) {JFLog.log(e);}
      stunWaiting = false;
    }
    if (stunResponse) {
      JFLog.log("Detected IP using STUN");
    }
    return stunResponse;
  }

  private boolean findlocalhost_java() {
    //this only detects your local IP, not your internet ip
    //not accurate on multi-homed systems
    try {
      InetAddress local = InetAddress.getLocalHost();
      localhost = local.getHostAddress();
      JFLog.log("Detected IP using Java:" + localhost);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Determines local IP address. Method depends on NAT traversal type selected.
   */
  private void findlocalhost() {
    JFLog.log("Detecting localhost for remotehost = " + remotehost);
    if (useNATOnPrivateNetwork || !isPrivateNetwork(remoteip)) {
      if (nat == NAT.STUN || nat == NAT.ICE) {
        if (findlocalhost_stun()) return;
        JFLog.log("SIP:STUN:Failed");
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
   * Issues a command to the SIP server.
   */
  private boolean issue(RTSPSession sess, String cmd, boolean sdp, boolean src) {
    JFLog.log("sessid:" + sess.id + "\r\nissue command : " + cmd + " from : " + user + " to : " + remotehost);
    sess.remotehost = remoteip;
    sess.remoteport = remoteport;
    StringBuilder req = new StringBuilder();
    req.append(cmd + " " + sess.uri + " RTSP/1.0\r\n");
    req.append("Cseq: " + sess.cseq + " " + cmd + "\r\n");
    req.append("User-Agent: " + useragent + "\r\n");
    if (sess.epass != null) {
      req.append(sess.epass);
    }
    if ((sess.sdp != null) && (sdp)) {
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + sess.sdp.length() + "\r\n\r\n");
      req.append(sess.sdp);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    return send(remoteaddr, remoteport, req.toString());
  }

  /**
   * Sends a reply to a SIP server.
   */
  private boolean reply(String cmd, int code, String msg, boolean sdp, boolean src) {
    JFLog.log("callid:" + sess.id + "\r\nissue reply : " + code + " to : " + remotehost);
    StringBuilder req = new StringBuilder();
    req.append("RTSP/1.0 " + code + " " + msg + "\r\n");
    req.append("Cseq: " + sess.cseq + "\r\n");
    req.append("User-Agent: JavaForce\r\n");
    if ((sess.sdp != null) && (sdp)) {
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + sess.sdp.length() + "\r\n\r\n");
      req.append(sess.sdp);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    send(remoteaddr, remoteport, req.toString());
    return true;
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
    StringBuilder sb = new StringBuilder();
    sb.append("OPTIONS ");
    sb.append(url);
    sb.append(" RTSP/1.0\r\n");
    sb.append("CSeq: ");
    sb.append(sess.cseq++);
    sb.append("\r\n");
    if (false) {
      //add authorization ???
    }
    sb.append("\r\n");
    return send(remoteaddr, remoteport, sb.toString());
  }

  /**
   * Send DESCRIBE request to server.
   */
  public boolean describe(String url) {
    StringBuilder sb = new StringBuilder();
    sb.append("DESCRIBE ");
    sb.append(url);
    sb.append(" RTSP/1.0\r\n");
    sb.append("CSeq: ");
    sb.append(sess.cseq++);
    sb.append("\r\n");
    sb.append("Accept: application/sdp\r\n");
    if (false) {
      //add authorization ???
    }
    sb.append("\r\n");
    return send(remoteaddr, remoteport, sb.toString());
  }

  /**
   * Send SETUP request to server (RTSP).
   */
  public boolean setup(String url) {
    StringBuilder sb = new StringBuilder();
    sb.append("SETUP ");
    sb.append(url);
    sb.append(" RTSP/1.0\r\n");
    sb.append("CSeq: ");
    sb.append(sess.cseq++);
    sb.append("\r\n");
    sb.append("Transport: RTP/AVP;unicast;client_port=" + /* RTP port range??? */ "\r\n");
    if (false) {
      //add authorization ???
    }
    sb.append("\r\n");
    return send(remoteaddr, remoteport, sb.toString());
  }

  /**
   * Send PLAY request to server (RTSP).
   */
  public boolean play(String url) {
    StringBuilder sb = new StringBuilder();
    sb.append("PLAY ");
    sb.append(url);
    sb.append(" RTSP/1.0\r\n");
    sb.append("CSeq: ");
    sb.append(sess.cseq++);
    sb.append("\r\n");
    sb.append("Session: " + sess.id + "\r\n");
    if (false) {
      //add authorization ???
    }
    sb.append("\r\n");
    return send(remoteaddr, remoteport, sb.toString());
  }

  /**
   * Send TEARDOWN request to server (RTSP).
   */
  public boolean teardown(String url) {
    StringBuilder sb = new StringBuilder();
    sb.append("TEARDOWN ");
    sb.append(url);
    sb.append(" RTSP/1.0\r\n");
    sb.append("CSeq: ");
    sb.append(sess.cseq++);
    sb.append("\r\n");
    sb.append("Session: " + sess.id + "\r\n");
    if (false) {
      //add authorization ???
    }
    sb.append("\r\n");
    return send(remoteaddr, remoteport, sb.toString());
  }

  /**
   * Processes SIP messages sent from the SIP server.
   */
  public void packet(String msg[], String remoteip, int remoteport) {
    try {
      if (!remoteip.equals(this.remoteip) || remoteport != this.remoteport) {
        JFLog.log("Ignoring packet from unknown host:" + remoteip + ":" + remoteport);
        return;
      }
      String req = null;
      if (remoteip.equals("127.0.0.1")) {
        remoteip = sess.localhost;
      }
      sess.remotehost = remoteip;
      sess.remoteport = remoteport;
      sess.remotecseq = getcseq(msg);
      sess.headers = msg;

      int type = getResponseType(msg);
      if (type != -1) {
        JFLog.log("id:" + sess.id + "\r\nreply=" + type);
      } else {
        req = getRequest(msg);
        sess.uri = getURI(msg);
        JFLog.log("id:" + sess.id + "\r\nrequest=" + req);
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
          }
          break;
        case 401:
        case 407:
          if (sess.authsent) {
            JFLog.log("Server Error : Double " + type);
          } else {
            sess.authstr = getHeader("WWW-Authenticate:", msg);
            if (sess.authstr == null) {
              sess.authstr = getHeader("Proxy-Authenticate:", msg);
            }
            if (sess.authstr == null) {
              JFLog.log("err:401/407 without Authenticate tag");
              break;
            }
            sess.epass = getAuthResponse(sess, auth, pass, remotehost, sess.cmd, (type == 401 ? "Authorization:" : "Proxy-Authorization:"));
            if (sess.epass == null) {
              JFLog.log("err:gen auth failed");
              break;
            }
            sess.cseq++;
            issue(sess, sess.cmd, sess.cmd.equals("INVITE"), true);
            sess.authsent = true;
          }
          break;
        default:
          //treat all other codes as a cancel
          sess.epass = null;
          break;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private int getlocalport() {
    if (rport != -1) return rport; else return localport;
  }

  public static void setEnableRport(boolean state) {
    use_rport = state;
  }

  public static void setEnableReceived(boolean state) {
    use_received = state;
  }
}
