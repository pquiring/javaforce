package javaforce.jni;

/** Com Port JNI
 *
 * @author pquiring
 */

import javaforce.api.*;

public class ComPortJNI implements ComPortAPI {
  public native long comOpen(String name, int baud);

  public native int comRead(long handle, byte[] data);

  public native int comWrite(long handle, byte[] data);

  public native void comClose(long handle);
}
