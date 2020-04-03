package javaforce.io;

/** ObjectWriter
 *
 * @author pquiring
 */

import java.io.*;

public class ObjectWriter {
  public void writeObject(OutputStream os, SerialObject writer) throws Exception {
    DataOutputStream dos = new DataOutputStream(os);
    dos.writeInt(SerialObject.magic);
    writer.writeObject(dos);
  }
}
