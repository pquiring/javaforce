/** Request
 *
 * @author pquiring
 */

import java.io.*;

public class Request {
  public Request(String cmd) {
    this.cmd = cmd;
  }
  public Request(String cmd, String arg) {
    this.cmd = cmd;
    this.arg = arg;
  }
  public Request(String cmd, String arg, OutputStream os) {
    this.cmd = cmd;
    this.arg = arg;
    this.os = os;
  }
  public Request(String cmd, OutputStream os) {
    this.cmd = cmd;
    this.os = os;
  }
  public Request(String cmd, String arg, TapeDrive tape) {
    this.cmd = cmd;
    this.arg = arg;
    this.tape = tape;
  }
  public String cmd;
  public String arg;
  public OutputStream os;
  public long uncompressed;
  public long compressed;
  public String reply;
  public RequestNotify notify;
  public EntryFolder root;
  public TapeDrive tape;
}
