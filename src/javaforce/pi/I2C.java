package javaforce.pi;

/** I2C
 *
 * Provides access to read/write data on the I2C bus.
 *
 * @author pquiring
 */

import javaforce.jni.JFNative;

public class I2C {
  static {
    JFNative.load();
  }
  public native static boolean init();
  public native static boolean setSlave(int addr);
  public native static boolean write(byte data[]);
  public native static int read(byte data[]);
}
