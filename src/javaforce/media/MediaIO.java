package javaforce.media;

/** Interface for reading/writing files for media coders.
 *
 * @author pquiring
 */

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;

public interface MediaIO {
  public int read(MediaCoder coder, byte[] data);
  default public int read(MemorySegment data, int size) {
    byte[] byteArray = data.asSlice(0, size).toArray(JAVA_BYTE);
    return read(null, byteArray);
  }
  public int write(MediaCoder coder, byte[] data);
  default public int write(MemorySegment data, int size) {
    byte[] byteArray = data.asSlice(0, size).toArray(JAVA_BYTE);
    return write(null, byteArray);
  }
  public long seek(MediaCoder coder, long pos, int how);
  default public long seek(long pos, int how) {
    return seek(null, pos, how);
  }
}
