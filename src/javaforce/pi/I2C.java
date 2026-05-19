package javaforce.pi;

/** I2C
 *
 * Provides access to read/write data on the I2C bus.
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.ffm.*;
import javaforce.jni.*;

public class I2C {
  private I2CAPI api;
  public static I2C getInstance() {
    if (FFM.enabled()) {
      return new I2C(I2CFFM.getInstance());
    } else {
      return new I2C(I2CJNI.getInstance());
    }
  }
  public I2C(I2CAPI api) {
    this.api = api;
  }
  public boolean i2cSetup() {
    return api.i2cSetup();
  }
  public boolean i2cSetSlave(int addr) {
    return api.i2cSetSlave(addr);
  }
  public boolean i2cWrite(byte[] data) {
    return api.i2cWrite(data, data.length);
  }
  public int i2cRead(byte[] data) {
    return api.i2cRead(data, data.length);
  }
}
