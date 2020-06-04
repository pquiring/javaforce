package javaforce;

import java.io.*;
import java.net.*;

public class DatagramInputStream extends InputStream {

  private static final int MAX = 1460;
  private DatagramSocket ds;
  private byte[] buffer;
  private int buffersize = 0, bufferpos = 0;

  private boolean fillBuffer() {
    while (buffersize == 0) {
      byte[] data = new byte[MAX];
      DatagramPacket pack = new DatagramPacket(data, MAX);
      try {
        ds.receive(pack);
      } catch (Exception e) {
        return false;
      }
      buffer = pack.getData();
      buffersize = pack.getLength();
      bufferpos = 0;
    }
    return true;
  }

  public DatagramInputStream(DatagramSocket ds) {
    this.ds = ds;
  }

  public boolean markSupported() {
    return false;
  }

  public int read() {
    if (!fillBuffer()) {
      return -1;
    }
    int ret = ((int) buffer[bufferpos++]) & 0xff;
    buffersize--;
    return ret;
  }

  public int read(byte[] buf) {
    return read(buf, 0, buf.length);
  }

  public int read(byte[] buf, int pos, int len) {
    int ret;
    if (!fillBuffer()) {
      return -1;
    }
    if (len > buffersize) {
      ret = buffersize;
      System.arraycopy(buffer, bufferpos, buf, pos, buffersize);
      buffersize = 0;
      bufferpos = 0;
    } else {
      ret = len;
      System.arraycopy(buffer, bufferpos, buf, pos, len);
      buffersize -= len;
      bufferpos += len;
    }
    return ret;
  }
}
