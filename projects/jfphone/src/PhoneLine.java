import java.util.*;

import javaforce.voip.*;

/** Keeps track of each line. */

public class PhoneLine {
  public boolean unauth, auth;
  public boolean noregister;
  public boolean incall;   //INVITE (outbound)
  public boolean trying;   //100 trying
  public boolean ringing;  //180 ringing
  public boolean ringback; //183 ringback tone
  public boolean talking;  //200 ok

  public boolean disableVideo;
  public boolean srtp;
  public TransportType transport;
  public boolean dtls;

  public String user;

  public boolean incoming; //INVITE (inbound)
  public boolean rtpStarted;  //RTP started

  public String dial = "", status = "";
  public String callid = "";  //Call-ID in SIP header (not callerid)
  public String to;  //remote number
  public String callerid;  //TEXT name of person calling

  public SIPClient sip;

  public RTP audioRTP, videoRTP;
  public SDP sdp, localsdp;

  public int clr = -1;

  public boolean xfer,hld,dnd,cnf;

  //used in conference mode only
  public short samples[];
  public short samples8[] = new short[160];
  public short samples16[] = new short[320];
  public short samples32[] = new short[640];

  //RFC 2833 - DTMF
  public char dtmf = 'x';
  public boolean dtmfend = false;

  public boolean msgwaiting = false;

  public Vector<RemoteCamera> remoteCameras = new Vector<RemoteCamera>();
  public final Object remoteCamerasLock = new Object();
  public VideoWindow videoWindow;
}
