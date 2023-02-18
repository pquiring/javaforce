/**
 * Created : May 31, 2012
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.jni.*;
import javaforce.jni.lnx.*;
import javaforce.utils.*;

public class DeviceMonitor extends Thread implements ShellProcessListener {
  private StringBuilder output = new StringBuilder();;
  private LnxPty pty;

  public void run() {
    LnxPty.init();
    // NOTE : udevadm requires a tty to operate ???
    if (true) {
      pty = LnxPty.exec("udevadm", new String[] {"udevadm", "monitor", "--kernel", null}, LnxPty.makeEnvironment());
      new Thread() {
        public void run() {
          byte data[] = new byte[1024];
          while (true) {
            int len = pty.read(data);
            if (len > 0) {
              shellProcessOutput(new String(data, 0, len));
            } else {
              JF.sleep(250);
            }
          }
        }
      }.start();
    } else {
      //does NOT work for some reason
      ShellProcess sp = new ShellProcess();
      sp.addListener(this);
      sp.keepOutput(false);
      sp.run(new String[] {"udevadm", "monitor", "--kernel"}, false);
    }
  }

  private static String quote(String str) {
    return "\"" + str + "\"";
  }

  public void shellProcessOutput(String string) {
    output.append(string.replaceAll("\r", ""));  //remove \r ??? is this a MS app?
    while (true) {
      int idx = output.indexOf("\n");
      if (idx == -1) return;
      String msg = output.substring(0, idx);
      output.delete(0, idx+1);
      if (msg.endsWith("(drm)")) {
        Startup.jbusClient.broadcast("org.jflinux.jfdesktop.", "videoChanged", quote("udev"));
        Startup.jbusClient.broadcast("org.jflinux.jfconfig.", "videoChanged", quote("udev"));
      }
      if (msg.endsWith("(power_supply)")) {
        Startup.jbusClient.broadcast("org.jflinux.jfdesktop.", "powerChanged", "");
      }
      if (msg.endsWith("(block)")) {
        //could be an audio cd-rom inserted
        //KERNEL [time.stamp] change /devices/.../block/sr0 (block)
        idx = msg.lastIndexOf("/");
        String dev = msg.substring(idx+1);
        idx = dev.indexOf(" ");
        dev = dev.substring(0, idx);
        if (dev.startsWith("sr")) {
          Startup.autoMounter.mount("/dev/" + dev);
        }
      }
    }
  }
}
