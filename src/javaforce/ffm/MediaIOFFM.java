package javaforce.ffm;

/** MediaIO for FFM
 *
 * @author pquiring
 */

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.media.*;

public interface MediaIOFFM extends MediaIO {
  /** Default FFM implementation of read(). Do not implement. */
  default public int read(MemorySegment data, int size) {
    byte[] byteArray = new byte[size];
    int read = read(null, byteArray);
    if (read > 0) {
      MemorySegment.copy(byteArray, 0, data.reinterpret(read), JAVA_BYTE, 0, read);
    }
    return read;
  }
  /** Default FFM implementation of write(). Do not implement. */
  default public int write(MemorySegment data, int size) {
    byte[] byteArray = data.reinterpret(size).asSlice(0, size).toArray(JAVA_BYTE);
    return write(null, byteArray);
  }
  /** Default FFM implementation of seek(). Do not implement. */
  default public long seek(long pos, int how) {
    return seek(null, pos, how);
  }
}
