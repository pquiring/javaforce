package javaforce.jni;

/** GPIO JNI
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.api.*;

public class GPIOJNI implements GPIOAPI {

  private static GPIOJNI instance;

  public static GPIOAPI getInstance() {
    if (instance == null) {
      instance = new GPIOJNI();
      if (!instance.init()) {
        instance = null;
      }
    }
    return instance;
  }

  private boolean init() {
    try {
      File file = new File("/proc/iomem");
      if (!file.exists()) throw new Exception("/proc/iomem not found");
      FileInputStream fis = new FileInputStream(file);
      byte[] data = fis.readAllBytes();
      fis.close();
      String[] lns = new String(data).split("\n");
      int addr = -1;
      for(int a=0;a<lns.length;a++) {
        //xxxxxxxx-xxxxxxxx /soc/gpio@0x7e200000
        String ln = lns[a];
        if (ln.contains("gpio")) {
          addr = Integer.valueOf(ln.substring(0,8), 16);
          break;
        }
      }
      if (addr == -1) throw new Exception("GPIO not found in /proc/iomem");
      return gpioSetup(addr);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  public native boolean gpioSetup(int addr);
  public native boolean gpioConfigOutput(int idx);
  public native boolean gpioConfigInput(int idx);
  public native boolean gpioWrite(int idx, boolean state);
  public native boolean gpioRead(int idx);
}
