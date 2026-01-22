package javaforce.tests;

/** NI-DAQmx test
 *
 * @author pquiring
 */

import javaforce.controls.ni.*;

public class TestNI {
  public static void main(String[] args) {
    double[] f = new double[20];
    int[] i = new int[20];
    int[] b = new int[20 * 8];
    DAQmx ni = DAQmx.getInstance();
    if (ni == null) {
      System.out.println("DAQmx not loaded");
      return;
    }

    try {
      long task = ni.createTask();
      if (task == 0) throw new Exception("createTask failed");
      if (!ni.createChannelAnalog(task, "cDAQ9188-189E9F4Mod7/ai0", 20.0, 20, -10, 10)) throw new Exception("createChannel failed");
      if (!ni.startTask(task)) throw new Exception("startTask failed");
      int read = ni.readTaskAnalog(task, 1, f);
      if (read == 0) throw new Exception("no data");
      System.out.println("read=" + read);
      for(int a=0;a<read;a++) {
        System.out.println("A[" + a + "]=" + f[a]);
      }
      if (!ni.stopTask(task)) throw new Exception("stopTask failed");
      if (!ni.clearTask(task)) throw new Exception("clearTask failed");
    } catch (Exception e) {
      e.printStackTrace();
      ni.printError();
      System.exit(0);
    }

    try {
      long task = ni.createTask();
      if (task == 0) throw new Exception("createTask failed");
      if (!ni.createChannelDigital(task, "cDAQ9188-189E9F4Mod8/port0/line0:7", 20.0, 20)) throw new Exception("createChannel failed");
      if (!ni.startTask(task)) throw new Exception("startTask failed");
      int read = ni.readTaskDigital(task, 8, b);
      if (read == 0) throw new Exception("no data");
      System.out.println("read=" + read);
      read *= 8;
      for(int a=0;a<read;a++) {
        System.out.println("D[" + a + "]=" + b[a]);
      }
      if (!ni.stopTask(task)) throw new Exception("stopTask failed");
      if (!ni.clearTask(task)) throw new Exception("clearTask failed");
    } catch (Exception e) {
      e.printStackTrace();
      ni.printError();
      System.exit(0);
    }

    try {
      long task = ni.createTask();
      if (task == 0) throw new Exception("createTask failed");
      if (!ni.createChannelCounter(task, "cDAQ9188-189E9F4Mod1/ctr0"
              , 20000000, 20, 1, 1000, "/cDAQ9188-189E9F4Mod1/pfi0", 1.0 / 20.0, 1)) throw new Exception("createChannel failed");
      if (!ni.startTask(task)) throw new Exception("startTask failed");
      int read = ni.readTaskCounter(task, 1, f);
      if (read == 0) throw new Exception("no data");
      for(int a=0;a<read;a++) {
        System.out.println("C[" + a + "]=" + f[a]);
      }
      if (!ni.stopTask(task)) throw new Exception("stopTask failed");
      if (!ni.clearTask(task)) throw new Exception("clearTask failed");
    } catch (Exception e) {
      e.printStackTrace();
      ni.printError();
      System.exit(0);
    }
  }
}
