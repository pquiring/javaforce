package javaforce.io;

/** SerialObject - replacement for java.io.Serializable
 *
 * GraalVM does not support Serialization so a replacement is required.
 *
 * @author pquiring
 */

import java.io.*;

public interface SerialObject {
  public static int magic = 0x4a46534f;  //JFSO (JavaForce Serialized Object
  public void readObject(DataInputStream dis);
  public void writeObject(DataOutputStream dis);
}
