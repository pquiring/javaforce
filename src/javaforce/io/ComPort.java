package javaforce.io;

/** Com Port interface
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.lnx.*;
import javaforce.jni.win.*;

public interface ComPort {
  public static ComPort open(String name, int baud) {
    if (JF.isWindows()) {
      return WinCom.open(name, baud);
    } else {
      return LnxCom.open(name, baud);
    }
  }
  public int read(byte[] data);
  public int write(byte[] data);
  public void close();
}
