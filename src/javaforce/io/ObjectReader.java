package javaforce.io;

/** ObjectReader
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectReader {
  public Object readObject(InputStream is, SerialObject reader) throws Exception {
    DataInputStream dis = new DataInputStream(is);
    int magic = dis.readInt();
    if (magic != SerialObject.magic) {
      throw new Exception("invalid magic");
    }
    reader.readObject(dis);
    return reader;
  }
}
