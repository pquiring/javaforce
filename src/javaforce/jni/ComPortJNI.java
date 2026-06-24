package javaforce.jni;

import javaforce.api.*;

/** Com Port JNI
 *
 * @author pquiring
 */

public class ComPortJNI implements ComPortAPI {

  private static ComPortJNI instance;
  public static ComPortJNI getInstance() {
    if (instance == null) {
      JFNative.load();
      instance = new ComPortJNI();
    }
    return instance;
  }

  public native long comOpen(String name, int baud);

  public native int comRead(long handle, byte[] data, int size);

  public native int comWrite(long handle, byte[] data, int size);

  public native void comClose(long handle);
}
