/**
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

public class ServerClient extends Thread {
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private char mode;
  private Reader reader;
  private Writer writer;
  public ServerClient(Socket s) {
    this.s = s;
  }
  public void run() {
    //read client request
    try {
      is = s.getInputStream();
      os = s.getOutputStream();
      mode = (char)is.read();
      switch (mode) {
        case 'F':  //full duplex
          reader = new Reader();
          reader.start();
          writer = new Writer();
          writer.start();
          break;
        case 'S':  //client send only
          reader = new Reader();
          reader.start();
          break;
        case 'R':  //client recv only
          writer = new Writer();
          writer.start();
          break;
        default:
          s.close();
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  private class Reader extends Thread {
    public void run() {
      byte data[] = new byte[1460];
      try {
        while (true) {
          int r = is.read(data);
          if (r == -1) break;
        }
      } catch (Exception e) {
      }
    }
  }
  private class Writer extends Thread {
    public void run() {
      byte data[] = new byte[1460];
      try {
        while (true) {
          os.write(data);
        }
      } catch (Exception e) {
      }
    }
  }
}
