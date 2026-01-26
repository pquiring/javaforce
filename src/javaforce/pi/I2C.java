package javaforce.pi;

/** I2C
 *
 * Provides access to read/write data on the I2C bus.
 *
 * @author pquiring
 */

import javaforce.jni.*;

public interface I2C {
  public static I2C getInstance() {
    return I2CJNI.getInstance();
  }
  public boolean i2cSetSlave(int addr);
  public boolean i2cWrite(byte[] data);
  public int i2cRead(byte[] data);
}
