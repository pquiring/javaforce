package javaforce;

/** SOCKS4 client function
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

public class SOCKS {
  /** Connects to host via a SOCKS4 server.
   * Socket must already be connected to SOCKS4 server.
   * host must be x.x.x.x
   *
   * @return boolean state if SOCKS4 connection to host:port was successful.
   */
  public static boolean connect(Socket s, String host, int port) {
    byte[] req = new byte[9];
    byte[] reply = new byte[8];
    req[0] = 0x04;
    req[1] = 0x01;
    BE.setuint16(req, 2, port);
    String[] ips = host.split("[.]");
    if (ips == null || ips.length != 4) {
      JFLog.log("SOCKS.connect() : host is not an IP4 address");
      return false;
    }
    for(int a=0;a<4;a++) {
      req[4+a] = (byte)JF.atoi(ips[a]);
    }
    try {
      OutputStream os = s.getOutputStream();
      InputStream is = s.getInputStream();
      os.write(req);
      int totalread = 0;
      while (totalread != reply.length) {
        int read = is.read(reply, totalread, reply.length - totalread);
        if (read < 0) throw new Exception("bad read");
        if (read > 0) totalread += read;
      }
      return reply[1] == 0x5a;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}
