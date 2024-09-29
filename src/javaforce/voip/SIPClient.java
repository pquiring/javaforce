package javaforce.voip;

import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Handles the client end of a SIP link.
 */

public class SIPClient extends SIP implements SIPInterface, STUN.Listener {

  public enum NAT {None, STUN, TURN, ICE};

  private String remotehost, remoteip;
  private InetAddress remoteaddr;
  private int remoteport;
  private String name;  //display name (usually same as user)
  private String user;  //acct name (usually a phone number)
  private String auth;  //auth name (usually same as user)
  private String pass;  //password
  private SIPClientInterface iface;
  private String localhost;
  private int localport, rport = -1;
  private static boolean use_received = true;
  private static boolean use_rport = true;
  private HashMap<String, CallDetails> cdlist;
  private boolean registered;
  private static NAT nat = NAT.None;
  private static boolean useNATOnPrivateNetwork = false;  //do not use NATing techniques on private network servers
  private static String stunHost, stunUser, stunPass;
  private boolean caller;
  private int log = -1;

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
   * Determines if the SIP call is currently on hold.
   */
  public boolean isHold(String callid) {
    CallDetails cd = getCallDetails(callid);
    if (cd.dst.sdp == null) return false;
    return cd.dst.sdp.getFirstAudioStream().canSend();
  }

  /**
   * Returns the registration status.
   */
  public boolean isRegistered() {
    return registered;
  }

  /**
   * Initialize this instance for SIP.<br>
   *
   * @param remotehost,remoteport is the SIP Server/Proxy address.<br>
   * @param localport is the UDP port to bind to locally.<br>
   * @param iface must be a SIPClientInterface where SIP events are dispatched
   * to.<br>
   */
  public boolean init(String remotehost, int remoteport, int localport, SIPClientInterface iface, TransportType type) {
    this.iface = iface;
    this.localport = localport;
    cdlist = new HashMap<String, CallDetails>();
    try {
      if (!super.init(localhost, localport, this, false, type)) {
        return false;
      }
      this.remoteport = remoteport;
      this.remotehost = remotehost;
      this.remoteip = resolve(remotehost);
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
      return true;
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
    if (log != -1) {
      JFLog.close(log);
    }
    super.uninit();
  }

  /**
   * Logs all SIP messages to a log file.
   *
   * @param id = JFLog id (0 = default and should not be used)
   * @param file = log file
   */
  public void log(int id, String file) {
    log = id;
    JFLog.init(id, file, false);
  }

  /**
   * Sets the type of NAT traversal type (global setting).
   */
  public static void setNAT(NAT nat, String host, String user, String pass) {
    SIPClient.nat = nat;
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
   * Registers this client with the SIP server/proxy. <br>
   *
   * @param displayName : display name<br>
   * @param userAccount : username<br>
   * @param authName : authorization name (optional, default=user)<br>
   * @param password : password<br>
   * @return : if message was sent to server successfully<br> This function does
   * not block waiting for a reply. You should receive onRegister() thru the
   * SIPClientInterface when a reply is returned from server.<br>
   */
  public boolean register(String displayName, String userAccount, String authName, String password) {
    return register(displayName, userAccount, authName, password, 3600);
  }

  /**
   * Registers this client with the SIP server/proxy. <br>
   *
   * @param displayName : display name (usually same as userAccount)<br>
   * @param userAccount : username<br>
   * @param authName : authorization name (optional, default=userAccount)<br>
   * @param password : password<br>
   * @param expires : number seconds to register for (0=unregisters)<br>
   * @return : if message was sent to server<br> This function does not block
   * waiting for a reply. You should receive either registered() or
   * unauthorized() thru the SIPClientInterface when a reply is returned from
   * server.<br>
   *
   */
  public boolean register(String displayName, String userAccount, String authName, String password, int expires) {
    String regcallid = getcallid();
    CallDetails cd = getCallDetails(regcallid);  //new CallDetails
    if (userAccount == null || userAccount.length() == 0) return false;
    userAccount = cleanString(userAccount);
    if ((authName == null) || (authName.length() == 0)) {
      this.auth = userAccount;
    } else {
      this.auth = cleanString(authName);
    }
    if (displayName == null || displayName.length() == 0) {
      this.name = userAccount;
    } else {
      this.name = cleanString(displayName);
    }
    this.user = userAccount;
    this.pass = password;
    this.expires = expires;
    cd.src.expires = expires;
    cd.src.to = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.uri = "sip:" + remotehost;  // + ";rinstance=" + getrinstance();
    cd.src.branch = getbranch();
    cd.src.cseq++;
    if ((password == null) || (password.length() == 0)) {
      return true;  //non-register mode
    }
    cd.authsent = false;
    cd.src.extra = null;
    cd.src.epass = null;
    boolean ret = issue(cd, "REGISTER", false, true);
    return ret;
  }

  /**
   * Reregister with the server.
   */
  public boolean reregister() {
    return register(name, user, auth, pass, expires);
  }

  /**
   * Reregister with the server using an expiration of 0 (zero). Effectively
   * unregisters.
   */
  public boolean unregister() {
    return register(name, user, auth, pass, 0);
  }

  /**
   * Publishes Presence to server. (not tested since Asterisk doesn't support it)
   */
  public boolean publish(String state) {
    String pubcallid = getcallid();
    CallDetails cd = getCallDetails(pubcallid);  //new CallDetails
    cd.src.to = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.uri = "sip:" + user + "@" + remotehost;
    cd.src.branch = getbranch();
    cd.src.cseq++;
    cd.sdp = new String[] {
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
      "<presence xmlns=\"urn:ietf:params:xml:ns:pidf\" entity=\"pres:" + user + "@" + remotehost + "\">",
        "<tuple id=\"" + gettupleid() + "\">",
          "<status>",
            "<basic>" + state + "</basic>",
          "</status>",
        "</tuple>",
      "</presence>"
    };

    cd.authsent = false;
    cd.src.extra = "Event: presence\r\n";
    cd.src.epass = null;
    return issue(cd, "PUBLISH", true, true);
  }

  /**
   * Subscribe to a user's presence on server.
   * RFC : 3265
   *
   * @return Call-ID
   */
  public String subscribe(String subuser, String event, int expires) {
    String callid = getcallid();
    CallDetails cd = getCallDetails(callid);  //new CallDetails
    cd.src.to = new String[]{subuser, subuser, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.uri = "sip:" + subuser + "@" + remotehost;
    cd.src.branch = getbranch();
    cd.src.cseq++;
    cd.src.expires = expires;
    cd.src.extra = "Accept: multipart/related, application/rlmi+xml, application/pidf+xml\r\nEvent: " + event + "\r\n";
    cd.src.epass = null;
    if (!issue(cd, "SUBSCRIBE", false, true)) {
      return null;
    }
    return callid;
  }

  /** Unsubscribe a previously subscribed user.
   * Re-sends a subscribe with expires set to zero.
   * NOTE:Should receive last NOTIFY as confirmation.
   */
  public boolean unsubscribe(String callid, String subuser, String event) {
    CallDetails cd = getCallDetails(callid);
    cd.src.to = new String[]{subuser, subuser, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.uri = "sip:" + subuser + "@" + remotehost;
    cd.src.branch = getbranch();
    cd.src.cseq++;
    cd.src.expires = 0;
    cd.src.extra = "Accept: multipart/related, application/rlmi+xml, application/pidf+xml\r\nEvent: " + event + "\r\n";
    cd.src.epass = null;
    return issue(cd, "SUBSCRIBE", false, true);
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
  public String getlocalRTPhost(CallDetails cd) {
    if (RTP.useTURN)
      return RTP.getTurnIP();
    else
      return cd.localhost;
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

  private synchronized CallDetails getCallDetails(String callid) {
    CallDetails cd = cdlist.get(callid);
    if (cd == null) {
      cd = new CallDetails();
      JFLog.log("Create CallDetails:" + callid);
      cd.callid = callid;
      cd.localhost = localhost;
      setCallDetails(callid, cd);
    }
    return cd;
  }

  private void setCallDetails(String callid, CallDetails cd) {
    if (cd == null) {
      JFLog.log("Delete CallDetails:" + callid);
      cdlist.remove(callid);
    } else {
      cdlist.put(callid, cd);
    }
  }

  /**
   * Issues a command to the SIP server.
   */
  private boolean issue(CallDetails cd, String cmd, boolean sdp, boolean src) {
    CallDetails.SideDetails cdsd = (src ? cd.src : cd.dst);
    JFLog.log("callid:" + cd.callid + "\r\nissue command : " + cmd + " from : " + user + " to : " + remotehost);
    cd.dst.host = remoteip;
    cd.dst.port = remoteport;
    StringBuilder req = new StringBuilder();
    if (cd.uri == null) {
      cd.uri = "sip:" + user + "@" + remotehost;
    }
    req.append(cmd + " " + cd.uri + " SIP/2.0\r\n");
    req.append("Via: SIP/2.0/" + transport.getName() + " " + cd.localhost + ":" + getlocalport() + ";branch=" + cdsd.branch + (use_rport ? ";rport" : "") + "\r\n");
    req.append("Max-Forwards: 70\r\n");
    if (cdsd.routelist != null) {
      for (int a = cdsd.routelist.length-1; a >=0; a--) {
        req.append(cdsd.routelist[a]);
        req.append("\r\n");
      }
    }
    if (cdsd.contact != null) {
      req.append("Contact: " + cdsd.contact + "\r\n");
    }
    req.append("To: " + join(cdsd.to) + "\r\n");
    req.append("From: " + join(cdsd.from) + "\r\n");
    req.append("Call-ID: " + cd.callid + "\r\n");
    req.append("Cseq: " + cdsd.cseq + " " + cmd + "\r\n");
    if ((cmd.equals("REGISTER")) || (cmd.equals("PUBLISH")) || (cmd.equals("SUBSCRIBE"))) {
      req.append("Expires: " + cdsd.expires + "\r\n");
    }
    req.append("Allow: INVITE, ACK, CANCEL, BYE, REFER, NOTIFY, OPTIONS, MESSAGE\r\n");
    req.append("User-Agent: " + useragent + "\r\n");
    if (cdsd.extra != null) {
      req.append(cdsd.extra);
    }
    if (cdsd.epass != null) {
      req.append(cdsd.epass);
    }
    String post = null;
    if ((cd.sdp != null) && (sdp)) {
      post = String.join("\r\n", cd.sdp) + "\r\n";
      if (post.startsWith("<?xml")) {
        req.append("Content-Type: application/pidf+xml\r\n");
      } else if (post.startsWith("SIP/2.0")) {
        req.append("Content-Type: message/sipfrag;version=2.0\r\n");
      } else {
        if (cmd.equals("MESSAGE")) {
          req.append("Content-Type: text/plain\r\n");
        } else {
          req.append("Content-Type: application/sdp\r\n");
        }
      }
      req.append("Content-Length: " + post.length() + "\r\n\r\n");
      req.append(post);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    String sip = req.toString();
    if (log != -1) {
      JFLog.log(log, "Client -> Server");
      JFLog.log(log, sip);
    }
    return send(remoteaddr, remoteport, sip);
  }

  /**
   * Sends a reply to a SIP server.
   */
  private boolean reply(CallDetails cd, String cmd, int code, String msg, boolean sdp, boolean src) {
    JFLog.log("callid:" + cd.callid + "\r\nissue reply : " + code + " to : " + remotehost);
    CallDetails.SideDetails cdsd = (src ? cd.src : cd.dst);
    StringBuilder req = new StringBuilder();
    req.append("SIP/2.0 " + code + " " + msg + "\r\n");
    if (cdsd.vialist != null) {
      for (int a = 0; a < cdsd.vialist.length; a++) {
        req.append(cdsd.vialist[a]);
        req.append("\r\n");
      }
    }
    if ((cdsd.vialist == null) || (cdsd.vialist.length == 0)) {
      req.append("Via: SIP/2.0/" + transport.getName() + " " + cd.localhost + ":" + getlocalport() + ";branch=" + cdsd.branch + (use_rport ? ";rport" : "") + "\r\n");
    }
    if (cdsd.routelist != null) {
      for (int a = cdsd.routelist.length-1; a >=0; a--) {
        req.append(cdsd.routelist[a]);
        req.append("\r\n");
      }
    }
    req.append("Contact: " + cdsd.contact + "\r\n");
    req.append("To: " + join(cdsd.to) + "\r\n");
    req.append("From: " + join(cdsd.from) + "\r\n");
    req.append("Call-ID: " + cd.callid + "\r\n");
    req.append("Cseq: " + cdsd.cseq + " " + cmd + "\r\n");
    req.append("Allow: INVITE, ACK, CANCEL, BYE, REFER, NOTIFY, OPTIONS, MESSAGE\r\n");
    req.append("User-Agent: JavaForce\r\n");
    if ((cd.sdp != null) && (sdp)) {
      String post = String.join("\r\n", cd.sdp) + "\r\n";
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + post.length() + "\r\n\r\n");
      req.append(post);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    String sip = req.toString();
    if (log != -1) {
      JFLog.log(log, "Client -> Server");
      JFLog.log(log, sip);
    }
    send(remoteaddr, remoteport, sip);
    return true;
  }

  /**
   * Send an invite to server.<br>
   *
   * @param to : number to dial
   * @param sdp : SDP (only stream types/modes/codecs are needed)
   *
   * @return unique Call-ID (not caller id)<br>
   */
  public String invite(String to, SDP sdp) {
    caller = true;
    String callid = getcallid();
    CallDetails cd = getCallDetails(callid);  //new CallDetails
    cd.src.to = new String[]{to, to, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.uri = "sip:" + to + "@" + remotehost + ":" + remoteport;
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.branch = getbranch();
    cd.src.sdp = sdp;
    cd.sdp = buildsdp(cd, cd.src);
    cd.src.cseq++;
    cd.authsent = false;
    cd.src.extra = null;
    cd.src.epass = null;
    if (!issue(cd, "INVITE", true, true)) {
      return null;
    }
    return callid;
  }

  /**
   * Send a refer command to server (blind transfer)
   */
  public boolean refer(String callid, String to) {
    String headers = "Refer-To: <sip:" + to + "@" + remotehost + ">\r\nReferred-By: <sip:" + user + "@" + remotehost + ":" + getlocalport() + ">\r\n";
    CallDetails cd = getCallDetails(callid);
    if (cd.authstr != null) {
      cd.src.epass = getAuthResponse(cd, auth, pass, remotehost, "REFER", "Proxy-Authorization:");
    } else {
      cd.src.epass = null;
    }
    cd.uri = "sip:" + cd.src.to[1] + "@" + remotehost + ":" + remoteport;
    cd.src.cseq++;
    cd.authsent = false;
    cd.src.extra = headers;
    cd.src.epass = null;
    boolean ret = issue(cd, "REFER", false, true);
    return ret;
  }

  /**
   * Send a refer command to server (non-blind transfer)
   */
  public boolean referLive(String callid, String othercallid) {
    //see RFC5589 part 7.2
    //this MUST be sent to the transfer target from the transferor (not to the transferee) [we are actually transfering the target to the tranferee - kinda backwards thinking]
    //callid = transfer target call leg
    //othercallid = transferee call leg
    CallDetails cd = getCallDetails(callid);
    CallDetails othercd = getCallDetails(othercallid);
    String headers = "Refer-To: <sip:" + othercd.src.to[1] + "@" + remotehost + "?Replaces=" + othercallid
      + "%3Bto-tag%3D" + gettag(othercd.src.to) + "%3Bfrom-tag%3D" + gettag(othercd.src.from) + ">\r\n";
//    headers += "Referred-By: <sip:" + user + "@" + remotehost + ":" + getlocalport() + ">\r\n";
    headers += "Supported: gruu, replaces, tdialog\r\n";
    headers += "Require: tdialog\r\n";
    headers += "Target-Dialog: " + callid + ";local-tag=" + gettag(cd.src.to) + ";remote-tag=" + gettag(cd.src.from) + "\r\n";
    cd.uri = "sip:" + cd.src.to[1] + "@" + remotehost + ":" + remoteport;
    cd.src.cseq++;
    cd.authsent = false;
    cd.src.extra = headers;
    cd.src.epass = null;
    boolean ret = issue(cd, "REFER", false, true);
    return ret;
  }

  /**
   * Set/clear hold state, must call reinvite() after to notify server.
   */
  public boolean setHold(String callid, boolean state) {
    CallDetails cd = getCallDetails(callid);
    cd.src.sdp.getFirstAudioStream().mode = (state ? SDP.Mode.inactive : SDP.Mode.sendrecv);
    return true;
  }

  /**
   * Sends a reINVITE to server with a new SDP packet.
   *
   * @param callid : id of call to reinvite
   * @param sdp : SDP (only stream types/modes/codecs are needed) (ip not needed)
   */
  public boolean reinvite(String callid, SDP sdp) {
    CallDetails cd = getCallDetails(callid);
    cd.src.sdp = sdp;
    cd.src.sdp.o2++;
    cd.sdp = buildsdp(cd, cd.src);
    cd.uri = "sip:" + cd.src.to[1] + "@" + remotehost + ":" + remoteport;
    cd.src.cseq++;
    cd.authsent = false;
    cd.src.extra = null;
    if (cd.authstr != null) {
      cd.src.epass = getAuthResponse(cd, auth, pass, remotehost, "INVITE", "Proxy-Authorization:");
    } else {
      cd.src.epass = null;
    }
    if (!issue(cd, "INVITE", true, true)) {
      return false;
    }
    return true;
  }

  /**
   * Sends a reINVITE to server using previous SDP packet.
   *
   * @param callid : id of call to reinvite
   */
  public boolean reinvite(String callid) {
    CallDetails cd = getCallDetails(callid);
    return reinvite(callid, cd.src.sdp);
  }

  /**
   * Cancels an INVITE request. Usually done while phone is ringing.
   */
  public boolean cancel(String callid) {
    CallDetails cd = getCallDetails(callid);
    if (cd.authstr != null) {
      cd.src.epass = getAuthResponse(cd, auth, pass, remotehost, "BYE", "Proxy-Authorization:");
    } else {
      cd.src.epass = null;
    }
    cd.authsent = false;
    if (cd.dst.to != null) cd.src.to = cd.dst.to;  //update tag if avail
    if (cd.dst.from != null) cd.src.from = cd.dst.from;  //is this line even needed???
    cd.src.extra = null;
    cd.uri = "sip:" + cd.src.to[1] + "@" + remotehost + ":" + remoteport;
    boolean ret = issue(cd, "CANCEL", false, true);
    return ret;
  }

  /**
   * Send a request to terminate a call in progress.
   */
  public boolean bye(String callid) {
    CallDetails cd = getCallDetails(callid);
    if (cd.authstr != null) {
      cd.src.epass = getAuthResponse(cd, auth, pass, remotehost, "BYE", "Proxy-Authorization:");
    } else {
      cd.src.epass = null;
    }
    cd.uri = "sip:" + cd.src.to[1] + "@" + remotehost + ":" + remoteport;
    cd.src.cseq++;
    cd.authsent = false;
    cd.src.extra = null;
    boolean ret = issue(cd, "BYE", false, true);
    return ret;
  }

  /**
   * Send instant message.
   * Outside of a dialog.
   *
   * See RFC 3428
   *
   * TODO : RFC 4975 for rich text messages.
   */
  public String message(String to, String[] msg) {
    String callid = getcallid();
    CallDetails cd = getCallDetails(callid);  //new CallDetails
    cd.src.to = new String[]{to, to, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{name, user, remotehost + ":" + remoteport, ":"};
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.uri = "im:" + to + "@" + remotehost + ":" + remoteport;
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.branch = getbranch();
    cd.src.cseq++;
    cd.sdp = msg;
    if (!issue(cd, "MESSAGE", true, true)) {
      return null;
    }
    return callid;
  }

  /**
   * Send instant message.
   * Within current dialog.
   *
   * See RFC 3428
   *
   * TODO : RFC 4975 for rich text messages.
   */
  public String message(String callid, String to, String[] msg) {
    CallDetails cd = getCallDetails(callid);  //new CallDetails
    if (cd.authstr != null) {
      cd.src.epass = getAuthResponse(cd, auth, pass, remotehost, "BYE", "Proxy-Authorization:");
    } else {
      cd.src.epass = null;
    }
    cd.authsent = false;
    cd.src.extra = null;
    cd.src.to = new String[]{to, to, remotehost + ":" + remoteport, ":"};
    cd.uri = "im:" + to + "@" + remotehost + ":" + remoteport;
    cd.src.cseq++;
    cd.sdp = msg;
    if (!issue(cd, "MESSAGE", true, true)) {
      return null;
    }
    return callid;
  }

  /**
   * Sends a reply to accept an inbound (re)INVITE.
   * @param sdp : SDP (only stream types/modes/codecs are needed) (ip not needed)
   */
  public boolean accept(String callid, SDP sdp) {
    caller = false;
    CallDetails cd = getCallDetails(callid);
    cd.src.sdp = sdp;
    cd.sdp = buildsdp(cd, cd.src);
    reply(cd, "INVITE", 200, "OK", true, false);
    //need to swap to/from for BYE (only if accept()ing a call)
    cd.src.to = cd.dst.from.clone();
    cd.src.from = cd.dst.to.clone();
    //copy all other needed fields
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.src.branch = cd.dst.branch;
    return true;
  }

  /**
   * Sends a reply to accept an inbound REINVITE.
   * @param sdp : SDP (only stream types/modes/codecs are needed) (ip not needed)
   */
  public boolean reaccept(String callid, SDP sdp) {
    CallDetails cd = getCallDetails(callid);
    cd.src.sdp = sdp;
    cd.sdp = buildsdp(cd, cd.src);
    reply(cd, "INVITE", 200, "OK", true, false);
    //do NOT swap to/from in this case
    //copy all other needed fields
    cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
    cd.src.branch = cd.dst.branch;
    return true;
  }

  /**
   * Denies an INVITE for whatever reason.
   */
  public boolean deny(String callid, String msg, int code) {
    CallDetails cd = getCallDetails(callid);
    reply(cd, "INVITE", code, msg, false, false);
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
   * Processes SIP messages sent from the SIP server.
   */
  public void packet(String[] msg, String remoteip, int remoteport) {
    try {
      if (!remoteip.equals(this.remoteip) || remoteport != this.remoteport) {
        JFLog.log("Ignoring packet from unknown host:" + remoteip + ":" + remoteport);
        return;
      }
      if (log != -1) {
        JFLog.log(log, "Server -> Client");
        StringBuilder sip = new StringBuilder();
        for(int a=0;a<msg.length;a++) {
          sip.append(msg[a]);
          sip.append("\r\n");
        }
        JFLog.log(log, sip.toString());
      }
      String tmp, req = null;
      String callid = HTTP.getParameter(msg, "Call-ID");
      if (callid == null) callid = HTTP.getParameter(msg, "i");
      if (callid == null) {
        JFLog.log("Bad packet (no Call-ID) from:" + remoteip + ":" + remoteport);
        return;
      }
      CallDetails cd = getCallDetails(callid);
      if (remoteip.equals("127.0.0.1")) {
        remoteip = cd.localhost;
      }
      cd.dst.host = remoteip;
      cd.dst.port = remoteport;
      cd.dst.cseq = getcseq(msg);
      cd.dst.branch = getbranch(msg);
      cd.headers = msg;
      //get cd.dst.to
      tmp = HTTP.getParameter(msg, "To");
      if (tmp == null) {
        tmp = HTTP.getParameter(msg, "t");
      }
      cd.dst.to = split(tmp);
      //get cd.dst.from
      tmp = HTTP.getParameter(msg, "From");
      if (tmp == null) {
        tmp = HTTP.getParameter(msg, "f");
      }
      cd.dst.from = split(tmp);

      //RFC 3581 - rport
      String via = HTTP.getParameter(msg, "Via");
      boolean localhost_changed = false;
      if (via == null) {
        via = HTTP.getParameter(msg, "v");
      }
      if (via != null) {
        String[] via_params = convertParameters(via, ';');
        if (use_received) {
          //check for received in via header which equals my IP as seen by server
          String received = HTTP.getParameter(via_params, "received");
          if (received != null) {
            if (!cd.localhost.equals(received)) {
              localhost = received;
              cd.localhost = received;
              JFLog.log("received ip=" + received + " for remotehost = " + remotehost);
              localhost_changed = true;
            }
          }
        }
        if (use_rport) {
          //check for rport in via header which equals my port as seen by server
          String rportstr = HTTP.getParameter(via_params, "rport");
          if (rportstr != null && rportstr.length() > 0) {
            int newrport = JF.atoi(rportstr);
            if (rport != newrport) {
              rport = newrport;
              JFLog.log("received port=" + rport + " for remotehost = " + remotehost);
              localhost_changed = true;
            }
          }
        }
      }

      //get via list
      cd.dst.vialist = getvialist(msg);
      //get route list (RFC 2543 6.29)
      cd.dst.routelist = getroutelist(msg);
      //set contact
//      cd.dst.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
      //get uri (it must equal the Contact field)
      cd.dst.contact = HTTP.getParameter(msg, "Contact");
      if (cd.dst.contact == null) {
        cd.dst.contact = HTTP.getParameter(msg, "m");
      }
      String cmd = getcseqcmd(msg);
      int type = getResponseType(msg);
      if (type != -1) {
        JFLog.log("callid:" + callid + "\r\nreply=" + type);
        //RFC 3261 : 8.1.3.3 - Vias
        if (cd.dst.vialist.length > 1) {
          JFLog.log("Multiple Via:s detected in reply, discarding reply");
          return;
        }
      } else {
        req = getRequest(msg);
        cd.uri = getURI(msg);
        JFLog.log("callid:" + callid + "\r\nrequest=" + req);
      }
      switch (type) {
        case -1:
          if (req.equals("INVITE")) {
            //generate toTag
            if (gettag(cd.dst.to) == null) {
              cd.dst.to = replacetag(cd.dst.to, generatetag());
            }
            cd.dst.sdp = getSDP(msg);
            cd.dst.sdp.o1++;
            cd.dst.sdp.o2++;
            switch (iface.onInvite(this, callid, cd.dst.from[0], cd.dst.from[1], cd.dst.sdp)) {
              case 180:  //this is the normal return
                reply(cd, cmd, 180, "RINGING", false, false);
                break;
              case 200:  //this was usually a reINVITE to change sdp details
                cd.sdp = buildsdp(cd, cd.src);
                reply(cd, cmd, 200, "OK", true, false);
                break;
              case 486:
                reply(cd, cmd, 486, "BUSY HERE", false, false);
                break;
              case -1:
                //do nothing
                break;
            }
            break;
          }
          if (req.equals("CANCEL")) {
            iface.onCancel(this, callid, 0);
            reply(cd, cmd, 200, "OK", false, false);
            reply(cd, "INVITE", 487, "CANCELLED", false, false);
            //then should receive ACK
//            setCallDetails(callid, null);  //need to wait for ACK
            break;
          }
          if (req.equals("BYE")) {
            reply(cd, cmd, 200, "OK", false, false);
            iface.onBye(this, callid);
//            setCallDetails(callid, null);  //need to wait for ACK
            break;
          }
          if (req.equals("NOTIFY")) {
            reply(cd, cmd, 200, "OK", false, false);
            String event = HTTP.getParameter(msg, "Event");
            if (event == null) event = HTTP.getParameter(msg, "o");
            iface.onNotify(this, callid, event, HTTP.getContent(msg));
            break;
          }
          if (req.equals("ACK")) {
            SDP sdp = getSDP(msg);
            if (cd.dst.sdp == null) {
              cd.dst.sdp = sdp;
            }
            iface.onAck(this, callid, sdp);
            if (cmd.equals("BYE")) {
              setCallDetails(callid, null);
            }
            break;
          }
          if (req.equals("MESSAGE")) {
            iface.onMessage(this, callid, cd.dst.from[0], cd.dst.from[1], HTTP.getContent(msg));
            reply(cd, "MESSAGE", 200, "OK", false, false);
            break;
          }
          reply(cd, cmd, 405, "Method Not Allowed", false, false);
          break;
        case 100:
          iface.onTrying(this, callid);
          break;
        case 180:
          iface.onRinging(this, callid);
          break;
        case 181:
          //call if being forwarded
          //ignore for now
          break;
        case 183:
        case 200:
          if (cmd.equals("REGISTER")) {
            if (type == 183) break;  //not used in REGISTER command
            if (cd.src.expires > 0) {
              if (localhost_changed) {
                JFLog.log("localhost change detected, reregister()ing");
                reregister();
                break;
              }
              registered = true;
              iface.onRegister(this, true);
            } else {
              registered = false;
              //iface.onRegister() ???
            }
          } else if (cmd.equals("INVITE")) {
            cd.dst.sdp = getSDP(msg);
            //update cd.src.to tag value
            cd.src.to = cd.dst.to;
            cd.src.epass = null;
            cd.src.routelist = cd.dst.routelist;  //RFC 2543 6.29
            if (type == 200) issue(cd, "ACK", false, true);
            iface.onSuccess(this, callid, cd.dst.sdp, type == 200);
          } else if (cmd.equals("BYE")) {
            if (type == 183) break;  //not used in BYE command
            //call leg ended
            setCallDetails(callid, null);
          }
          break;
        case 202:
          if (cmd.equals("REFER")) {
            iface.onRefer(this, callid);
          }
          break;
        case 401:
        case 407:
          if (cd.authsent) {
            JFLog.log("Server Error : Double " + type);
          } else {
            String[] src_to = cd.src.to;
            cd.src.to = cd.dst.to;  //update to (tag may have been added)
            if (iface != null) {
              if (cmd.equals("INVITE")) {
                //only issue ACK for call setup (INVITE)
                //not for REGISTER or SUBSCRIBE or NOTIFY
                issue(cd, "ACK", false, true);
              }
            }
            cd.src.to = src_to;
            cd.authstr = HTTP.getParameter(msg, "WWW-Authenticate");
            if (cd.authstr == null) {
              cd.authstr = HTTP.getParameter(msg, "Proxy-Authenticate");
            }
            if (cd.authstr == null) {
              JFLog.log("err:401/407 without Authenticate tag");
              break;
            }
            cd.src.epass = getAuthResponse(cd, auth, pass, remotehost, cmd, (type == 401 ? "Authorization:" : "Proxy-Authorization:"));
            if (cd.src.epass == null) {
              JFLog.log("err:gen auth failed");
              break;
            }
            cd.src.cseq++;
            //update contact info
            cd.src.contact = "<sip:" + user + "@" + cd.localhost + ":" + getlocalport() + ">";
            issue(cd, cmd, cmd.equals("INVITE") || cmd.equals("MESSAGE"), true);
            cd.authsent = true;
          }
          break;
        case 403:
          cd.src.epass = null;
          cd.src.cseq = cd.dst.cseq;
          if (cmd.equals("REGISTER")) {
            //bad password
            iface.onRegister(this, false);
          } else {
            issue(cd, "ACK", false, true);
            iface.onCancel(this, callid, type);
          }
          break;
        case 404:  //no one there
        case 486:  //busy
          if (cd.dst.to != null) cd.src.to = cd.dst.to;
          cd.src.epass = null;
          cd.src.cseq = cd.dst.cseq;
          issue(cd, "ACK", false, true);
          iface.onCancel(this, callid, type);
          setCallDetails(callid, null);
          break;
        case 481:
          //call leg unknown - ignore it
          break;
        default:
          //treat all other codes as a cancel
          if (cd.dst.to != null) cd.src.to = cd.dst.to;
          cd.src.epass = null;
          cd.src.cseq = cd.dst.cseq;
          issue(cd, "ACK", false, true);
          iface.onCancel(this, callid, type);
//          setCallDetails(callid, null);  //might not be done
          break;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private int getlocalport() {
    if (rport != -1) return rport; else return localport;
  }

  public void setremoteport(int port) {
    remoteport = port;
  }

  public static void setEnableRport(boolean state) {
    use_rport = state;
  }

  public static void setEnableReceived(boolean state) {
    use_received = state;
  }

  /** Returns the raw SDP from onSuccess() event */
  public String[] getSDP(String callid) {
    CallDetails cd = getCallDetails(callid);
    cd.sdp = buildsdp(cd, cd.dst);
    return cd.sdp;
  }

  public String[] getHeaders(String callid) {
    CallDetails cd = getCallDetails(callid);
    return cd.headers;
  }

  public boolean isCaller() {
    return caller;
  }
}
