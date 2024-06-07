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

public class Telnet extends javaforce.Telnet {

  public boolean decode(char[] code, int codelen, Buffer buffer) {
    char[] res = new char[3];
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
        //IAC SB TO_TT TT_REQUEST ... IAC SE
        if (code[2] == TO_TT && code[3] == TT_REQUEST) {
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
    char[] response = new char[3];
    response[0] = IAC;
    response[1] = ww;
    response[2] = code;
    buffer.output(response);
  }
}
