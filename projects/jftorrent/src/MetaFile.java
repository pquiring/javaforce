/** Meta File
 *
 * @author pquiring
 */

import java.io.*;

public class MetaFile {
  public String name;
  public long length;
  public RandomAccessFile file;
  public final Object lock = new Object();
  public void mkdirs(String dest) {
    String filename = dest + "/" + name;
    int idx = filename.lastIndexOf("/");
    if (idx == -1) return;
    new File(filename.substring(0, idx)).mkdirs();
  }
}
