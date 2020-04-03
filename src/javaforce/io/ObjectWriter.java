package javaforce.io;

/** ObjectWriter
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectWriter {
  public void writeObject(OutputStream os, SerialObject writer) throws Exception {
    BufferedOutputStream bos = new BufferedOutputStream(os);
    DataOutputStream dos = new DataOutputStream(bos);
    dos.writeInt(SerialObject.magic);
    writer.writeObject(dos);
  }
}
