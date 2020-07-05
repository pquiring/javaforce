package javaforce.io;

/** ObjectReader
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectReader {
  private DataInputStream dis;
  private boolean header;
  private int type = -1;
  public ObjectReader(InputStream is) {
    dis = new DataInputStream(new BufferedInputStream(is));
  }
  /** Reads and verifies the header (optional)
   * This can be used to determine object type before calling readObject()
   */
  public void readHeader() throws Exception {
    if (header) throw new Exception("header already read");
    short magic = dis.readShort();
    if (magic != SerialObject.javaforce_magic) {
      throw new Exception("invalid magic");
    }
    short res1 = dis.readShort();
    type = dis.readInt();
    long res2 = dis.readLong();
    header = true;
  }
  public int getType() {
    return type;
  }
  public Object readObject(SerialObject reader) throws Exception {
    if (!header) readHeader();
    reader.readInit(dis);
    reader.readObject();
    return reader;
  }
}
