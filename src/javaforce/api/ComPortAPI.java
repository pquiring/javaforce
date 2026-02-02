package javaforce.api;

/** Com Port native API
 *
 * @author pquiring
 */

public interface ComPortAPI {
  /** Opens a com port.
   *
   * @param name = com port name (com1, com2, etc)
   * @param baud = baud rate (9600, 19200, 38400, 57600, 115200)
   */
  public long comOpen(String name, int baud);
  /** Read data from Com Port (blocking) */
  public int comRead(long handle, byte[] data, int size);
  /** Writes data to Com Port */
  public int comWrite(long handle, byte[] data, int size);
  /** Closes Com Port */
  public void comClose(long handle);
}
