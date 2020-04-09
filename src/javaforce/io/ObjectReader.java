package javaforce.io;

/** ObjectReader
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectReader {
  private InputStream is;
  public ObjectReader(InputStream is) {
    this.is = is;
  }
  public Object readObject(SerialObject reader) throws Exception {
    BufferedInputStream bis = new BufferedInputStream(is);
    DataInputStream dis = new DataInputStream(bis);
    short magic = dis.readShort();
    if (magic != SerialObject.javaforce_magic) {
      throw new Exception("invalid magic");
    }
    reader.readInit(dis);
    reader.readObject();
    return reader;
  }
}
