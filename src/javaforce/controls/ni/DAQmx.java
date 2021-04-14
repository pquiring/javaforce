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
 * CounterInput : cDAQ9188-189E9F4Mod1/ctr0/pfi0
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.*;
import javaforce.controls.*;

public class DAQmx {
  public static void load() {
    if (loaded) return;
    JFNative.load();  //ensure native library is loaded
    if (JFNative.loaded) {
      loaded = daqInit();
    }
  }

  public static boolean loaded;

  private static native boolean daqInit();

  public static native long createTask();
  public static native boolean createChannelAnalog(long task, String dev, double rate, long samples, double min, double max);
  public static native boolean createChannelDigital(long task, String dev, double rate, long samples);
  public static native boolean createChannelCounter(long task, String dev, double rate, long samples, double min, double max, String term, double measureTime, int divisor);
  public static native boolean startTask(long task);
  public static native int readTaskAnalog(long task, int numchs, double data[]);
  public static native int readTaskBinary(long task, int numchs, int data[]);
  public static native int readTaskDigital(long task, int numchs, int data[]);
  public static native int readTaskCounter(long task, int numchs, double freq[]);
  public static native boolean stopTask(long task);
  public static native boolean clearTask(long task);
  public static native void printError();  //prints any errors to stdout

  //DAQ instance

  private long handle;
  private enum types {AI,DI,CI};
  private types type;
  private int chs = 1;

  public boolean connect(String url) {
    //url = device/port
    //device = cDAQ9188-189E9F4Mod1
    //port = ai0 or port0/line0[:7] or ctr0/pfi0
    int idx = url.indexOf('/');
    String device = url.substring(0, idx);
    String port = url.substring(idx+1);
    int samples = (int)Controller.rate;
    if (samples == 0) samples = 1;
    samples = 1;
    if (port.startsWith("ai")) {
      //analog input (voltage)
      type = types.AI;
      handle = createTask();
      if (!createChannelAnalog(handle, url, Controller.rate, samples, -10, 10)) return false;
      if (startTask(handle)) return true;
      printError();
      return false;
    }
    else if (port.startsWith("port")) {
      //digital input
      type = types.DI;
      handle = createTask();
      if (!createChannelDigital(handle, url, Controller.rate, samples)) return false;
      // device/portx/liney:z
      int li = url.lastIndexOf("/");
      if (li != -1) {
        String ln = url.substring(li+1+4);
        String yz[] = ln.split(":");
        if (yz.length == 2) {
          int y = Integer.valueOf(yz[0]);
          int z = Integer.valueOf(yz[1]);
          chs = z - y + 1;
        }
      }
      if (startTask(handle)) return true;
      printError();
      return false;
    }
    else if (port.startsWith("ctr")) {
      //counter (freq) input
      type = types.CI;
      handle = createTask();
      //device = DEVICE/ctr0/pfi0
      String p[] = url.split("/");
      device = p[0] + "/" + p[1];
      port = "/" + p[0] + "/" + p[2];
      if (!createChannelCounter(handle, device, 20000000.0, samples, 2.0, 1000.0, port, 1.0 / Controller.rate, 4)) return false;
      if (startTask(handle)) return true;
      printError();
      return false;
    }
    System.out.println("Unsupported DAQmx host:" + url);
    return false;
  }

  public void finalize() {
    close();
  }

  public void close() {
    if (handle != 0) {
      stopTask(handle);
      clearTask(handle);
      handle = 0;
    }
  }

  public byte[] read() {
    int read = 0;
    byte out[] = null;
    switch (type) {
      case AI: {
        double data[] = new double[chs];
        read = readTaskAnalog(handle, 1, data);
        out = new byte[chs * 8];
        //copy data -> out
        int pos = 0;
        for(int a=0;a<chs;a++) {
          BE.setuint64(out, pos, Double.doubleToLongBits(data[a]));
          pos += 8;
        }
        break;
      }
      case DI: {
        int data[] = new int[chs];
        read = readTaskDigital(handle, chs, data);
        out = new byte[chs];
        //copy data -> out
        int pos = 0;
        for(int a=0;a<chs;a++) {
          out[pos++] |= (byte)(data[a]);
          if (pos == chs) pos = 0;
        }
        break;
      }
      case CI: {
        double data[] = new double[chs];
        read = readTaskCounter(handle, 1, data);
        out = new byte[chs * 8];
        int pos = 0;
        for(int a=0;a<chs;a++) {
          BE.setuint64(out, pos, Double.doubleToLongBits(data[a]));
          pos += 8;
        }
        break;
      }
    }
    if (read != chs) {
      printError();
    }
    return out;
  }
}
