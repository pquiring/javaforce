package javaforce.pi;

import javaforce.api.*;
import javaforce.ffm.*;

/** GPIO.
 *
 * Used to configure/read/write Raspberry PI GPIO pins.
 *
 * @author pquiring
 */

public class GPIO {
  private GPIOAPI api;

  public static GPIO getInstance() {
    return new GPIO(GPIOFFM.getInstance());
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
