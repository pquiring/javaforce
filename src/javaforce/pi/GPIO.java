package javaforce.pi;

/** GPIO
 *
 * Used to configure/read/write Raspberry PI GPIO pins.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.jni.*;

public class GPIO {
  static {
    JFNative.load();
  }
  public static boolean init() {
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
      return ninit(addr);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  private native static boolean ninit(int addr);
  public native static boolean configOutput(int idx);
  public native static boolean configInput(int idx);
  public native static boolean write(int idx, boolean state);
  public native static boolean read(int idx);
}
