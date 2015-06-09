package javaforce.voip;

/**
 * Keeps track of Call Details based on the 'callid' field of SIP messages.
 * Extends CallDetails for Server side details.
 */

import java.util.*;

public class CallDetailsServer extends CallDetails {
  /*
   SideDetails Layout:

   caller -------------> pbx -------------> callee
   src            pbxsrc     pbxdst         dst

   */

  /**
   * Tracks details on PBX side to call originator.
   */
  public SideDetails pbxsrc = new SideDetails();
  /**
   * Tracks details on PBX side to call terminator.
   */
  public SideDetails pbxdst = new SideDetails();
  public Object lock = new Object();
  public SIPServer sip;
  public int pid;
  public String cmd;
  public String nonce;
  public String user;  //From: field (or trunk register)
  public String pass;  //for trunk register
  public String dialed;
  public String fromname;
  public String fromnumber;
  public boolean authorized;
  public boolean xfer_src, xfer_dst;
  public long lastPacket;
}
