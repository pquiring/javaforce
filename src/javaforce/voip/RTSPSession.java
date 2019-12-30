package javaforce.voip;

/** RTSP session
 *
 * @author pquiring
 */

import java.util.*;

public class RTSPSession {
  public RTSPSession(String localhost) {
    this.localhost = localhost;
    extra = "";
  }
  public String localhost;
  public int cseq = 2;
  public long id;
  public String uri;
  public String extra;
  public String epass;
  public String transport;
  public String headers[];
  public String sdp;
  public String cmd;
  public boolean authsent;
  public String authstr;
  public String nonce;
  public int nonceCount;

  public String remotehost;
  public int remoteport;
  public int remotecseq;
}
