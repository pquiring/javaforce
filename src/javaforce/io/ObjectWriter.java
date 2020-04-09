package javaforce.io;

/** ObjectWriter
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectWriter {
  private OutputStream os;
  public ObjectWriter(OutputStream os) {
    this.os = os;
  }
  public void writeObject(SerialObject writer) throws Exception {
    BufferedOutputStream bos = new BufferedOutputStream(os);
    DataOutputStream dos = new DataOutputStream(bos);
    dos.writeShort(SerialObject.javaforce_magic);
    writer.writeInit(dos);
    writer.writeObject();
    bos.flush();
  }
}
