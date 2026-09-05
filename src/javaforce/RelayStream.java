package javaforce;

import java.io.*;
import java.util.*;

/** Relay Stream.
 *
 * Continuously reads data from input stream and writes to output stream or JFLog.
 *
 * @author pquiring
 */

public class RelayStream extends Thread {
  private InputStream in;
  private OutputStream out;
  private int logid;
  private Condition connected;

  public static boolean debug = false;

  public RelayStream(InputStream in, OutputStream out, Condition connected) {
    this.in = in;
    this.out = out;
    this.connected = connected;
  }
  public RelayStream(InputStream in, int logid, Condition connected) {
    this.in = in;
    this.logid = logid;
    this.connected = connected;
  }
  public void run() {
    if (out != null) {
      run_stream();
    } else {
      run_log();
    }
    if (debug) {
      JFLog.log("RelayStream done");
    }
  }
  private void run_stream() {
    byte[] data = new byte[1024];
    try {
      while (connected.check()) {
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
  }
  private void run_log() {
    byte[] data = new byte[1024];
    byte[] buf = new byte[1024];
    int bufsize = 0;
    try {
      while (connected.check()) {
        int read = in.read(data);
        if (read == -1) break;
        if (read > 0) {
          while (bufsize + read > buf.length) {
            buf = Arrays.copyOf(buf, buf.length << 1);
          }
          System.arraycopy(data, 0, buf, bufsize, read);
          bufsize += read;
          int idx;
          do {
            idx = Arrays.binarySearch(buf, 0, bufsize, (byte)'\n');
            if (idx != -1) {
              String str = new String(buf, 0, idx);
              JFLog.log(logid, str);
              idx++;  //skip \n
              if (idx == bufsize) {
                bufsize = 0;
              } else {
                System.arraycopy(buf, idx, buf, 0, buf.length - idx);
                bufsize = bufsize - idx;
              }
            }
          } while (idx != -1);
        } else {
          JF.sleep(10);
        }
      }
    } catch (Exception e) {
      //JFLog.log(e);
    }
  }
}
