package javaforce;

/** SOCKS client connect functions
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

  /** Binds to a remote SOCKS4 server port.
   * @param host = client allowed to connect (0.0.0.0 = any host)
   * @param port = port to listen on socks server
   */
  public static boolean bind(Socket s, String host, int port) {
    byte[] req = new byte[9];
    byte[] reply = new byte[8];
    req[0] = 0x04;
    req[1] = 0x02;
    BE.setuint16(req, 2, port);
    String[] ips = host.split("[.]");
    if (ips == null || ips.length != 4) {
      JFLog.log("SOCKS.bind() : host is not an IP4 address");
      return false;
    }
    for(int a=0;a<4;a++) {
      req[4+a] = (byte)JF.atoi(ips[a]);
    }
    //first reply
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
      if (reply[1] != 0x5a) return false;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    //second reply after connection is made
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

  /** Connects to host via a SOCKS5 server.
   * Socket must already be connected to SOCKS5 server.
   * host may be x.x.x.x or domain name.
   *
   * @return boolean state if SOCKS5 connection to host:port was successful.
   */
  public static boolean connect(Socket s, String host, int port, String user, String pass) {
    if (!socks5_authenticate(s, user, pass)) return false;
    byte[] req = new byte[10];
    byte[] reply = new byte[10];
    req[0] = 0x05;  //version
    req[1] = 0x01;  //connect cmd
    req[2] = 0x00;  //reserved
    String[] ips = host.split("[.]");
    if (ips == null || ips.length != 4) {
      JFLog.log("SOCKS.connect() : host is not an IP4 address");
      return false;
    }
    //TODO : support type 3 (domain name)
    req[3] = 0x01;  //IP4 address
    for(int a=0;a<4;a++) {
      req[4+a] = (byte)JF.atoi(ips[a]);
    }
    BE.setuint16(req, 8, port);
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
      return reply[1] == 0x00;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Binds to a remote SOCKS5 server port. */
  public static boolean bind(Socket s, String host, int port, String user, String pass) {
    if (!socks5_authenticate(s, user, pass)) return false;
    byte[] req = new byte[10];
    byte[] reply = new byte[10];
    req[0] = 0x05;  //version
    req[1] = 0x02;  //bind cmd
    req[2] = 0x00;  //reserved
    String[] ips = host.split("[.]");
    if (ips == null || ips.length != 4) {
      JFLog.log("SOCKS.connect() : host is not an IP4 address");
      return false;
    }
    //TODO : support type 3 (domain name)
    req[3] = 0x01;  //IP4 address
    for(int a=0;a<4;a++) {
      req[4+a] = (byte)JF.atoi(ips[a]);
    }
    BE.setuint16(req, 8, port);
    try {
      OutputStream os = s.getOutputStream();
      InputStream is = s.getInputStream();
      os.write(req);
      //first reply
      int totalread = 0;
      while (totalread != reply.length) {
        int read = is.read(reply, totalread, reply.length - totalread);
        if (read < 0) throw new Exception("bad read");
        if (read > 0) totalread += read;
      }
      if (reply[1] != 0x00) return false;
      //second reply
      totalread = 0;
      while (totalread != reply.length) {
        int read = is.read(reply, totalread, reply.length - totalread);
        if (read < 0) throw new Exception("bad read");
        if (read > 0) totalread += read;
      }
      return reply[1] == 0x00;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private static boolean socks5_authenticate(Socket s, String user, String pass) {
    try {
      OutputStream os = s.getOutputStream();
      InputStream is = s.getInputStream();
      //send auth request
      {
        byte[] req = new byte[3];
        req[0] = 0x05;  //version
        req[1] = 0x01;  //# of auth
        req[2] = 0x02;  //auth type : user/pass
        os.write(req);
      }
      //read server reply
      {
        byte[] reply = new byte[2];
        int totalread = 0;
        while (totalread != reply.length) {
          int read = is.read(reply, totalread, reply.length - totalread);
          if (read < 0) throw new Exception("bad read");
          if (read > 0) totalread += read;
        }
        if (reply[1] == 0xff) return false;
      }
      //send user/pass
      {
        byte[] req = new byte[1 + 1 + user.length() + 1 + pass.length()];
        req[0] = 0x01;  //version
        int offset = 1;
        int userlen = user.length();
        req[offset++] = (byte)userlen;  //length of user
        BE.setString(req, offset, userlen, user);
        offset += userlen;
        int passlen = pass.length();
        req[offset++] = (byte)passlen;  //length of pass
        BE.setString(req, offset, passlen, pass);
        offset += passlen;
        os.write(req);
      }
      //read server reply
      {
        byte[] reply = new byte[2];
        int totalread = 0;
        while (totalread != reply.length) {
          int read = is.read(reply, totalread, reply.length - totalread);
          if (read < 0) throw new Exception("bad read");
          if (read > 0) totalread += read;
        }
        if (reply[1] == 0x00) return false;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
}
