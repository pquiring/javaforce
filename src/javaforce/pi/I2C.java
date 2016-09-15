package javaforce.pi;

/** I2C
 *
 * Provides access to read/write data on the I2C bus.
 *
 * @author pquiring
 */

public class I2C {
  public native boolean init();
  public native boolean write(byte data[]);
  public native int read(byte data[]);
}
