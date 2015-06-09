package javaforce.voip;

import java.io.*;
import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Handles the client end of a SIP link.
 */
public class SIPClient extends SIP implements SIPInterface {

  private String remotehost, remoteip;
  private int remoteport;
  private String user, auth;
  private String pass;
  private SIPClientInterface iface;
  private String localhost;
  private int localport;
  private Hashtable<String, CallDetails> cdlist;
  private boolean registered;
  public Object rtmp;  //used by RTMP2SIPServer
  public Object userobj;  //user definable

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
    return cd.onhold;
  }

  /**
   * Returns the registration status.
   */
  public boolean isRegistered() {
    return registered;
  }

  /**
   * Initialize this instance.<br>
   *
   * @param remotehost,remoteport is the SIP Server/Proxy address.<br>
   * @param localport is the UDP port to bind to locally.<br>
   * @param iface must be a SIPClientInterface where SIP events are dispatched
   * to.<br>
   */
  public boolean init(String remotehost, int remoteport, int localport, SIPClientInterface iface) {
    this.iface = iface;
    this.localport = localport;
    this.remoteport = remoteport;
    cdlist = new Hashtable<String, CallDetails>();
    try {
      super.init(localport, this, false);
      this.remotehost = remotehost;
      this.remoteip = resolve(remotehost);
    } catch (Exception e) {
      JFLog.log("SIPClient:init() failed : " + e);
      return false;
    }
    return true;
  }

  /**
   * Free all resources.
   */
  public void uninit() {
    super.uninit();
  }

  /**
   * Registers this client with the SIP server/proxy. <br>
   *
   * @param user : username<br>
   * @param auth : authorization name (optional, default=user)<br>
   * @param pass : password<br>
   * @return : if message was sent to server successfully<br> This function does
   * not block waiting for a reply. You should receive onRegister() thru the
   * SIPClientInterface when a reply is returned from server.<br> Password may
   * be "." which causes getResponse() thru the SIPClientInterface to be called
   * for password verification, so you don't have to send password to
   * SIPClient.<br> This is useful if the client app communicates to the
   * SIPClient code remotely (as is the case with RTMP2SIP).<br>
   *
   */
  public boolean register(String user, String auth, String pass) {
    return register(user, auth, pass, 3600);
  }

  /**
   * Registers this client with the SIP server/proxy. <br>
   *
   * @param user : username<br>
   * @param auth : authorization name (optional, default=user)<br>
   * @param pass : password<br>
   * @param expires : number seconds to register for (0=unregisters)<br>
   * @return : if message was sent to server<br> This function does not block
   * waiting for a reply. You should receive either registered() or
   * unauthorized() thru the SIPClientInterface when a reply is returned from
   * server.<br>
   *
   */
  public boolean register(String user, String auth, String pass, int expires) {
    String regcallid = getcallid();
    CallDetails cd = getCallDetails(regcallid);  //new CallDetails
    if ((auth == null) || (auth.length() == 0)) {
      this.auth = user;
    } else {
      this.auth = auth;
    }
    this.user = user;
    this.pass = pass;
    cd.src.expires = expires;
    cd.src.to = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.contact = "<sip:" + user + "@" + getlocalhost(null) + ":" + localport + ">";
    cd.src.uri = "sip:" + remotehost;  // + ";rinstance=" + getrinstance();
    cd.src.branch = getbranch();
    cd.src.cseq++;
    if ((pass == null) || (pass.length() == 0)) {
      return true;  //non-register mode
    }
    cd.authsent = false;
    boolean ret = issue(cd, "REGISTER", null, false, true);
    return ret;
  }

  /**
   * Reregister with the server.
   */
  public boolean reregister() {
    return register(user, auth, pass);
  }

  /**
   * Reregister with the server using an expiration of 0 (zero). Effectively
   * unregisters.
   */
  public boolean unregister() {
    return register(user, auth, pass, 0);
  }

  /**
   * Publishes Presence to server. (not tested since Asterisk doesn't support
   * it)
   */
  public boolean publish(String state) {
    String pubcallid = getcallid();
    CallDetails cd = getCallDetails(pubcallid);  //new CallDetails
    cd.src.to = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.contact = "<sip:" + user + "@" + getlocalhost(null) + ":" + localport + ">";
    cd.src.uri = "sip:" + user + "@" + remotehost;
    cd.src.branch = getbranch();
    cd.src.cseq++;
    cd.sdp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><presence xmlns=\"urn:ietf:params:xml:ns:pidf\" entity=\"pres:" + user + "@" + remotehost
            + "\">" + "<tuple id=\"" + gettupleid() + "\"><status><basic>" + state + "</basic></status></tuple></presence>";
    cd.authsent = false;
    return issue(cd, "PUBLISH", "Event: presence\r\n", true, true);
  }

  /**
   * Subscribe to a user's presence on server.
   */
  public boolean subscribe(String subuser, String event, int expires) {
    String subcallid = getcallid();
    CallDetails cd = getCallDetails(subcallid);  //new CallDetails
    cd.src.to = new String[]{subuser, subuser, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.contact = "<sip:" + user + "@" + getlocalhost(null) + ":" + localport + ">";
    cd.src.uri = "sip:" + subuser + "@" + remotehost;
    cd.src.branch = getbranch();
    cd.src.cseq++;
    cd.src.expires = expires;
    return issue(cd, "SUBSCRIBE", "Accept: multipart/related, application/rlmi+xml, application/pidf+xml\r\nEvent: " + event + "\r\n", false, true);
  }

  /**
   * Send an empty SIP message to server. This should be done periodically to
   * keep firewalls open. Most routers close UDP connections after 60 seconds.
   */
  public void keepalive() {
    send(remoteip, remoteport, "\r\n");
  }

  /**
   * Determine if server is on a local private network.
   */
  private boolean isServerOnPrivateNetwork() {
    //in case your PBX is on your own local IP network
    if (remoteip.startsWith("192.168.")) {
      return true;
    }
    if (remoteip.startsWith("10.")) {
      return true;
    }
//    if (remoteip.startsWith("172.[16-31].")) return true;  //who uses that?
    return false;
  }

  /**
   * Returns local IP address.
   */
  public String getlocalhost(String host) {
    if (localhost != null) {
      return localhost;
    }
    findlocalhost();
    JFLog.log("localhost = " + localhost + " for remotehost = " + remotehost);
    return localhost;
  }
  private static final int tcpports[] = {22, 80};
  private static String inetip;

  /**
   * Determines local IP address. Uses several methods (such as
   * http://checkip.dyndns.org).
   */
  private void findlocalhost() {
    Socket s;
    if (!isServerOnPrivateNetwork()) {
      //Try using http://checkip.dyndns.org
      if (inetip != null) {
        localhost = inetip;
        return;
      }
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new URL("http://checkip.dyndns.org").openStream()));
        String line = reader.readLine();
        int idx = line.indexOf(':');
        line = line.substring(idx + 1);
        idx = line.indexOf('<');
        inetip = line.substring(0, idx).trim();
        localhost = inetip;
        return;
      } catch (Exception e3) {
      }
    }
    //try to detect local ip by connecting to remote using TCP on well known ports
    for (int a = 0; a < tcpports.length; a++) {
      try {
        s = new Socket();
        s.connect(new InetSocketAddress(remotehost, tcpports[a]), 1500);
      } catch (Exception e1) {
        continue;
      }
      localhost = s.getLocalAddress().getHostAddress();
      try {
        s.close();
      } catch (Exception e2) {
      }
      return;
    }
    //use java (not reliable on multi-homed systems)
    try {
      InetAddress local = InetAddress.getLocalHost();
      localhost = local.getHostAddress();
      return;
    } catch (Exception e4) {
    }
    //if all else fails
    localhost = "127.0.0.1";
  }

  private CallDetails getCallDetails(String callid) {
    CallDetails cd = cdlist.get(callid);
    if (cd == null) {
      cd = new CallDetails();
      JFLog.log("Create CallDetails:" + callid);
      cd.callid = callid;
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
  private boolean issue(CallDetails cd, String cmd, String extra, boolean sdp, boolean src) {
    CallDetails.SideDetails cdsd = (src ? cd.src : cd.dst);
    JFLog.log("callid:" + cd.callid + "\r\nissue command : " + cmd + " from : " + user + " to : " + remotehost);
    if (extra != null) {  //NOTE:refer has special headers and auth may need to add to them : BUG:this causes a build up of redudant headers sometimes
      if (cdsd.extra != null) {
        cdsd.extra += extra;
      } else {
        cdsd.extra = extra;
      }
    }
    cd.dst.host = remoteip;
    cd.dst.port = remoteport;
    StringBuffer req = new StringBuffer();
    req.append(cmd + " " + cdsd.uri + " SIP/2.0\r\n");
    req.append("Via: SIP/2.0/UDP " + getlocalhost(null) + ":" + localport + ";branch=" + cdsd.branch + ";rport\r\n");
    req.append("Max-Forwards: 70\r\n");
    req.append("Contact: " + cdsd.contact + "\r\n");
    req.append("To: " + join(cdsd.to) + "\r\n");
    req.append("From: " + join(cdsd.from) + "\r\n");
    req.append("Call-ID: " + cd.callid + "\r\n");
    req.append("Cseq: " + cdsd.cseq + " " + cmd + "\r\n");
    if ((cmd.equals("REGISTER")) || (cmd.equals("PUBLISH")) || (cmd.equals("SUBSCRIBE"))) {
      req.append("Expires: " + cdsd.expires + "\r\n");
    }
    req.append("Allow: INVITE, ACK, CANCEL, BYE, REFER, NOTIFY, OPTIONS\r\n");
    req.append("User-Agent: " + useragent + "\r\n");
    if (cdsd.extra != null) {
      req.append(cdsd.extra);
    }
    if ((cd.sdp != null) && (sdp)) {
      if (cd.sdp.startsWith("<?xml")) {
        req.append("Content-Type: application/pidf+xml\r\n");
      } else {
        req.append("Content-Type: application/sdp\r\n");
      }
      req.append("Content-Length: " + cd.sdp.length() + "\r\n\r\n");
      req.append(cd.sdp);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    return send(remoteip, remoteport, req.toString());
  }

  /**
   * Sends a reply to a SIP server.
   */
  private boolean reply(CallDetails cd, String cmd, int code, String msg, boolean sdp, boolean src) {
    JFLog.log("callid:" + cd.callid + "\r\nissue reply : " + code + " to : " + remotehost);
    CallDetails.SideDetails cdsd = (src ? cd.src : cd.dst);
    StringBuffer req = new StringBuffer();
    req.append("SIP/2.0 " + code + " " + msg + "\r\n");
    if (cdsd.vialist != null) {
      for (int a = 0; a < cdsd.vialist.length; a++) {
        req.append(cdsd.vialist[a]);
        req.append("\r\n");
      }
    }
    if ((cdsd.vialist == null) || (cdsd.vialist.length == 0)) {
      req.append("Via: SIP/2.0/UDP " + getlocalhost(null) + ":" + localport + ";branch=" + cdsd.branch + ";rport\r\n");
    }
    req.append("Contact: " + cdsd.contact + "\r\n");
    req.append("To: " + join(cdsd.to) + "\r\n");
    req.append("From: " + join(cdsd.from) + "\r\n");
    req.append("Call-ID: " + cd.callid + "\r\n");
    req.append("Cseq: " + cdsd.cseq + " " + cmd + "\r\n");
    req.append("Allow: INVITE, ACK, CANCEL, BYE, REFER, NOTIFY, OPTIONS\r\n");
    req.append("User-Agent: JavaForce\r\n");
    if ((cd.sdp != null) && (sdp)) {
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + cd.sdp.length() + "\r\n\r\n");
      req.append(cd.sdp);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    send(remoteip, remoteport, req.toString());
    return true;
  }

  /**
   * Send an invite to server.<br>
   *
   * @return unique Call-ID (not caller id)<br>
   */
  public String invite(String to, int rtp_port_audio, int rtp_port_video, Codec codecs[]) {
    String callid = getcallid();
    CallDetails cd = getCallDetails(callid);  //new CallDetails
    cd.src.to = new String[]{to, to, remotehost + ":" + remoteport, ":"};
    cd.src.from = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.src.contact = "<sip:" + user + "@" + getlocalhost(null) + ":" + localport + ">";
    cd.src.uri = "sip:" + to + "@" + remotehost + ":" + remoteport;
    cd.src.from = replacetag(cd.src.from, generatetag());
    cd.src.branch = getbranch();
    cd.src.o1 = 256;
    cd.src.o2 = 256;
    cd.src.codecs = codecs;
    cd.src.rtp_port_audio = rtp_port_audio;
    cd.src.rtp_port_video = rtp_port_video;
    buildsdp(cd, cd.src);
    cd.src.cseq++;
    cd.authsent = false;
    if (!issue(cd, "INVITE", null, true, true)) {
      return null;
    }
    return callid;
  }

  /**
   * Send a refer command to server.
   */
  public boolean refer(String callid, String to) {
    String headers = "Refer-To: <sip:" + to + "@" + remotehost + ">\r\nReferred-By: <sip:" + user + "@" + remotehost + ":" + localport + ">\r\n";
    CallDetails cd = getCallDetails(callid);
    cd.src.cseq++;
    cd.authsent = false;
    boolean ret = issue(cd, "REFER", headers, false, true);  //NOTE:auth may add to headers later!!!
    return ret;
  }

  /**
   * Sends a reINVITE to server with a special SDP packet to place the call on
   * hold.
   */
  public boolean hold(String callid, int rtp_port_audio) {
    CallDetails cd = getCallDetails(callid);
    cd.src.o2++;
    cd.holding = true;
    cd.src.rtp_port_audio = rtp_port_audio;
    buildsdp(cd, cd.src);
//    cd.src.cseq++;  //do NOT inc on hold request
    cd.authsent = false;
    if (!issue(cd, "INVITE", null, true, true)) {
      return false;
    }
    return true;
  }

  /**
   * Sends a reINVITE to server with a special SDP packet to remove the hold on
   * a call or change other SDP details.
   */
  public boolean reinvite(String callid, int rtp_port_audio, Codec codecs[]) {
    CallDetails cd = getCallDetails(callid);
    cd.src.o2++;
    cd.holding = false;
//    for(int a=0;a<codecs.length;a++) JFLog.log("reinvite : codecs[]=" + codecs[a].name + ":" + codecs[a].id);
    cd.src.codecs = codecs;
    cd.src.rtp_port_audio = rtp_port_audio;
    buildsdp(cd, cd.src);
    cd.src.cseq++;
    cd.authsent = false;
    if (!issue(cd, "INVITE", null, true, true)) {
      return false;
    }
    return true;
  }

  /**
   * Cancels an INVITE request. Usually done while phone is ringing.
   */
  public boolean cancel(String callid) {
    CallDetails cd = getCallDetails(callid);
    String epass;
    if (cd.authstr != null) {
      epass = getAuthResponse(cd.authstr, auth, pass, remotehost, "BYE", "Proxy-Authorization:");
    } else {
      epass = null;
    }
    cd.authsent = false;
    cd.src.to = cd.dst.to;
    cd.src.from = cd.dst.from;
    boolean ret = issue(cd, "CANCEL", epass, false, true);
    return ret;
  }

  /**
   * Send a request to terminate a call in progress.
   */
  public boolean bye(String callid) {
    CallDetails cd = getCallDetails(callid);
    String epass;
    if (cd.authstr != null) {
      epass = getAuthResponse(cd.authstr, auth, pass, remotehost, "BYE", "Proxy-Authorization:");
    } else {
      epass = null;
    }
    cd.src.cseq++;
    cd.authsent = false;
    boolean ret = issue(cd, "BYE", epass, false, true);
    return ret;
  }

  /**
   * Sends a reply to accept an inbound INVITE.
   */
  public boolean accept(String callid, int rtp_port_audio, int rtp_port_video, Codec codecs[]) {
    CallDetails cd = getCallDetails(callid);
    cd.src.codecs = codecs;
    cd.src.rtp_port_audio = rtp_port_audio;
    cd.src.rtp_port_video = rtp_port_video;
    buildsdp(cd, cd.src);
    reply(cd, "INVITE", 200, "OK", true, false);
    //need to swap to/from for BYE
    cd.src.to = cd.dst.from;
    cd.src.from = cd.dst.to;
    //copy all other needed fields
    cd.src.uri = cd.dst.uri;
    cd.src.contact = "<sip:" + user + "@" + getlocalhost(null) + ":" + localport + ">";
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

  /**
   * Processes SIP messages sent from the SIP server.
   */
  public void packet(String msg[], String remoteip, int remoteport) {
    try {
      if (!remoteip.equals(this.remoteip) || remoteport != this.remoteport) {
        JFLog.log("Ignoring packet from unknown host:" + remoteip + ":" + remoteport);
        return;
      }
      String tmp, req = null, epass;
      int idx;
      String callid = getHeader("Call-ID:", msg);
      if (callid == null) {
        callid = getHeader("i:", msg);
        if (callid == null) {
          JFLog.log("Bad packet (no Call-ID) from:" + remoteip + ":" + remoteport);
          return;
        }
      }
      CallDetails cd = getCallDetails(callid);
      cd.dst.host = remoteip;
      cd.dst.port = remoteport;
      cd.dst.cseq = getcseq(msg);
      cd.dst.branch = getbranch();
      //get cd.dst.to
      tmp = getHeader("To:", msg);
      if (tmp == null) {
        tmp = getHeader("t:", msg);
      }
      cd.dst.to = split(tmp);
      //get cd.dst.from
      tmp = getHeader("From:", msg);
      if (tmp == null) {
        tmp = getHeader("f:", msg);
      }
      cd.dst.from = split(tmp);
      //get via list
      cd.dst.vialist = getvialist(msg);
      //set contact
      cd.dst.contact = "<sip:" + user + "@" + getlocalhost(null) + ":" + localport + ">";
      //get uri (it must equal the Contact field)
      cd.dst.uri = getHeader("Contact:", msg);
      if (cd.dst.uri == null) {
        cd.dst.uri = getHeader("m:", msg);
      }
      if (cd.dst.uri != null) {
        cd.dst.uri = cd.dst.uri.substring(1, cd.dst.uri.length() - 1);  //remove < > brackets
      }
      String cmd = getcseqcmd(msg);
      int type = getResponseType(msg);
      if (type != -1) {
        JFLog.log("callid:" + callid + "\r\nreply=" + type);
      } else {
        req = getRequest(msg);
        JFLog.log("callid:" + callid + "\r\nrequest=" + req);
      }
      switch (type) {
        case -1:
          if (req.equals("INVITE")) {
            //get SDP details
            //get RTP info
            String remotertphost = getremotertphost(msg);
            int remotertpport = getremotertpport(msg);
            int remoteVrtpport = getremoteVrtpport(msg);
            //get codecs
            Codec codecs[] = getCodecs(msg);
            //generate toTag
            cd.dst.to = replacetag(cd.dst.to, generatetag());
            //get o1/o2
            cd.dst.o1 = geto(msg, 1) + 1;
            cd.dst.o2 = geto(msg, 2) + 1;
            cd.onhold = ishold(msg);
            cd.src.codecs = codecs;
            switch (iface.onInvite(this, callid, cd.dst.from[0], cd.dst.from[1], remotertphost, remotertpport, remoteVrtpport, codecs)) {
              case 180:  //this is the normal return
                reply(cd, cmd, 180, "RINGING", false, false);
                break;
              case 200:  //this was usually a reINVITE to change sdp details
                buildsdp(cd, cd.src);
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
            reply(cd, "INVITE", 487, "CANCELLED", false, false);
            JF.sleep(1);  //just in case
            reply(cd, cmd, 200, "OK", false, false);
//            setCallDetails(callid, null);
            break;
          }
          if (req.equals("BYE")) {
            reply(cd, cmd, 200, "OK", false, false);
            iface.onBye(this, callid);
//            setCallDetails(callid, null);
            break;
          }
          if (req.equals("OPTIONS")) {
            reply(cd, cmd, 200, "OK", false, false);
            break;
          }
          if (req.equals("NOTIFY")) {
            reply(cd, cmd, 200, "OK", false, false);
            for (int a = 0; a < msg.length; a++) {
              if (msg[a].length() == 0) {
                String content = "";
                for (int b = a + 1; b < msg.length; b++) {
                  content += msg[b];
                  content += "\r\n";
                }
                iface.onNotify(this, getHeader("Event:", msg), content);
              }
            }
            break;
          }
          break;
        case 100:
          iface.onTrying(this, callid);
          break;
        case 180:
        case 183:
          iface.onRinging(this, callid);
          break;
        case 200:
          if (cmd.equals("REGISTER")) {
            if (cd.src.expires > 0) {
              registered = true;
              iface.onRegister(this, true);
            } else {
              registered = false;
            }
          } else if (cmd.equals("INVITE")) {
            String remotertphost = getremotertphost(msg);
            int remotertpport = getremotertpport(msg);
            int remoteVrtpport = getremoteVrtpport(msg);
            Codec codecs[] = getCodecs(msg);
            //update uri
            cd.dst.uri = getHeader("Contact:", msg);
            if (cd.dst.uri == null) {
              cd.dst.uri = getHeader("m:", msg);
            }
            cd.dst.uri = cd.dst.uri.substring(1, cd.dst.uri.length() - 1);  //remove < > brackets
            //update cd.dst.to tag value
            cd.dst.to = replacetag(cd.dst.to, getHeader("To:", msg));
            cd.dst.to = replacetag(cd.dst.to, getHeader("t:", msg));
            cd.src.to = cd.dst.to;
            issue(cd, "ACK", null, false, true);
            iface.onSuccess(this, callid, remotertphost, remotertpport, remoteVrtpport, codecs);
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
            issue(cd, "ACK", null, false, true);
            cd.authstr = getHeader("WWW-Authenticate:", msg);
            if (cd.authstr == null) {
              cd.authstr = getHeader("Proxy-Authenticate:", msg);
            }
            if (cd.authstr == null) {
              JFLog.log("err:401/407 without Authenticate tag");
              break;
            }
            epass = getAuthResponse(cd.authstr, auth, pass, remotehost, cmd, (type == 401 ? "Authorization:" : "Proxy-Authorization:"));
            if (epass == null) {
              JFLog.log("err:gen auth failed");
              break;
            }
            cd.src.cseq++;
            cd.src.extra = null;  //delete any prev. attempts
            issue(cd, cmd, epass, cmd.equals("INVITE"), true);
            cd.authsent = true;
          }
          break;
        case 403:
          issue(cd, "ACK", null, false, true);
          if (cmd.equals("REGISTER")) {
            //bad password
            iface.onRegister(this, false);
          } else {
            iface.onCancel(this, callid, type);
          }
          break;
        default:
          //treat all other codes as a cancel
          issue(cd, "ACK", null, false, true);
          iface.onCancel(this, callid, type);
//          setCallDetails(callid, null);
          break;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getResponse(String realm, String cmd, String uri, String nonce, String qop, String nc, String cnonce) {
    return iface.getResponse(this, realm, cmd, uri, nonce, qop, nc, cnonce);
  }
}
