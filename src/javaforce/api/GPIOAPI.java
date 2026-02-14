package javaforce.api;

/** GPIO native API
 *
 * @author pquiring
 */

public interface GPIOAPI {
  public boolean gpioSetup(int addr);
  public boolean gpioConfigOutput(int idx);
  public boolean gpioConfigInput(int idx);
  public boolean gpioWrite(int idx, boolean state);
  public boolean gpioRead(int idx);
}
