/**
 * Telnet.java
 *
 * Processes telnet codes.
 *
 * Created on August 3, 2007, 8:36 PM
 *
 * @author pquiring
 */

package javaforce.ansi.client;

public class Telnet {

  public Telnet() {}

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

//Telnet Options
  public final static char TO_SGO =  3;   //Supress Go Ahead
  public final static char TO_TM  =  6;   //Timing Mark
  public final static char TO_TT  = 24;   //Terminal Type (WILL/WONT)
  public final static char TO_EOR = 25;   //EOR?

  public final static char pre_tt[] = {255, 250, 24, 0};
    //Terminal Type
  public final static char post_tt[] = {255 , 240};

  public boolean decode(char code[], int codelen, Buffer buffer) {
    char res[] = new char[3];
    res[0] = IAC;
    if (codelen < 2) return false;
    if (code[1] == IAC) return true;  //double IAC - ignore it
    if (codelen < 3) return false;
    switch (code[1]) {
      case DO:
        //respond with WILL
        switch (code[2]) {
          case TO_EOR:
          case TO_TT:
          case TO_SGO:
          case TO_TM:
            send(WILL, code[2], buffer);
            return true;
          default:
            send(WONT, code[2], buffer);
            return true;
        }
      case DONT:
        send(WONT, code[2], buffer);
        return true;
      case SB:
        //decode sub-parameter(s)
        if (!((code[codelen-2] == IAC) && (code[codelen-1] == SE))) return false;
        //IAC SB TO_TT ... IAC SE
        if (code[2] == TO_TT) {
          buffer.output(pre_tt);
          buffer.output(buffer.settings.termType.toCharArray());
          buffer.output(post_tt);
          return true;
        }
        return true;  //ignore unknown sub code
      default:
        return true;  //ignore unknown code
    }
  }
  private static void send(char ww, char code, Buffer buffer) {
    char response[] = new char[3];
    response[0] = IAC;
    response[1] = ww;
    response[2] = code;
    buffer.output(response);
  }
}

