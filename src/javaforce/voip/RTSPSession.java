package javaforce.voip;

/** RTSP session
 *
 * @author peterq.admin
 */

import java.util.*;

public class RTSPSession {
  public RTSPSession(String localhost) {
    this.localhost = localhost;
    Random r = new Random();
    id = r.nextInt();
  }
  public String localhost;
  public int cseq = 2;
  public int id;
  public String uri;
  public String epass;
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
