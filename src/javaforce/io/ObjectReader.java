package javaforce.io;

/** ObjectReader
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectReader {
  public Object readObject(InputStream is, SerialObject reader) throws Exception {
    BufferedInputStream bis = new BufferedInputStream(is);
    DataInputStream dis = new DataInputStream(bis);
    int magic = dis.readInt();
    if (magic != SerialObject.magic) {
      throw new Exception("invalid magic");
    }
    reader.readObject(dis);
    return reader;
  }
}
