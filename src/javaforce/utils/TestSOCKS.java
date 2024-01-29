package javaforce.utils;

/** Test SOCKS server
 *
 * @author pquiring
 */

import java.net.*;
import java.io.*;
import javaforce.JF;
import javaforce.SOCKS;

public class TestSOCKS {
  public static void main(String[] args) {
    if (args.length != 3) {
      System.out.println("Usage:TestSOCKS socks_server real_server real_port");
      return;
    }
    try {
      System.out.println("Connecting to SOCKS4");
      Socket socket = new Socket(args[0], 1080);
      System.out.println("Requesting connect from SOCKS4");
      if (!SOCKS.connect(socket, args[1], Integer.valueOf(args[2]))) {
        System.err.println("SOCKS.connect() failed");
      }
      System.out.println("Connect ok");
      OutputStream os = socket.getOutputStream();
      InputStream is = socket.getInputStream();
      os.write("GET / HTTP/1.0\r\n\r\n".getBytes());
      while (socket.isConnected()) {
        System.out.print(new String(is.readAllBytes()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
