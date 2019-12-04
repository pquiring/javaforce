/** EntryFile
 *
 * @author pquiring
 */

import java.io.*;

public class EntryFile implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public long o;  //logical 64k block offset in tape
  public int t;  //tape # (multi-tape backup)
  public long s;  //uncompressed size
  public long c;  //compressed size
  public long u;  //uncompressed size
  public long b;  //# of blocks used on tape
  public byte ct;  //compression type (0=none 1=zip)
  public transient String localfile;
}
