package javaforce.controls.ni;

/** NI DAQ mx
 *
 * Requires NI Drivers loaded (nicaiu.dll)
 * Free download from ni.com
 *
 * Use NI MAX to determine device names.
 *
 * @author pquiring
 */

import javaforce.jni.*;

public class DAQmx {
  static {
    JFNative.load();  //ensure native library is loaded
    if (JFNative.loaded) {
      daqInit();
    }
  }

  public static void load() {}  //ensure native library is loaded

  private static native boolean daqInit();

  //channel types
  public static final int AI_Voltage = 1;

  public static native long createTask();
  public static native boolean createChannel(long task, int type, String dev);
  public static native boolean configTiming(long task, double rate, long samples);
  public static native boolean startTask(long task);
  public static native int readTask(long task, double data[]);
  public static native boolean stopTask(long task);
  public static native boolean clearTask(long task);

  public static native void printError();  //prints any errors to stdout
}
