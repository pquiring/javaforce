package javaforce.pi;

/** GPIO
 *
 * Used to configure/read/write Raspberry PI GPIO pins.
 *
 * @author pquiring
 */

import javaforce.jni.*;

public interface GPIO {
  public static GPIO getInstance() {
    return GPIOJNI.getInstance();
  }
  public boolean gpioConfigOutput(int idx);
  public boolean gpioConfigInput(int idx);
  public boolean gpioWrite(int idx, boolean state);
  public boolean gpioRead(int idx);
}
