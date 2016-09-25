package javaforce.pi;

/** GPIO
 *
 * Used to configure/read/write Raspberry PI GPIO pins.
 *
 * @author pquiring
 */

import javaforce.jni.*;

public class GPIO {
  static {
    JFNative.load();
  }
  public native static boolean init();
  public native static boolean configOutput(int idx);
  public native static boolean configInput(int idx);
  public native static boolean write(int idx, boolean state);
  public native static boolean read(int idx);
}
