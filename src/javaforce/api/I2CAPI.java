package javaforce.api;

/** I2C native API
 *
 * @author pquiring
 */

public interface I2CAPI {
  public boolean i2cSetup();
  public boolean i2cSetSlave(int addr);
  public boolean i2cWrite(byte[] data, int length);
  public int i2cRead(byte[] data, int length);
}
