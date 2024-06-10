package javaforce;

/** Relay Stream
 *
 * Continuously reads data from input stream and writes to output stream.
 *
 * @author pquiring
 */

import java.io.*;

public class RelayStream extends Thread {
  private InputStream in;
  private OutputStream out;
  private Condition connected;

  public static boolean debug = false;

  public RelayStream(InputStream in, OutputStream out, Condition connected) {
    this.in = in;
    this.out = out;
    this.connected = connected;
  }
  public void run() {
    byte[] data = new byte[1024];
    try {
      while (connected.call()) {
        int read = in.read(data);
        if (read == -1) break;
        if (read > 0) {
          out.write(data, 0, read);
        } else {
          JF.sleep(10);
        }
      }
    } catch (Exception e) {
      //JFLog.log(e);
    }
    if (debug) {
      JFLog.log("RelayStream done");
    }
  }
}
