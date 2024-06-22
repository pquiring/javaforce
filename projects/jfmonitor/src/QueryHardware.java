/** QueryHardware
 *
 * @author pquiring
 */

import javaforce.*;

public class QueryHardware extends Thread {
  public static boolean scan_now = false;
  public static boolean debug = false;
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
            String ip = dev.getip();
            if (debug) {
              JFLog.log("Query:" + dev);
            }
            try {
              Device _dev = dev.clone();
              if (!cisco.queryConfig(_dev)) {
                JFLog.log("Failed to query config:" + dev);
                break;
              }
              if (!cisco.queryVLANs(_dev)) {
                JFLog.log("Failed to query VLANs:" + dev);
                break;
              }
              if (!cisco.queryStatus(_dev)) {
                JFLog.log("Failed to query status:" + dev);
                break;
              }
              _dev.hardware.lastUpdate = System.currentTimeMillis();
              dev.hardware = _dev.hardware;
            } catch (Exception e) {
              JFLog.log(e);
            }
            break;
        }
      }
      Config.save();
      //wait 5 mins
      if (debug) {
        JFLog.log("Query:sleep 5 mins");
      }
      for(int a=0;a<5 * 60 && Status.active;a++) {
        if (scan_now) break;
        JF.sleep(1000);
      }
      scan_now = false;
    }
  }
}
