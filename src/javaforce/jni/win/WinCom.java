package javaforce.jni.win;

/**
 * Windows Com Port I/O (RS-232)
 *
 * @author pquiring
 *
 * Created : Jan 16, 2014
 */

import java.util.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.io.*;

public class WinCom implements ComPort {
  private WinCom() {}
  public static boolean init() {
    return true;
  }
  private long handle;
  private String name;
  public static String[] list() {
    ArrayList<String> coms = new ArrayList<>();
    for(int a=1;a<10;a++) {
      String name = "com" + a;
      WinCom com = open(name, 9600);
      if (com != null) {
        com.close();
        coms.add(name);
      }
    }
    return coms.toArray(JF.StringArrayType);
  }
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
