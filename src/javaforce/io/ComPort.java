package javaforce.io;

/** Com Port interface
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.api.*;
import javaforce.jni.*;
import javaforce.ffm.*;

public class ComPort {
  private ComPortAPI api;
  private long handle;
  private String name;
  private ComPort() {
    api = new ComPortJNI();
  }
  /** Lists available Com ports. */
  public static String[] list() {
    if (JF.isWindows()) {
      ArrayList<String> coms = new ArrayList<>();
      for(int a=1;a<10;a++) {
        String name = "com" + a;
        ComPort com = open(name, 9600);
        if (com != null) {
          com.close();
          coms.add(name);
        }
      }
      return coms.toArray(JF.StringArrayType);
    } else {
      ArrayList<String> coms = new ArrayList<>();
      for(int a=0;a<10;a++) {
        String name = "/dev/ttyS" + a;
        ComPort com = open(name, 9600);
        if (com != null) {
          com.close();
          coms.add(name);
        }
        name = "/dev/ttyUSB" + a;
        ComPort usb = open(name, 9600);
        if (usb != null) {
          usb.close();
          coms.add(name);
        }
      }
      return coms.toArray(JF.StringArrayType);
    }
  }
  /** Opens a com port.
   *
   * @param name = com port name (com1, com2, etc)
   * @param baud = baud rate (9600, 19200, 38400, 57600, 115200)
   */
  public static ComPort open(String name, int baud) {
    ComPort port = new ComPort();
    port.handle = port.api.comOpen(name, baud);
    if (port.handle == 0) return null;
    port.name = name;
    return port;
  }
  /** Returns Com Port name. */
  public String getPort() {
    return name;
  }
  /** Read data from Com Port (blocking) */
  public int read(byte[] data) {
    return api.comRead(handle, data);
  }
  /** Writes data to Com Port */
  public int write(byte[] data) {
    return api.comWrite(handle, data);
  }
  /** Closes Com Port */
  public void close() {
    api.comClose(handle);
  }
}
