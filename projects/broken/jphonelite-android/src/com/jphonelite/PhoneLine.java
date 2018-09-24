package com.jphonelite;

import javaforce.voip.*;

/** Keeps track of each line. */

public class PhoneLine {
  public boolean unauth;
  public boolean incall;   //INVITE (outbound)
  public boolean trying;   //100 trying
  public boolean ringing;  //180 ringing
  public boolean talking;  //200 ok

  public boolean incoming; //INVITE (inbound)

  public boolean disableVideo;
  public boolean srtp;
  public SIP.Transport transport;
  public boolean dtls;
  public boolean rtpStarted;

  public String dial = "", status = "";
  public String callid;  //Call-ID in SIP header (not callerid)
  public String orgdial;  //connected dial string
  public String to;  //remote number
  public String callerid;  //TEXT name of person calling

  public SIPClient sip;

  public RTP audioRTP;
//  public RTP videoRTP;
  public SDP sdp, localsdp;

  public String remotertphost;
  public int remotertpport;

  public int clr = -1;

  public boolean xfr,hld,dnd,cnf;

  public short samples[];
  public short samples8[] = new short[160];
  public short samples16[] = new short[320];

  //RFC 2833 - DTMF
  public char dtmf = 'x';
  public boolean dtmfend = false;
  public int dtmfcnt = 0;

  public boolean msgwaiting = false;

  public PhoneLine() {
    dial = "";
    status = "";
    callid = "";
  }
}
