/** QueryHardware
 *
 * @author pquiring
 */

import javaforce.*;

public class QueryHardware extends Thread {
  public static boolean scan_now = false;
  public void run() {
    while (Status.active) {
      Device[] devs = Config.current.getDevices();
      Cisco cisco = new Cisco();
      for(Device dev : devs) {
        if (dev.type == Device.TYPE_UNKNOWN) continue;
        switch (dev.type) {
          case Device.TYPE_UNKNOWN:
            continue;
          case Device.TYPE_CISCO:
            Device _dev = dev.clone();
            cisco.queryConfig(_dev);
            cisco.queryStatus(_dev);
            dev.hardware = _dev.hardware;
            break;
        }
      }
      Config.save();
      //wait 5 mins
      for(int a=0;a<5 * 60 && Status.active;a++) {
        if (scan_now) break;
        JF.sleep(1000);
      }
      scan_now = false;
    }
  }
}