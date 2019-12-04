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
  public String cmd;
  public String arg;
  public String localfile;
  public FileOutputStream fos;
  public long uncompressed;
  public long compressed;
  public String reply;
  public RequestNotify notify;
}
