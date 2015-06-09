package javaforce.jni.lnx;

/**
 * Linux Com Port I/O (RS-232)
 *
 * @author pquiring
 *
 * Created : Jan 17, 2014
 */

import javaforce.jni.LnxNative;

public class LnxCom {
  public static boolean init() {
    return true;
  }

  private int fd;

  //assumes 8 data bits, 1 stop bit, no parity, etc.
  public static LnxCom open(String name, int baud) {
    LnxCom com = new LnxCom();
    com.fd = LnxNative.comOpen(name, baud);
    return com;
  }

  public int read(byte data[]) {
    return LnxNative.comRead(fd, data);
  }

  public int write(byte data[]) {
    return LnxNative.comRead(fd, data);
  }

  public void close() {
    LnxNative.comClose(fd);
  }
}
