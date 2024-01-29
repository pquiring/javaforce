package javaforce.media;

/** Interface for reading/writing files for media coders.
 *
 * @author pquiring
 */

public interface MediaIO {
  public int read(MediaCoder coder, byte[] data);
  public int write(MediaCoder coder, byte[] data);
  public long seek(MediaCoder coder, long pos, int how);
}
