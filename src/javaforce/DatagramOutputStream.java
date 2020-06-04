package javaforce;

import java.io.*;
import java.net.*;

public class DatagramOutputStream extends OutputStream {

  private DatagramSocket ds;
  private InetAddress ip;
  private int port;

  public DatagramOutputStream(DatagramSocket ds, InetAddress ip, int port) {
    this.ds = ds;
    this.ip = ip;
    this.port = port;
  }

  public void write(int b) {
    byte[] data = new byte[1];
    data[0] = (byte) b;
    DatagramPacket pack = new DatagramPacket(data, 1, ip, port);
    try {
      ds.send(pack);
    } catch (Exception e) {
    }
  }

  public void write(byte[] buf) {
    write(buf, 0, buf.length);
  }

  public void write(byte[] buf, int pos, int len) {
    DatagramPacket pack = new DatagramPacket(buf, pos, len, ip, port);
    try {
      ds.send(pack);
    } catch (Exception e) {
    }
  }
}
