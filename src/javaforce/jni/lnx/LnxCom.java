package javaforce.jni.lnx;

/**
 * Linux Com Port I/O (RS-232)
 *
 * @author pquiring
 *
 * Created : Jan 17, 2014
 */

import javaforce.jni.*;
import javaforce.io.*;

public class LnxCom implements ComPort {
  public static boolean init() {
    return true;
  }

  private int fd;
  private String name;

  //assumes 8 data bits, 1 stop bit, no parity, etc.
  public static LnxCom open(String name, int baud) {
    LnxCom com = new LnxCom();
    com.fd = LnxNative.comOpen(name, baud);
    com.name = name;
    return com;
  }

  public String getPort() {
    return name;
  }

  public int read(byte[] data) {
    return LnxNative.comRead(fd, data);
  }

  public int write(byte[] data) {
    return LnxNative.comRead(fd, data);
  }

  public void close() {
    LnxNative.comClose(fd);
  }
}
