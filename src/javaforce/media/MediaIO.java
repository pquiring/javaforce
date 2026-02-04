package javaforce.media;

/** Interface for reading/writing files for media coders.
 *
 * @author pquiring
 */

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;

public interface MediaIO {
  public static boolean debug = false;
  /** Request to read data.
   * @param data = buffer to receive data
   */
  public int read(MediaCoder coder, byte[] data);
  /** Default FFM implementation of read(). Do not implement. */
  default public int read(MemorySegment data, int size) {
    if (debug) System.out.println("read:" + size);
    byte[] byteArray = new byte[size];
    int read = read(null, byteArray);
    if (read > 0) {
      MemorySegment.copy(byteArray, 0, data.reinterpret(read), JAVA_BYTE, 0, read);
    }
    return read;
  }
  /** Request to write data.
   * @param data = buffer of data to be written.
   */
  public int write(MediaCoder coder, byte[] data);
  /** Default FFM implementation of write(). Do not implement. */
  default public int write(MemorySegment data, int size) {
    if (debug) System.out.println("write:" + size);
    byte[] byteArray = data.reinterpret(size).asSlice(0, size).toArray(JAVA_BYTE);
    return write(null, byteArray);
  }
  /** Request to seek file.
   * @param pos = position
   * @param how = how to seek (see MediaCoder.SEEK_... types)
   */
  public long seek(MediaCoder coder, long pos, int how);
  /** Default FFM implementation of seek(). Do not implement. */
  default public long seek(long pos, int how) {
    if (debug) System.out.println("seek:" + pos + "," + how);
    return seek(null, pos, how);
  }
  /** internal to FFM : implementing class must store upcall references until no longer needed.
   * See MediaInput and MediaOutput
   */
  default public MemorySegment[] store(MemorySegment[] array) {return array;}
}
