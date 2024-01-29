package javaforce.voip;

import java.io.*;
import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Handles the server end of a SIP link.
 */
public class SIPServer extends SIP implements SIPInterface {

  private int localport;
  private String localhost;
  private HashMap<String, CallDetailsServer> cdlist;
  private SIPServerInterface iface;
  private boolean use_qop = false;
  private static final String realm = "javaforce";

  private static class Trunk {
    public String user, auth, pass;
  }

  private HashMap<String, Trunk> trunks = new HashMap<String, Trunk>();

  public boolean init(int localport, SIPServerInterface iface, TransportType type) {
    this.iface = iface;
    this.localport = localport;
    cdlist = new HashMap<String, CallDetailsServer>();
    try {
      JFLog.log("Starting SIP Server on port " + localport);
      super.init(localhost, localport, this, true, type);
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
      cd.localhost = localhost;
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
    StringBuilder req = new StringBuilder();
    req.append(cd.cmd + " " + cd.uri + " SIP/2.0\r\n");
    req.append("Via: SIP/2.0/UDP " + cd.localhost + ":" + localport + ";branch=" + cdsd.branch + "\r\n");
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
      String post = String.join("\r\n",cd.sdp) + "\r\n";
      if (cd.cmd.equals("MESSAGE")) {
        req.append("Content-Type: text/plain\r\n");
      } else {
        req.append("Content-Type: application/sdp\r\n");
      }
      req.append("Content-Length: " + post.length() + "\r\n\r\n");
      req.append(post);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    if (cdsd.addr == null) {
      try {
        cdsd.addr = InetAddress.getByName(cdsd.host);
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
    return send(cdsd.addr, cdsd.port, req.toString());
  }

  public boolean reply(CallDetailsServer cd, int code, String msg, String header, boolean sdp, boolean src) {
    CallDetails.SideDetails cdsd = (src ? cd.pbxsrc : cd.pbxdst);
    JFLog.log("callid:" + cd.callid + "\r\nissue reply : " + code + " to : " + cdsd.host + ":" + cdsd.port);
    StringBuilder req = new StringBuilder();
    req.append("SIP/2.0 " + code + " " + msg + "\r\n");
    if (cdsd.vialist != null) {
      for (int a = 0; a < cdsd.vialist.length; a++) {
        if (a == 0) {
          //add received to first via entry (and rport if requested)
          String via = cdsd.vialist[a];
          String[] f = via.split(";");
          StringBuilder sb = new StringBuilder();
          for(int b=0;b<f.length;b++) {
            if (f[b].equals("rport")) {
              f[b] = "rport=" + cdsd.port;
            }
            sb.append(f[b]);
            sb.append(";");
          }
          sb.append("received=" + cdsd.host);
          req.append(sb.toString());
          req.append("\r\n");
        } else {
          req.append(cdsd.vialist[a]);
          req.append("\r\n");
        }
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
      String post = String.join("\r\n", cd.sdp) + "\r\n";
      req.append("Content-Type: application/sdp\r\n");
      req.append("Content-Length: " + post.length() + "\r\n\r\n");
      req.append(post);
    } else {
      req.append("Content-Length: 0\r\n\r\n");
    }
    if (cdsd.addr == null) {
      try {
        cdsd.addr = InetAddress.getByName(cdsd.host);
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
    return send(cdsd.addr, cdsd.port, req.toString());
  }

  public String getlocalRTPhost(CallDetails cd) {
    return cd.localhost;
  }

  public boolean register(String user, String pass, String remotehost, int remoteport, int expires, String did, String regcallid) {
    String key = remotehost + ":" + remoteport;
    Trunk trunk = new Trunk();
    trunk.user = user;
    trunk.auth = user;
    trunk.pass = pass;
    trunks.put(key, trunk);
    //NOTE : There is no dst in a register, it's a one-sided call
    CallDetailsServer cd = getCallDetailsServer(regcallid);
    cd.user = user;
    cd.pass = pass;
    cd.pbxsrc.expires = expires;
    cd.pbxsrc.to = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.pbxsrc.from = new String[]{user, user, remotehost + ":" + remoteport, ":"};
    cd.pbxsrc.from = replacetag(cd.pbxsrc.from, generatetag());
    cd.pbxsrc.contact = "<sip:" + did + "@" + cd.localhost + ":" + localport + ">";
    cd.uri = "sip:" + remotehost;  // + ";rinstance=" + getrinstance();
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
    dst.cseq = src.cseq;
    dst.branch = src.branch;
    dst.contact = src.contact;
    dst.vialist = src.vialist;
  }

  public void packet(String[] msg, String remoteip, int remoteport) {
    try {
      String tmp, cmd = null, epass;
      String callid = getHeader("Call-ID:", msg);
      if (callid == null) callid = getHeader("i:", msg);
      if (callid == null) {
        JFLog.log("Bad packet (no Call-ID) from:" + remoteip + ":" + remoteport);
        return;
      }
      CallDetailsServer cd = getCallDetailsServer(callid);
      if (cd.localhost == null && !msg[0].startsWith("SIP/")) {
        String[] f = msg[0].split(" ");  //REQUEST sip:[ext@]HOST[:port] SIP/2.0
        String sip = f[1];
        if (sip.startsWith("sip:")) {
          sip = sip.substring(4);
        }
        int idx1 = sip.indexOf("@");
        if (idx1 == -1) {
          idx1 = 0;
        } else {
          idx1++;
        }
        int idx2 = sip.indexOf(":");
        String host;
        if (idx2 == -1) {
          host = sip.substring(idx1);
        } else {
          host = sip.substring(idx1, idx2);
        }
        cd.localhost = InetAddress.getByName(host).getHostAddress();
        JFLog.log("server address=" + cd.localhost);
        localhost = cd.localhost;
      }
      cd.lastPacket = System.currentTimeMillis();
      boolean src = false;
      CallDetails.SideDetails cdsd = null;
      CallDetails.SideDetails cdpbx = null;
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
        cdsd.branch = getbranch(msg);
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
        //get uri (it must equal the Contact field)
        cdsd.contact = getHeader("Contact:", msg);
        if (cdsd.contact == null) {
          cdsd.contact = getHeader("m:", msg);
        }
        cd.cmd = getcseqcmd(msg);
        int reply = getResponseType(msg);
        if (reply != -1) {
          JFLog.log("callid:" + callid + "\r\nreply=" + reply + " from " + remoteip + ":" + remoteport);
        } else {
          cmd = getRequest(msg);
          JFLog.log("callid:" + callid + "\r\nrequest=" + cmd + " from " + remoteip + ":" + remoteport);
        }
        switch (reply) {
          case -1:
            clone(cdsd, cdpbx);
            if (cmd.equalsIgnoreCase("REGISTER")) {
              String auth = getHeader("Authorization:", msg);
              if (auth == null) {
                //send a 401
                cd.nonce = getnonce();
                String challenge = "WWW-Authenticate: Digest algorithm=MD5, realm=\"" + realm + "\", nonce=\"" + cd.nonce + "\"";
                if (use_qop) {
                  challenge += ", qop=\"auth\"";
                }
                challenge += "\r\n";
                reply(cd, 401, "REQ AUTH", challenge, false, src);
                break;
              }
              if (!auth.regionMatches(true, 0, "digest ", 0, 7)) {
                JFLog.log("invalid Authorization");
                break;
              }
              String[] tags = auth.substring(7).replaceAll(" ", "").replaceAll("\"", "").split(",");
              String res = getHeader("response=", tags);
              String nonce = getHeader("nonce=", tags);
              if ((nonce == null) || (cd.nonce == null) || (!cd.nonce.equals(nonce))) {
                //send another 401
                cd.nonce = getnonce();
                String challenge = "WWW-Authenticate: Digest algorithm=MD5, realm=\"" + realm + "\", nonce=\"" + cd.nonce + "\"";
                if (use_qop) {
                  challenge += ", qop=\"auth\"";
                }
                challenge += "\r\n";
                reply(cd, 401, "REQ AUTH", challenge, false, src);
                break;
              }
              String test = getResponse(cd.user, iface.getPassword(cd.user), realm, cd.cmd, getHeader("uri=", tags), cd.nonce, getHeader("qop=", tags),
                getHeader("nc=", tags), getHeader("cnonce=", tags));
              cd.nonce = null;  //don't allow value to be reused
              if (!res.equalsIgnoreCase(test)) {
                reply(cd, 403, "BAD PASSWORD", null, false, src);
                setCallDetailsServer(callid, null);
                break;
              }
              //REGISTER OK
              iface.onRegister(cd.user, getexpires(msg), remoteip, remoteport);
              reply(cd, 200, "OK", null, false, src);
              setCallDetailsServer(callid, null);
              break;
            }
            if (cmd.equalsIgnoreCase("INVITE")) {
              //BUG : What if call is from same extension but from another PBX
              //      this will think the INVITE must auth first
              //      need to check if dest is on this PBX and bypass auth check
              String pass = iface.getPassword(cd.user);
              if (pass != null) {
                //do auth only if has a password
                String resln = getHeader("Proxy-Authorization:", msg);
                if ((resln == null) || (cd.nonce == null)) {
                  //send a 407
                  cd.nonce = getnonce();
                  String challenge = "Proxy-Authenticate: Digest algorithm=MD5, realm=\"" + realm + "\", nonce=\"" + cd.nonce + "\"\r\n";
                  reply(cd, 407, "REQ AUTH", challenge, false, src);
                  break;
                }
                if (!resln.regionMatches(true, 0, "digest ", 0, 7)) {
                  break;
                }
                String[] tags = resln.substring(7).replaceAll(" ", "").replaceAll("\"", "").split(",");
                String res = getHeader("response=", tags);
                String nonce = getHeader("nonce=", tags);
                if ((nonce == null) || (!cd.nonce.equals(nonce))) {
                  //send another 407
                  cd.nonce = getnonce();
                  String challenge = "Proxy-Authenticate: Digest algorithm=MD5, realm=\"" + realm + "\", nonce=\"" + cd.nonce + "\"\r\n";
                  reply(cd, 407, "REQ AUTH", challenge, false, src);
                  break;
                }
                String test = getResponse(cd.user, pass, realm, cd.cmd, getHeader("uri=", tags), cd.nonce, null, null, null);
                cd.nonce = null;  //don't allow value to be reused
                if (!res.equalsIgnoreCase(test)) {
                  reply(cd, 403, "BAD PASSWORD", null, false, src);
                  setCallDetailsServer(callid, null);
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
              cdsd.sdp = getSDP(msg);
              JFLog.log("src=" + cdsd.sdp);
              //get o1/o2
              cdsd.sdp.o1 = geto(msg, 1);
              cdsd.sdp.o2 = geto(msg, 2);
              cd.authorized = (pass != null);
              iface.onInvite(cd, src);
              break;
            }
            if (cmd.equalsIgnoreCase("CANCEL")) {
              iface.onCancel(cd, src);
//              setCallDetailsServer(callid, null);  //still too soon
              break;
            }
            if (cmd.equalsIgnoreCase("BYE")) {
              //BUG : can't delete calldetails yet (memory leak)
              iface.onBye(cd, src);
              break;
            }
            if (cmd.equalsIgnoreCase("ACK")) {
              //TODO : ???
              break;
            }
            if (cmd.equalsIgnoreCase("REFER")) {
              iface.onFeature(cd, cmd, getHeader("Refer-To:", msg), src);
              break;
            }
            if (cmd.equalsIgnoreCase("OPTIONS")) {
              iface.onOptions(cd, src);
              break;
            }
            if (cmd.equalsIgnoreCase("SUBSCRIBE")) {
              //send 200 and ignore
              reply(cd, 200, "OK", null, false, src);
              setCallDetailsServer(callid, null);
              break;
            }
            if (cmd.equalsIgnoreCase("SHUTDOWN")) {
              iface.onFeature(cd, cmd, remoteip, src);
              setCallDetailsServer(callid, null);
              break;
            }
            if (cmd.equalsIgnoreCase("MESSAGE")) {
              iface.onMessage(cd, cdsd.from[1], cdsd.to[1], HTTP.getContent(msg), src);
              break;
            }
            JFLog.log("Unknown command:" + cmd);
            setCallDetailsServer(callid, null);
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
              cdsd.sdp = getSDP(msg);
              cdsd.sdp.o1 = geto(msg, 1);
              cdsd.sdp.o2 = geto(msg, 2);
            }
            else if (cd.cmd.equals("BYE")) {
              setCallDetailsServer(cd.callid, null);
              break;
            }
            else if (cd.cmd.equals("REGISTER")) {
              //send ACK and ignore
              cd.cmd = "ACK";
              issue(cd, null, false, src);
              setCallDetailsServer(callid, null);
              break;
            }
            iface.onSuccess(cd, src);
            break;
          case 401:
            if (cd.authsent) {
              JFLog.log("Server Error : Double 401");
              setCallDetailsServer(callid, null);
              break;
            }
            boolean sdp = false;
            if (cd.cmd.equals("INVITE")) {
              cd.cmd = "ACK";
              issue(cd, null, false, src);
              cd.cmd = "INVITE";
              sdp = true;
            }
            String key = remoteip + ":" + remoteport;
            Trunk trunk = trunks.get(key);
            if (trunk == null) {
              JFLog.log("err:trunk not found:" + key);
              setCallDetailsServer(callid, null);
              break;
            }
            cd.authstr = getHeader("WWW-Authenticate:", msg);
            epass = getAuthResponse(cd, trunk.user, trunk.pass, cdpbx.host, cd.cmd, "Authorization:");
            if (epass == null) {
              JFLog.log("err:gen auth failed");
              setCallDetailsServer(callid, null);
              break;
            }
            cdsd.cseq++;
            cd.authsent = true;
            issue(cd, epass, sdp, src);
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
                setCallDetailsServer(callid, null);
                break;
              }
              String reg = iface.getTrunkRegister(remoteip);  //user : pass @ host / did
              if (reg == null) {
                JFLog.log("TRUNK : 407 : no register string for trunk");
                setCallDetailsServer(callid, null);
                break;
              }
              int idx1 = reg.indexOf(":");
              int idx2 = reg.indexOf("@");
              if ((idx1 == -1) || (idx2 == -1)) {
                JFLog.log("TRUNK : 407 : invalid register string for trunk");
                setCallDetailsServer(callid, null);
                break;
              }
              String trunk_user = reg.substring(0, idx1);
              String trunk_pass = reg.substring(idx1 + 1, idx2);
              cd.authstr = getHeader("Proxy-Authenticate:", msg);
              epass = getAuthResponse(cd, trunk_user, trunk_pass, cdpbx.host, cd.cmd, "Proxy-Authorization:");
              if (epass == null) {
                JFLog.log("err:gen auth failed");
                setCallDetailsServer(callid, null);
                break;
              }
              cdsd.cseq++;
              cd.authsent = true;
              issue(cd, epass, true, src);
            }
            break;
          default:
            iface.onError(cd, reply, src);
            if (reply == 487) {
              setCallDetailsServer(cd.callid, null);  //call canceled
            }
            break;
        }
      }  //synchronized
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public Set getCalls() {
    return cdlist.keySet();
  }
}
