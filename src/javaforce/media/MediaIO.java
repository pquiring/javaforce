package javaforce.media;

/** Interface for reading/writing files for media coders.
 *
 * @author pquiring
 */

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;

public interface MediaIO {
  /** Request to read data.
   * @param data = buffer to receive data
   */
  public int read(MediaCoder coder, byte[] data);
  /** Default FFM implementation of read(). Do not implement. */
  default public int read(MemorySegment data, int size) {
    byte[] byteArray = data.asSlice(0, size).toArray(JAVA_BYTE);
    return read(null, byteArray);
  }
  /** Request to write data.
   * @param data = buffer of data to be written.
   */
  public int write(MediaCoder coder, byte[] data);
  /** Default FFM implementation of write(). Do not implement. */
  default public int write(MemorySegment data, int size) {
    byte[] byteArray = data.asSlice(0, size).toArray(JAVA_BYTE);
    return write(null, byteArray);
  }
  /** Request to seek file.
   * @param pos = position
   * @param how = how to seek (see MediaCoder.SEEK_... types)
   */
  public long seek(MediaCoder coder, long pos, int how);
  /** Default FFM implementation of seek(). Do not implement. */
  default public long seek(long pos, int how) {
    return seek(null, pos, how);
  }
}
