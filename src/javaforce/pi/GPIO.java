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
  public boolean configOutput(int idx);
  public boolean configInput(int idx);
  public boolean write(int idx, boolean state);
  public boolean read(int idx);
}
