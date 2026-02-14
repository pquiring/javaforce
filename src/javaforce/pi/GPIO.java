package javaforce.pi;

/** GPIO
 *
 * Used to configure/read/write Raspberry PI GPIO pins.
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.ffm.*;
import javaforce.jni.*;

public class GPIO {
  private GPIOAPI api;

  public static GPIO getInstance() {
    if (FFM.enabled()) {
      return new GPIO(GPIOFFM.getInstance());
    } else {
      return new GPIO(GPIOJNI.getInstance());
    }
  }

  public GPIO(GPIOAPI api) {
    this.api = api;
  }
  public boolean gpioSetup(int idx) {
    return api.gpioSetup(idx);
  }
  public boolean gpioConfigOutput(int idx) {
    return api.gpioConfigOutput(idx);
  }
  public boolean gpioConfigInput(int idx) {
    return api.gpioConfigInput(idx);
  }
  public boolean gpioWrite(int idx, boolean state) {
    return api.gpioWrite(idx, state);
  }
  public boolean gpioRead(int idx) {
    return api.gpioRead(idx);
  }
}
