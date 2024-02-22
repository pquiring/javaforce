package javaforce.voip;

/** RTSP session
 *
 * @author pquiring
 */

import java.net.*;

public class RTSPSession {
  public RTSPSession(String localhost, int localport) {
    this.localhost = localhost;
    this.localport = localport;
    extra = "";
  }
  public String localhost;
  public int localport;
  public int cseq = 2;
  public String id;
  public String uri;
  public String extra;
  public String base;
  public String epass;
  public String transport;
  public String[] sdp;
  public String cmd;
  public boolean authsent;
  public String authstr;
  public String authtype;
  public String nonce;
  public int nonceCount;
  public String accept;
  public String[] params;

  public String remotehost;
  public int remoteport;
  public int remotecseq;

  //server only
  public InetAddress remoteaddr;
  public int reply;
  public String user;
  public boolean auth;
  public RTP rtp;
  public RTPChannel channel;
  public long ts;

  //user defined for tracking resource
  public int res_type;  //resource type
  public String res_name;  //resource name
  public Object res_user;  //resource object

  public String toString() {
    return "RTSPSession:{local=" + localport + ",remote=" + remotehost + ":" + remoteport + ":" + rtp + ":" + cmd + "}";
  }
}
