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
  public boolean setSlave(int addr);
  public boolean write(byte[] data);
  public int read(byte[] data);
}
