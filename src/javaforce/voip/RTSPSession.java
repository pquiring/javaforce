package javaforce.voip;

/** RTSP session
 *
 * @author pquiring
 */

import java.net.*;

public class RTSPSession {
  public RTSPSession(String localhost) {
    this.localhost = localhost;
    extra = "";
  }
  public String localhost;
  public int cseq = 2;
  public long id = -1;
  public String uri;
  public String extra;
  public String epass;
  public String transport;
  public String[] headers;
  public String sdp;
  public String cmd;
  public boolean authsent;
  public String authstr;
  public String nonce;
  public int nonceCount;
  public String accept;
  public String params;

  public String remotehost;
  public int remoteport;
  public int remotecseq;

  //server only
  public InetAddress remoteaddr;
  public int reply;
  public String user;
  public boolean auth;
}
