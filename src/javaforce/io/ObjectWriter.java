package javaforce.io;

/** ObjectWriter
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectWriter {
  private DataOutputStream dos;
  public ObjectWriter(OutputStream os) {
    dos = new DataOutputStream(new BufferedOutputStream(os));
  }

  /** Writes object with type=0 */
  public void writeObject(SerialObject writer) throws Exception {
    writeObject(writer, 0);
  }

  /** Writes object with specified type in header which can be read with
   * ObjectReader.readHeader() can obtain type before reading object.
   */
  public void writeObject(SerialObject writer, int type) throws Exception {
    //write header (16 bytes)
    dos.writeShort(SerialObject.javaforce_magic);
    dos.writeShort(0);  //reserved
    dos.writeInt(type);
    dos.writeLong(0);  //reserved
    //write object
    writer.writeInit(dos);
    writer.writeObject();
    dos.flush();
  }
}
