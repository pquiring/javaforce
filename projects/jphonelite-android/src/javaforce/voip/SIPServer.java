package javaforce.voip;

import java.io.*;
import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Handles the server end of a SIP link.
 */
public class SIPServer extends SIP implements SIPInterface {

  private String localip, publicip;
  private int localport;
  private Hashtable<String, CallDetailsServer> cdlist;
  private SIPServerInterface iface;
  private boolean use_qop = false;

  public boolean init(int localport, SIPServerInterface iface) {
    this.iface = iface;
    this.localport = localport;
    cdlist = new Hashtable<String, CallDetailsServer>();
    try {
      JFLog.log("Starting SIPServer on port " + localport + "...");
      super.init(localport, this, true);
    } catch (Exception e) {
      JFLog.log("SIPServer:init() failed : " + e);
      return false;
    }
    return true;
  }

  public void uninit() {
    super.uninit();
  }

  public void enableQOP(boolean state) {
    use_qop = state;
  }

  public CallDetailsServer getCallDetailsServer(String callid) {
    CallDetailsServer cd = cdlist.get(callid);
    if (cd == null) {
      cd = iface.createCallDetailsServer();
      cd.sip = this;
      cd.callid = callid;
      setCallDetailsServer(callid, cd);
    }
    return cd;
  }

  public void setCallDetailsServer(String callid, CallDetailsServer cd) {
    if (cd == null) {
      cdlist.remove(callid);
    } else {
      cdlist.put(callid, cd);
    }
  }

  public boolean issue(CallDetailsServer cd, String header, boolean sdp, boolean src) {
    CallDetails.SideDetails cdsd = (src ? cd.pbxsrc : cd.pbxdst);
    JFLog.log("callid:" + cd.callid + "\r\nissue command : " + cd.cmd + " from : " + cd.user + " to : " + cdsd.host + ":" + cdsd.port);
    StringBuffer req = new StringBuffer();
    req.append(cd.cmd + " " + cdsd.uri + " SIP/2.0\r\n");
    req.append("Via: SIP/2.0/UDP " + getlocalhost(null) + ":" + localport + ";branch=" + cdsd.branch + ";rport\r\n");
    req.append("Max-Forwards: 70\r\n");
    req.append("Contact: " + cdsd.contact + "\r\n");
    req.append("To: " + join(cdsd.to) + "\r\n");
    req.append("From: " + join(cdsd.from) + "\r\n");
    req.append("Call-ID: " + cd.callid + "\r\n");
    req.append("Cseq: " + cdsd.cseq + " " + cd.cmd + "\r\n");
    req.append("Allow: INVITE, ACK, CANCEL, BYE, REFER, NOTIFY, OPTIONS\r\n");
    req.append("User-Agent: " + useragent + "\r\n");
    if (header != null) {
      req.append(header);
    }
    if ((cd.sdp != null) && (sdp)) {
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + cd.sdp.length() + "\r\n\r\n");
      req.append(cd.sdp);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    return send(cdsd.host, cdsd.port, req.toString());
  }

  public boolean reply(CallDetailsServer cd, int code, String msg, String header, boolean sdp, boolean src) {
    CallDetails.SideDetails cdsd = (src ? cd.pbxsrc : cd.pbxdst);
    JFLog.log("callid:" + cd.callid + "\r\nissue reply : " + code + " to : " + cdsd.host + ":" + cdsd.port);
    StringBuffer req = new StringBuffer();
    req.append("SIP/2.0 " + code + " " + msg + "\r\n");
    if (cdsd.vialist != null) {
      for (int a = 0; a < cdsd.vialist.length; a++) {
        req.append(cdsd.vialist[a]);
        req.append("\r\n");
      }
    }
    if (code < 400) {
      req.append("Contact: " + cdsd.contact + "\r\n");
    }
    req.append("To: " + join(cdsd.to) + "\r\n");
    req.append("From: " + join(cdsd.from) + "\r\n");
    req.append("Call-ID: " + cd.callid + "\r\n");
    req.append("Cseq: " + cdsd.cseq + " " + cd.cmd + "\r\n");
    req.append("Allow: INVITE, ACK, CANCEL, BYE, REFER, NOTIFY, OPTIONS\r\n");
    req.append("User-Agent: " + useragent + "\r\n");
    if (header != null) {
      req.append(header);
    }
    if ((cd.sdp != null) && (sdp)) {
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + cd.sdp.length() + "\r\n\r\n");
      req.append(cd.sdp);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    return send(cdsd.host, cdsd.port, req.toString());
  }

  private boolean isLocalHost(String host) {
    if (host == null) {
      return false;
    }
    if (host.equals("0:0:0:0:0:0:0:1")) {
      return true;  //IP6?
    }
    if (host.equals("127.0.0.1")) {
      return true;
    }
    if (host.startsWith("192.168.")) {
      return true;
    }
    if (host.startsWith("10.")) {
      return true;
    }
//    if (remoteip.startsWith("172.[16-31].")) return true;  //who uses that?
    return false;
  }

  public String getlocalhost(String host) {
    //if host is local network use local ip instead of detected publicip
    if (isLocalHost(host)) {
      //detect and use localhost
      if (localip == null) {
        try {
          InetAddress local = InetAddress.getLocalHost();
          localip = local.getHostAddress();
          JFLog.log("Detected Local IP=" + localip);
        } catch (Exception e) {
          JFLog.log(e);
          return null;
        }
      }
      return localip;
    }
    //detect and use publicip
    if (publicip != null) {
      return publicip;
    }
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
              new URL("http://checkip.dyndns.org").openStream()));
      String line = reader.readLine();
      int idx = line.indexOf(':');
      line = line.substring(idx + 1);
      idx = line.indexOf('<');
      publicip = line.substring(0, idx).trim();
      JFLog.log("Detected Public IP=" + publicip);
    } catch (Exception e3) {
      JFLog.log(e3);
    }
    return publicip;
  }

  public void setlocalip(String ip) {
    this.localip = ip;
  }

  public void setpublicip(String ip) {
    this.publicip = ip;
  }

  public boolean register(String user, String pass, String remotehost, int remoteport, int expires, String did, String regcallid) {
    //NOTE : There is no dst in a register, it's a one-sided call
    CallDetailsServer cd = getCallDetailsServer(regcallid);
    cd.user = user;
    cd.pass = pass;
    cd.pbxsrc.expires = expires;
    cd.pbxsrc.to = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.pbxsrc.from = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.pbxsrc.from = replacetag(cd.pbxsrc.from, generatetag());
    cd.pbxsrc.contact = "<sip:" + did + "@" + getlocalhost(null) + ":" + localport + ">";
    cd.pbxsrc.uri = "sip:" + remotehost;  // + ";rinstance=" + getrinstance();
    cd.callid = regcallid;
    cd.pbxsrc.branch = getbranch();
    cd.pbxsrc.cseq++;
    cd.cmd = "REGISTER";
    cd.src.host = cd.pbxsrc.host = remotehost;
    cd.src.port = cd.pbxsrc.port = remoteport;
    cd.authsent = false;
    boolean ret = issue(cd, null, false, true);
    return ret;
  }

  //copies "some" fields from src to dest
  public void clone(CallDetails.SideDetails src, CallDetails.SideDetails dst) {
    dst.host = src.host;
    dst.port = src.port;
    dst.to = src.to.clone();
    dst.from = src.from.clone();
    dst.uri = src.uri;
    dst.cseq = src.cseq;
    dst.branch = src.branch;
    dst.contact = src.contact;
    dst.vialist = src.vialist;
  }

  public void packet(String msg[], String remoteip, int remoteport) {
    try {
      String tmp, req = null, epass;
      int idx;
      String callid = getHeader("Call-ID:", msg);
      if (callid == null) {
        callid = getHeader("i:", msg);  //callcentric.com
        if (callid == null) {
          JFLog.log("Bad packet (no Call-ID) from:" + remoteip + ":" + remoteport);
          return;
        }
      }
      CallDetailsServer cd = getCallDetailsServer(callid);
      boolean src;
      CallDetails.SideDetails cdsd = null;
      CallDetails.SideDetails cdpbx = null;
//      if (remoteip.equals("127.0.0.1")) remoteip = getlocalhost(null);
      //update CallDetailsServer
      synchronized (cd.lock) {
        if ((cd.src.host == null) && (cd.dst.host == null)) {
          //new call leg (assign this side to src)
          src = true;
          cdsd = cd.src;
          cdpbx = cd.pbxsrc;
        } else {
          if (cd.src.host != null && resolve(cd.src.host).equals(remoteip) && cd.src.port == remoteport) {
            src = true;
            cdsd = cd.src;
            cdpbx = cd.pbxsrc;
          } else if (cd.dst.host != null && resolve(cd.dst.host).equals(remoteip) && cd.dst.port == remoteport) {
            src = false;
            cdsd = cd.dst;
            cdpbx = cd.pbxdst;
          } else {
            JFLog.log("Ignoring packet from unknown host:" + remoteip + ":" + remoteport);
            return;  //you were not invited to this party
          }
        }
        cdsd.cseq = getcseq(msg);
        cdsd.host = remoteip;
        cdsd.port = remoteport;
        cdsd.branch = getbranch();
        //get cd.to
        tmp = getHeader("To:", msg);
        if (tmp == null) {
          tmp = getHeader("t:", msg);
        }
        cdsd.to = split(tmp);
        //get cd.from
        tmp = getHeader("From:", msg);
        if (tmp == null) {
          tmp = getHeader("f:", msg);
        }
        cdsd.from = split(tmp);
        //extract user from cd.from "display" <sip:user@host>;tag=...
        cd.user = cdsd.from[1];
        //get via list
        cdsd.vialist = getvialist(msg);
        //set contact
        cdsd.contact = "<sip:" + cd.user + "@" + getlocalhost(null) + ":" + localport + ">";
        //get uri (it must equal the Contact field)
        cdsd.uri = getHeader("Contact:", msg);
        if (cdsd.uri == null) {
          cdsd.uri = getHeader("m:", msg);
        }
        if (cdsd.uri != null) {
          cdsd.uri = cdsd.uri.substring(1, cdsd.uri.length() - 1);  //remove < > brackets
        }
        cd.cmd = getcseqcmd(msg);
        int type = getResponseType(msg);
        if (type != -1) {
          JFLog.log("callid:" + callid + "\r\nreply=" + type + " from " + remoteip + ":" + remoteport);
        } else {
          req = getRequest(msg);
          JFLog.log("callid:" + callid + "\r\nrequest=" + req + " from " + remoteip + ":" + remoteport);
        }
        switch (type) {
          case -1:
            clone(cdsd, cdpbx);
            if (req.equalsIgnoreCase("REGISTER")) {
              String resln = getHeader("Authorization:", msg);
              if (resln == null) {
                //send a 401
                cd.nonce = getnonce();
                String challenge = "WWW-Authenticate: Digest algorithm=MD5, realm=\"jpbx\", nonce=\"" + cd.nonce + "\"";
                if (use_qop) {
                  challenge += ", qop=\"auth\"";
                }
                challenge += "\r\n";
                reply(cd, 401, "REQ AUTH", challenge, false, src);
                break;
              }
              if (!resln.regionMatches(true, 0, "digest ", 0, 7)) {
                break;
              }
              String tags[] = resln.substring(7).replaceAll(" ", "").replaceAll("\"", "").split(",");
              String res = getHeader("response=", tags);
              String nonce = getHeader("nonce=", tags);
              if ((nonce == null) || (cd.nonce == null) || (!cd.nonce.equals(nonce))) {
                //send another 401
                cd.nonce = getnonce();
                String challenge = "WWW-Authenticate: Digest algorithm=MD5, realm=\"jpbx\", nonce=\"" + cd.nonce + "\"";
                if (use_qop) {
                  challenge += ", qop=\"auth\"";
                }
                challenge += "\r\n";
                reply(cd, 401, "REQ AUTH", challenge, false, src);
                break;
              }
              String test = getResponse(cd.user, iface.getPassword(cd.user), "jpbx", cd.cmd, getHeader("uri=", tags), cd.nonce, getHeader("qop=", tags),
                      getHeader("nc=", tags), getHeader("cnonce=", tags));
              cd.nonce = null;  //don't allow value to be reused
              if (!res.equalsIgnoreCase(test)) {
                reply(cd, 403, "BAD PASSWORD", null, false, src);
                break;
              }
              //REGISTER OK
              iface.onRegister(cd.user, getexpires(msg), remoteip, remoteport);
              reply(cd, 200, "OK", null, false, src);
              break;
            }
            if (req.equalsIgnoreCase("INVITE")) {
              String pass = iface.getPassword(cd.user);
              if (pass != null) {
                //do auth only if has a password
                String resln = getHeader("Proxy-Authorization:", msg);
                if ((resln == null) || (cd.nonce == null)) {
                  //send a 407
                  cd.nonce = getnonce();
                  String challenge = "Proxy-Authenticate: Digest algorithm=MD5, realm=\"jpbx\", nonce=\"" + cd.nonce + "\"\r\n";
                  reply(cd, 407, "REQ AUTH", challenge, false, src);
                  break;
                }
                if (!resln.regionMatches(true, 0, "digest ", 0, 7)) {
                  break;
                }
                String tags[] = resln.substring(7).replaceAll(" ", "").replaceAll("\"", "").split(",");
                String res = getHeader("response=", tags);
                String nonce = getHeader("nonce=", tags);
                if ((nonce == null) || (!cd.nonce.equals(nonce))) {
                  //send another 407
                  cd.nonce = getnonce();
                  String challenge = "Proxy-Authenticate: Digest algorithm=MD5, realm=\"jpbx\", nonce=\"" + cd.nonce + "\"\r\n";
                  reply(cd, 407, "REQ AUTH", challenge, false, src);
                  break;
                }
                String test = getResponse(cd.user, pass, "jpbx", cd.cmd, getHeader("uri=", tags), cd.nonce, null, null, null);
                cd.nonce = null;  //don't allow value to be reused
                if (!res.equalsIgnoreCase(test)) {
                  reply(cd, 403, "BAD PASSWORD", null, false, src);
                  break;
                }
                iface.onRegister(cd.user, 3600, remoteip, remoteport);  //BUG - this assumes expires is 3600
                //no break
              }
              //get dialed # (if INVITE)
              cd.dialed = cdsd.to[1];
              //split cd.from into parts
              cd.fromname = cdsd.from[0];
              cd.fromnumber = cdsd.from[1];
              //get SDP details
              //get RTP info
              cdsd.rtp_host = getremotertphost(msg);
              cdsd.rtp_port_audio = getremotertpport(msg);
              //get codecs
              cdsd.codecs = getCodecs(msg);
              //get o1/o2
              cdsd.o1 = geto(msg, 1);
              cdsd.o2 = geto(msg, 2);
              cd.onhold = ishold(msg);
              cd.authorized = (pass != null);
              iface.onInvite(cd, src);
              break;
            }
            if (req.equalsIgnoreCase("CANCEL")) {
              iface.onCancel(cd, src);
              setCallDetailsServer(callid, null);
              break;
            }
            if (req.equalsIgnoreCase("BYE")) {
              //BUG : can't delete calldetails yet (memory leak)
              iface.onBye(cd, src);
              break;
            }
            if (req.equalsIgnoreCase("ACK")) {
              //TODO : ???
              break;
            }
            if (req.equalsIgnoreCase("REFER")) {
              iface.onFeature(cd, req, getHeader("Refer-To:", msg), src);
              break;
            }
            if (req.equalsIgnoreCase("OPTIONS")) {
              //send 200 and ignore
              reply(cd, 200, "OK", null, false, src);
              break;
            }
            if (req.equalsIgnoreCase("SUBSCRIBE")) {
              //send 200 and ignore
              reply(cd, 200, "OK", null, false, src);
              break;
            }
            if (req.equalsIgnoreCase("SHUTDOWN")) {
              iface.onFeature(cd, req, remoteip, src);
              break;
            }
            break;
          case 100:
            iface.onTrying(cd, src);
            break;
          case 180:
          case 183:
            iface.onRinging(cd, src);
            break;
          case 200:
            if (cd.cmd.equals("INVITE")) {
              //update tag
              cdsd.to = replacetag(cdsd.to, getHeader("To:", msg));
              cdsd.to = replacetag(cdsd.to, getHeader("t:", msg));
              cdpbx.to = cdsd.to.clone();
              cdsd.rtp_host = getremotertphost(msg);
              cdsd.rtp_port_audio = getremotertpport(msg);
              cdsd.o1 = geto(msg, 1);
              cdsd.o2 = geto(msg, 2);
              cdsd.codecs = getCodecs(msg);
            }
            if (cd.cmd.equals("BYE")) {
              setCallDetailsServer(cd.callid, null);
              break;
            }
            if (cd.cmd.equals("REGISTER")) {
              //send ACK and ignore
              cd.cmd = "ACK";
              issue(cd, null, false, src);
              break;
            }
            iface.onSuccess(cd, src);
            break;
          case 401:
            if (cd.cmd.equals("REGISTER")) {
              cd.cmd = "ACK";
              issue(cd, null, false, src);
              cd.cmd = "REGISTER";
              if (cd.authsent) {
                JFLog.log("Server Error : Double 401");
                break;
              }
              cd.authstr = getHeader("WWW-Authenticate:", msg);
              epass = getAuthResponse(cd.authstr, cd.user, cd.pass, cdpbx.host, cd.cmd, "Authorization:");
              if (epass == null) {
                JFLog.log("err:gen auth failed");
                break;
              }
              cdsd.cseq++;
              cd.authsent = true;
              issue(cd, epass, false, src);
            }
            break;
          case 407:
            //BUG : Should the cdsd.contact be changed to:
            //  "<sip:" + did + "@" + getlocalhost(null) + ":" + localport + ">";
            //like it is in register() above?  Because it wouldn't have been in the initial INVITE.
            if (cd.cmd.equals("INVITE")) {
              cd.cmd = "ACK";
              issue(cd, null, false, src);
              if (cd.authsent) {
                JFLog.log("Server Error : Double 407");
                break;
              }
              String reg = iface.getTrunkRegister(remoteip);  //user : pass @ host / did
              if (reg == null) {
                JFLog.log("TRUNK : 407 : no register string for trunk");
                break;
              }
              int idx1 = reg.indexOf(":");
              int idx2 = reg.indexOf("@");
              if ((idx1 == -1) || (idx2 == -1)) {
                JFLog.log("TRUNK : 407 : invalid register string for trunk");
                break;
              }
              String trunk_user = reg.substring(0, idx1);
              String trunk_pass = reg.substring(idx1 + 1, idx2);
              cd.authstr = getHeader("Proxy-Authenticate:", msg);
              epass = getAuthResponse(cd.authstr, trunk_user, trunk_pass, cdpbx.host, cd.cmd, "Proxy-Authorization:");
              if (epass == null) {
                JFLog.log("err:gen auth failed");
                break;
              }
              cdsd.cseq++;
              cd.authsent = true;
              issue(cd, epass, true, src);
            }
            break;
          default:
            iface.onError(cd, type, src);
            break;
        }
      }  //synchronized
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //not used
  public String getResponse(String realm, String cmd, String uri, String nonce, String qop, String nc, String cnonce) {
    return null;
  }
}
