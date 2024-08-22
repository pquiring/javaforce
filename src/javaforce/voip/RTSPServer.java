package javaforce.voip;

import java.util.*;
import java.net.*;

import javaforce.*;

/**
 * Handles the server end of a RTSP link.
 */

public class RTSPServer extends RTSP implements RTSPInterface, STUN.Listener {

  public enum NAT {None, STUN, TURN, ICE};

  private RTSPServerInterface iface;
  private String localhost;
  private int localport;
  private static NAT nat = NAT.None;
  private static boolean useNATOnPrivateNetwork = false;  //do not use NATing techniques on private network servers
  private static String stunHost, stunUser, stunPass;
  private boolean use_qop = false;
  private static final String realm = "javaforce";
  private boolean active = true;
  private WorkerKeepAlive keepAlive;

  public Object userobj;  //user definable
  public int expires;  //expires

  private Object clientsLock = new Object();
  private HashMap<String, RTSPSession> clients = new HashMap<>();

  /**
   * Initialize this instance for RTSP.<br>
   *
   * @param localport is the TCP port to bind to locally (usually 554).<br>
   * @param iface must be a RTSPServerInterface where RTSP events are dispatched
   * to.<br>
   */
  public boolean init(int localport, RTSPServerInterface iface, TransportType type) {
    this.iface = iface;
    if (localport == -1) localport = 554;
    this.localport = localport;
    try {
      JFLog.log(log, "localhost = " + localhost);
      if (nat == NAT.STUN || nat == NAT.ICE) {
        startSTUN();
      }
      keepAlive = new WorkerKeepAlive();
      keepAlive.start();
      return super.init(localhost, localport, this, true, type);
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
    if (nat == NAT.STUN || nat == NAT.ICE) {
      stopSTUN();
    }
    active = false;
    super.uninit();
  }

  /**
   * Sets the type of NAT traversal type (global setting).
   */
  public static void setNAT(NAT nat, String host, String user, String pass) {
    RTSPServer.nat = nat;
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

  /**
   * Issues a reply to the RTSP client.
   */
  public boolean reply(RTSPSession sess, int code, String msg, String header) {
    JFLog.log(log, "issue reply : " + code + ":" + sess);
    sess.reply = code;
    StringBuilder req = new StringBuilder();
    req.append("RTSP/1.0 " + code + " " + msg + "\r\n");
    req.append("Cseq: " + sess.cseq + "\r\n");
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
    if (header != null) {
      req.append(header);
    }
    if (sess.sdp != null) {
      String post = String.join("\r\n", sess.sdp) + "\r\n";
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + post.length() + "\r\n\r\n");
      req.append(post);
    } else if (sess.params != null) {
      String post = String.join("\r\n", sess.params) + "\r\n";
      req.append("Content-Type: text/plain\r\n");
      req.append("Content-Length: " + post.length() + "\r\n\r\n");
      req.append(post);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    return send(sess.remoteaddr, sess.remoteport, req.toString());
  }

  /**
   * Issues a reply to the RTSP client.
   */
  public boolean reply(RTSPSession sess, int reply, String msg) {
    return reply(sess, reply, msg, null);
  }

  private RTSPSession getSession(String host, int port, String id) {
    RTSPSession sess;
    synchronized (clientsLock) {
      sess = clients.get(id);
    }
    return sess;
  }

  private RTSPSession createSession(String host, int port, String id) {
    RTSPSession sess;
    synchronized (clientsLock) {
      sess = clients.get(id);
      if (sess == null) {
        sess = new RTSPSession(localhost, localport);
        sess.remotehost = host;
        sess.remoteport = port;
        sess.ts = System.currentTimeMillis();
        clients.put(id, sess);
      }
    }
    return sess;
  }

  private static RTSPSession[] RTSPSessionArrayType = new RTSPSession[0];
  public RTSPSession[] getSessions() {
    synchronized (clientsLock) {
      return clients.values().toArray(RTSPSessionArrayType);
    }
  }

  public String[] getTransportClients() {
    return transport.getClients();
  }

  /**
   * Processes RTSP messages sent from clients.
   */
  public void onPacket(RTSP rtsp, String[] msg, String remoteip, int remoteport) {
    String id = remoteip + ":" + remoteport;
    try {
      RTSPSession sess = getSession(remoteip, remoteport, id);
      sess.ts = System.currentTimeMillis();
      String cmd = null;
      if (remoteip.equals("127.0.0.1")) {
        if (sess.localhost != null) {
          remoteip = sess.localhost;
        }
      }
      sess.remotehost = remoteip;
      sess.remoteaddr = InetAddress.getByName(remoteip);
      sess.remoteport = remoteport;
      sess.remotecseq = getcseq(msg);
      sess.id = HTTP.getParameter(msg, "Session");

      int reply = getResponseType(msg);
      if (reply != -1) {
        JFLog.log(log, "nreply=" + reply + ":" + sess);
      } else {
        cmd = getRequest(msg);
        sess.uri = getURI(msg);
        JFLog.log(log, "request=" + cmd + ":" + sess);
      }
      switch (reply) {
        case -1:
          //request/cmd
          sess.cmd = cmd;
          switch (cmd) {
            case "OPTIONS": {
              String auth = HTTP.getParameter(msg, "Authorization");
              if (auth == null) {
                //send a 401
                sess.nonce = getnonce();
                String challenge = "WWW-Authenticate: Digest algorithm=MD5, realm=\"" + realm + "\", nonce=\"" + sess.nonce + "\"";
                if (use_qop) {
                  challenge += ", qop=\"auth\"";
                }
                challenge += "\r\n";
                reply(sess, 401, "REQ AUTH", challenge);
                break;
              }
              if (!auth.regionMatches(true, 0, "digest ", 0, 7)) {
                JFLog.log(log, "invalid Authorization");
                break;
              }
              String[] tags = SIP.convertParameters(auth.substring(7),',');
              String res = HTTP.getParameter(tags, "response");
              String nonce = HTTP.getParameter(tags, "nonce");
              sess.user = HTTP.getParameter(tags, "username");
              if ((nonce == null) || (sess.nonce == null) || (!sess.nonce.equals(nonce)) || (sess.user == null)) {
                //send another 401
                sess.nonce = getnonce();
                String challenge = "WWW-Authenticate: Digest algorithm=MD5, realm=\"" + realm + "\", nonce=\"" + sess.nonce + "\"";
                if (use_qop) {
                  challenge += ", qop=\"auth\"";
                }
                challenge += "\r\n";
                reply(sess, 401, "REQ AUTH", challenge);
                break;
              }
              String test = getResponse(
                sess.user,
                iface.getPassword(sess.user),
                realm,
                sess.cmd,
                HTTP.getParameter(tags, "uri"),
                sess.nonce,
                HTTP.getParameter(tags, "qop"),
                HTTP.getParameter(tags, "nc"),
                HTTP.getParameter(tags, "cnonce")
              );
              sess.nonce = null;  //don't allow value to be reused
              if (!res.equalsIgnoreCase(test)) {
                reply(sess, 403, "BAD PASSWORD", null);
                break;
              }
              sess.auth = true;
              iface.onOptions(this, sess);
              break;
            }
            case "DESCRIBE": {
              if (!sess.auth) {
                JFLog.log(log, "!auth");
                break;
              }
              iface.onDescribe(this, sess);
              break;
            }
            case "SETUP": {
              if (!sess.auth) {
                JFLog.log(log, "!auth");
                break;
              }
              //get client RTP ports : Transport: RTP/AVP;unicast;client_port=x-x
              String transport = HTTP.getParameter(msg, "Transport");
              String[] tags = SIP.convertParameters(transport, ';');
              String client_port = HTTP.getParameter(tags, "client_port");
              String[] ports = client_port.split("[-]");
              sess.channel.stream.port = Integer.valueOf(ports[0]);
              iface.onSetup(this, sess);
              break;
            }
            case "PLAY": {
              if (!sess.auth) {
                if (debug) JFLog.log(log, "!auth");
                break;
              }
              iface.onPlay(this, sess);
              break;
            }
            case "TEARDOWN": {
              if (!sess.auth) {
                if (debug) JFLog.log(log, "!auth");
                break;
              }
              iface.onTeardown(this, sess);
              break;
            }
            case "GET_PARAMETER": {
              iface.onGetParameter(this, sess, HTTP.getContent(msg));
              break;
            }
            case "SET_PARAMETER": {
              iface.onSetParameter(this, sess, HTTP.getContent(msg));
              break;
            }
          }
          break;
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public void onConnect(RTSP rtsp, String remoteip, int remoteport) {
    String id = remoteip + ":" + remoteport;
    iface.onConnect(this, createSession(remoteip, remoteport, id));
  }

  public void onDisconnect(RTSP rtsp, String remoteip, int remoteport) {
    //NOTE : this is only invoked for TCP clients
    String id = remoteip + ":" + remoteport;
    RTSPSession sess = getSession(remoteip, remoteport, id);
    if (sess == null) return;
    iface.onDisconnect(this, sess);
    synchronized (clientsLock) {
      clients.remove(id);
    }
  }

  private byte[] nullmsg = new byte[4];

  public class WorkerKeepAlive extends Thread {
    public void run() {
      setName("RTSPServer.WorkerKeepAlive");
      while (active) {
        JF.sleep(1000);
        long now = System.currentTimeMillis();
        long cut = now - 60 * 1000;
        String[] clients = transport.getClients();
        for(String client : clients) {
          try {
            int idx = client.indexOf(':');
            String host = client.substring(0, idx);
            String portstr = client.substring(idx+1);
            int port = Integer.valueOf(portstr);
            RTSPSession sess = getSession(host, port, client);
            if (sess == null) continue;
            if (sess.ts < cut) {
              InetAddress hostaddr = InetAddress.getByName(host);
              if (false) {
                transport.send(nullmsg, log, log, hostaddr, port);
              } else {
                transport.disconnect(host, port);
              }
            }
          } catch (Exception e) {
            if (debug) JFLog.log(log, e);
          }
        }
      }
    }
  }
}
