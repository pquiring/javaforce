package javaforce.jni;

/** NI DAQmx JNI
 *
 * @author pquiring
 */

import javaforce.controls.ni.*;

public class DAQmxJNI extends DAQmx {
  public native boolean init();
  public native long createTask();
  public native boolean createChannelAnalog(long task, String dev, double rate, long samples, double min, double max);
  public native boolean createChannelDigital(long task, String dev, double rate, long samples);
  public native boolean createChannelCounter(long task, String dev, double rate, long samples, double min, double max, String term, double measureTime, int divisor);
  public native boolean startTask(long task);
  public native int readTaskAnalog(long task, int numchs, double[] data);
  public native int readTaskBinary(long task, int numchs, int[] data);
  public native int readTaskDigital(long task, int numchs, int[] data);
  public native int readTaskCounter(long task, int numchs, double[] freq);
  public native boolean stopTask(long task);
  public native boolean clearTask(long task);
  public native void printError();  //prints any errors to stdout
}
