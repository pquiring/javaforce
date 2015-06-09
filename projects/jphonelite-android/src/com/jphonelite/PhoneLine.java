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

  public String dial = "", status = "";
  public String callid;  //Call-ID in SIP header (not callerid)
  public String orgdial;  //connected dial string
  public String to;  //remote number
  public String callerid;  //TEXT name of person calling

  public SIPClient sip;

  public RTP rtp;
  public String remotertphost;
  public int remotertpport;

  public Codec codecs[];  //codecs for incoming INVITE

  public int clr = -1;

  public boolean xfr,hld,dnd,cnf;

  public short samples[] = new short[160];  //used in conference mode only

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
