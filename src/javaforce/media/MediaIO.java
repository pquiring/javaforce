package javaforce.media;

/** Interface for reading/writing files for media coders.
 *
 * @author pquiring
 */

import java.lang.foreign.*;

public interface MediaIO {
  public int read(MediaCoder coder, byte[] data);
  default public int readFFM(MemorySegment data, int size) {
    byte[] byteArray = data.asSlice(0, size).toArray(ValueLayout.JAVA_BYTE);
    return read(null, byteArray);
  }
  public int write(MediaCoder coder, byte[] data);
  default public int writeFFM(MemorySegment data, int size) {
    byte[] byteArray = data.asSlice(0, size).toArray(ValueLayout.JAVA_BYTE);
    return write(null, byteArray);
  }
  public long seek(MediaCoder coder, long pos, int how);
  default public long seekFFM(long pos, int how) {
    return seek(null, pos, how);
  }
}
