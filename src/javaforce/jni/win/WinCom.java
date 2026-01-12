package javaforce.jni.win;

/**
 * Windows Com Port I/O (RS-232)
 *
 * @author pquiring
 *
 * Created : Jan 16, 2014
 */

import javaforce.jni.*;
import javaforce.io.*;

public class WinCom implements ComPort {
  private WinCom() {}
  public static boolean init() {
    return true;
  }
  private long handle;
  private String name;
  //assumes 8 data bits, 1 stop bit, no parity, etc.
  public static WinCom open(String name, int baud) {
    WinCom com = new WinCom();
    com.handle = WinNative.comOpen(name, baud);
    if (com.handle == 0) return null;
    com.name = name;
    return com;
  }
  public String getPort() {
    return name;
  }
  public int read(byte[] data) {
    return WinNative.comRead(handle, data);
  }
  public int write(byte[] data) {
    return WinNative.comWrite(handle, data);
  }
  public void close() {
    WinNative.comClose(handle);
  }
}
