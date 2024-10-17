package javaforce;

/** FTPS : Secure FTP.
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

public class FTPS extends FTP {
  public boolean connect(String host) throws Exception {
    return connect(host, 990);
  }

  public boolean connect(String host, int port) throws Exception {
    active = true;
    s = new Socket(host, port);
    s = JF.connectSSL(s, KeyMgmt.getDefaultClient());
    if (s == null) {
      throw new Exception("SSL Upgrade failed");
    }
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    reader = new Reader();
    reader.start();
    wait4Response();
    if (getLastResponse().startsWith("220")) {
      return true;
    }
    disconnect();  //not valid FTP site
    return false;
  }

  protected ServerSocket createServerSocket() throws Exception {
    return JF.createServerSocketSSL(KeyMgmt.getDefaultClient());
  }

  protected Socket connectData(String host, int port) throws Exception {
    Socket s = new Socket(host, port);
    return JF.connectSSL(s, KeyMgmt.getDefaultClient());
  }
}
