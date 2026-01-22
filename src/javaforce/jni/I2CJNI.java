package javaforce.jni;

/** I2C JNI
 *
 * @author pquiring
 */

import javaforce.pi.*;

public class I2CJNI implements I2C {
  private static I2CJNI instance;

  public static I2C getInstance() {
    if (instance == null) {
      instance = new I2CJNI();
      if (!instance.init()) {
        instance = null;
      }
    }
    return instance;
  }

  private native boolean init();
  public native boolean setSlave(int addr);
  public native boolean write(byte[] data);
  public native int read(byte[] data);
}
