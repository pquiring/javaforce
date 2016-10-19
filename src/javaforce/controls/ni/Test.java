package javaforce.controls.ni;

/** NI-DAQmx test
 *
 * @author pquiring
 */

public class Test {
  public static void main(String args[]) {
    double data[] = new double[1000];
    DAQmx.load();
    try {
      long task = DAQmx.createTask();
      if (task == 0) throw new Exception("createTask failed");
      if (!DAQmx.createChannel(task, DAQmx.AI_Voltage, "cDAQ9188-189E9F4Mod6/ai0")) throw new Exception("createChannel failed");
      if (!DAQmx.configTiming(task, 10000.0, 1000)) throw new Exception("configTiming failed");
      int read = DAQmx.readTask(task, data);
      for(int a=0;a<read;a++) {
        System.out.println("V[" + a + "]=" + data[a]);
      }
      if (!DAQmx.stopTask(task)) throw new Exception("stopTask failed");
      if (!DAQmx.clearTask(task)) throw new Exception("clearTask failed");
    } catch (Exception e) {
      e.printStackTrace();
      DAQmx.printError();
    }
  }
}
