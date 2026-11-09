package javaforce.controls;

import java.util.*;

/** PLC interface.
 *
 * Represents a PLC or remote I/O device.
 *
 * @author pquiring
 */

public interface PLC {
  /** Connects to device. */
  public boolean connect(String host);
  /** Disconnects device. */
  public boolean disconnect();
  /** Login with device if required. */
  public boolean login(String user, String pass);
  /** Returns true if device is connected. */
  public boolean isConnected();
  /** Returns true if device uses Big Endian byte order. */
  public boolean isBE();
  /** Returns true if device uses Little Endian byte order. */
  default public boolean isLE() {return !isBE();}
  /** Sends a keep alive packet. */
  public boolean keepAlive();
  /** Reads a tag from device. */
  public byte[] readTag(String tag);
  /** Reads multiple tags from device. */
  public byte[][] readTags(String[] tags);
  /** Writes data to a tag on device. */
  default public boolean writeTag(String tag, byte[] data) {
    return writeTag(tag, data, TagType.any);
  }
  /** Writes data to a tag on device.
   *
   * The tag type is only required for Allen Bradley PLCs.
   */
  public boolean writeTag(String tag, byte[] data, int tagType);
  /** Reads clock from device if supported. */
  public Calendar getClock();
  /** Writes the clock to device if supported. */
  public boolean setClock(Calendar clock);
  /** Returns last exception that occured. */
  public Exception getException();
}
