package javaforce.pi;

/** GPIO
 *
 * Used to configure/read/write Raspberry PI GPIO pins.
 *
 * @author pquiring
 */

public class GPIO {
  public native boolean init();
  public native boolean configOutput(int idx);
  public native boolean configInput(int idx);
  public native boolean write(int idx, boolean state);
  public native boolean read(int idx);
}
