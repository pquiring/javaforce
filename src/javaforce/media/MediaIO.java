package javaforce.media;

/** Interface for reading/writing files for media coders.
 *
 * @author pquiring
 */

public interface MediaIO {
  /** Request to read data.
   * @param data = buffer to receive data
   */
  public int read(MediaCoder coder, byte[] data);
  /** Request to write data.
   * @param data = buffer of data to be written.
   */
  public int write(MediaCoder coder, byte[] data);
  /** Request to seek file.
   * @param pos = position
   * @param how = how to seek (see MediaCoder.SEEK_... types)
   */
  public long seek(MediaCoder coder, long pos, int how);
}
