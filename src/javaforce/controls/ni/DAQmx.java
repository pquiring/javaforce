package javaforce.controls.ni;

/** NI DAQ mx
 *
 * Requires NI Drivers loaded (nicaiu.dll)
 * Free download from ni.com
 *
 * Use NI MAX or SignalExpress to determine device names.
 * Examples device names:
 * AnalogInput : cDAQ9188-189E9F4Mod6/ai0
 * DigitalInput : cDAQ9188-189E9F4Mod8/port0/line0
 * DigitalInput(s) : cDAQ9188-189E9F4Mod8/port0/line0:7
 * CounterInput : cDAQ9188-189E9F4Mod1/ctr0  (NOTE:configTiming() dev = "pfi0")
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

  public static native long createTask();
  public static native boolean createChannelAnalog(long task, String dev, double min, double max);
  public static native boolean createChannelDigital(long task, String dev);
  public static native boolean createChannelCounter(long task, String dev, double min, double max, double measureTime, int divisor);
  public static native boolean configTiming(long task, String dev, double rate, long samples);
  public static native boolean startTask(long task);
  public static native int readTaskDouble(long task, double data[]);
  public static native int readTaskBinary(long task, int data[]);
  public static native int readTaskDigital(long task, int data[]);
  public static native int readTaskCounter(long task, int data[]);
  public static native boolean stopTask(long task);
  public static native boolean clearTask(long task);

  public static native void printError();  //prints any errors to stdout
}
