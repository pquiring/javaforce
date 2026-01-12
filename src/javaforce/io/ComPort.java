package javaforce.io;

/** Com Port interface
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.lnx.*;
import javaforce.jni.win.*;

public interface ComPort {
  /** Opens a com port.
   *
   * @param name = com port name (com1, com2, etc)
   * @param baud = baud rate (9600, 19200, 38400, 57600, 115200)
   */
  public static ComPort open(String name, int baud) {
    if (JF.isWindows()) {
      return WinCom.open(name, baud);
    } else {
      return LnxCom.open(name, baud);
    }
  }
  /** Read data from Com Port (blocking) */
  public int read(byte[] data);
  /** Writes data to Com Port */
  public int write(byte[] data);
  /** Closes Com Port */
  public void close();
}
