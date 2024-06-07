package javaforce;

/** Telnet client.
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

public class Telnet {
  private Socket socket;
  private InputStream in;
  private OutputStream out;

  //Telnet Commands (Always preceded by IAC)
  public final static char SE  = 240;  //0xf0 : end of sub parameters
  public final static char NOP = 241;  //0xf1 : nothin
  public final static char DM  = 242;  //0xf2 : data mark (???)
  public final static char BRK = 243;  //0xf3 : break
  public final static char IP  = 244;  //0xf4 : interupt process
  public final static char AO  = 245;  //0xf5 : abort output
  public final static char AYT = 246;  //0xf6 : are you there?
  public final static char EC  = 247;  //0xf7 : erase char (del)
  public final static char EL  = 248;  //0xf8 : erase line
  public final static char GA  = 249;  //0xf9 : go ahead
  public final static char SB  = 250;  //0xfa : start of sub parameters
  public final static char WILL = 251; //0xfb : will (option) Confirm
  public final static char WONT = 252; //0xfc : won't (option) Confirm
  public final static char DO   = 253; //0xfd : do (option) Request
  public final static char DONT = 254; //0xfe : don't (option) Request
  public final static char IAC  = 255; //0xff : start of all above commands (interpret as command)

  //Telnet Options see https://www.iana.org/assignments/telnet-options/telnet-options.xhtml
  public final static char TO_BINARY    =  0;   //binary transmission
  public final static char TO_ECHO      =  1;   //echo
  public final static char TO_SGO       =  3;   //Supress Go Ahead
  public final static char TO_TM        =  6;   //Timing Mark
  public final static char TO_TT        = 24;   //Terminal Type
  public final static char TO_EOR       = 25;   //end of record
  public final static char TO_NWS       = 31;   //neg window size
  public final static char TO_RFC       = 33;   //remote flow control

  public final static char TT_REPLY    = 0;     //term type reply
  public final static char TT_REQUEST  = 1;     //term type request

  public final static char[] pre_tt = {IAC, SB, TO_TT, TT_REPLY};
  //terminal type (ie:ANSI)
  public final static char[] post_tt = {IAC, SE};

  /** Connect to telnet socket. */
  public boolean connect(String host, int port) {
    try {
      socket = new Socket(host, port);
      in = socket.getInputStream();
      out = socket.getOutputStream();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Connect to SSL socket. */
  public boolean connectSSL(String host, int port, KeyMgmt keys) {
    try {
      socket = JF.connectSSL(host, port, keys);
      in = socket.getInputStream();
      out = socket.getOutputStream();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Upgrade existing raw socket to SSL socket. */
  public boolean connectSSL(Socket rawSocket, KeyMgmt keys) {
    try {
      socket = JF.connectSSL(rawSocket, keys);
      in = socket.getInputStream();
      out = socket.getOutputStream();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void disconnect() {
    if (socket != null) try {socket.close();} catch (Exception e) {}
  }

  public InputStream getInputStream() {
    return in;
  }

  public OutputStream getOutputStream() {
    return out;
  }
}
