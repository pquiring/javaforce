package javaforce.jni.lnx;

/**
 * Linux Com Port I/O (RS-232)
 *
 * @author pquiring
 *
 * Created : Jan 17, 2014
 */

import java.util.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.io.*;

public class LnxCom implements ComPort {
  public static boolean init() {
    return true;
  }

  private int fd;
  private String name;

  public static String[] list() {
    ArrayList<String> coms = new ArrayList<>();
    for(int a=0;a<10;a++) {
      String name = "/dev/ttyS" + a;
      LnxCom com = open(name, 9600);
      if (com != null) {
        com.close();
        coms.add(name);
      }
      name = "/dev/ttyUSB" + a;
      LnxCom usb = open(name, 9600);
      if (usb != null) {
        usb.close();
        coms.add(name);
      }
    }
    return coms.toArray(JF.StringArrayType);
  }

  //assumes 8 data bits, 1 stop bit, no parity, etc.
  public static LnxCom open(String name, int baud) {
    LnxCom com = new LnxCom();
    com.fd = LnxNative.comOpen(name, baud);
    if (com.fd == 0) return null;
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
    return LnxNative.comWrite(fd, data);
  }

  public void close() {
    LnxNative.comClose(fd);
  }
}
