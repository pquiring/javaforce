package javaforce.controls.ni;

/** NI-DAQmx test
 *
 * @author pquiring
 */

public class Test {
  public static void main(String args[]) {
    double f[] = new double[20];
    int i[] = new int[20];
    DAQmx.load();

    try {
      long task = DAQmx.createTask();
      if (task == 0) throw new Exception("createTask failed");
      if (!DAQmx.createChannelAnalog(task, "cDAQ9188-189E9F4Mod6/ai0", -10, 10)) throw new Exception("createChannel failed");
      if (!DAQmx.configTiming(task, null, 20.0, 20)) throw new Exception("configTiming failed");
      int read = DAQmx.readTaskDouble(task, f);
      if (read == 0) throw new Exception("no data");
      System.out.println("read=" + read);
      for(int a=0;a<read;a++) {
        System.out.println("A[" + a + "]=" + f[a]);
      }
      if (!DAQmx.stopTask(task)) throw new Exception("stopTask failed");
      if (!DAQmx.clearTask(task)) throw new Exception("clearTask failed");
    } catch (Exception e) {
      e.printStackTrace();
      DAQmx.printError();
      System.exit(0);
    }

    try {
      long task = DAQmx.createTask();
      if (task == 0) throw new Exception("createTask failed");
      if (!DAQmx.createChannelDigital(task, "cDAQ9188-189E9F4Mod8/port0/line0")) throw new Exception("createChannel failed");
      if (!DAQmx.configTiming(task, null, 20.0, 20)) throw new Exception("configTiming failed");
      int read = DAQmx.readTaskDigital(task, i);
      if (read == 0) throw new Exception("no data");
      System.out.println("read=" + read);
      for(int a=0;a<read;a++) {
        System.out.println("D[" + a + "]=" + i[a]);
      }
      if (!DAQmx.stopTask(task)) throw new Exception("stopTask failed");
      if (!DAQmx.clearTask(task)) throw new Exception("clearTask failed");
    } catch (Exception e) {
      e.printStackTrace();
      DAQmx.printError();
      System.exit(0);
    }

    try {
      long task = DAQmx.createTask();
      if (task == 0) throw new Exception("createTask failed");
      if (!DAQmx.createChannelCounter(task, "cDAQ9188-189E9F4Mod1/ctr0", 1, 1000, 0.1, 1)) throw new Exception("createChannel failed");
      if (!DAQmx.configTiming(task, "pfi0", 10.0, 5)) throw new Exception("configTiming failed");
      i = new int[5];
      int read = DAQmx.readTaskCounter(task, i);
      if (read == 0) throw new Exception("no data");
      for(int a=0;a<read;a++) {
        System.out.println("C[" + a + "]=" + i[a]);
      }
      if (!DAQmx.stopTask(task)) throw new Exception("stopTask failed");
      if (!DAQmx.clearTask(task)) throw new Exception("clearTask failed");
    } catch (Exception e) {
      e.printStackTrace();
      DAQmx.printError();
      System.exit(0);
    }
  }
}
