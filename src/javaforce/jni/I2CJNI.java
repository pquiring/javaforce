package javaforce.jni;

/** I2C JNI
 *
 * @author pquiring
 */

import javaforce.api.*;

public class I2CJNI implements I2CAPI {
  private static I2CJNI instance;

  public static I2CAPI getInstance() {
    if (instance == null) {
      instance = new I2CJNI();
      if (!instance.i2cSetup()) {
        instance = null;
      }
    }
    return instance;
  }

  public native boolean i2cSetup();
  public native boolean i2cSetSlave(int addr);
  public native boolean i2cWrite(byte[] data, int length);
  public native int i2cRead(byte[] data, int length);
}
