/** QueryHardware
 *
 * @author pquiring
 */

import javaforce.*;

public class QueryHardware extends Thread {
  public void run() {
    while (Status.active) {
      Device[] devs = Config.current.getDevices();
      Cisco cisco = new Cisco();
      for(Device dev : devs) {
        if (dev.type == Device.TYPE_UNKNOWN) continue;
        switch (dev.type) {
          case Device.TYPE_UNKNOWN: continue;
          case Device.TYPE_CISCO: cisco.queryConfig(dev); break;
        }
      }
      //wait 5 mins
      for(int a=0;a<5 * 60 && Status.active;a++) {
        JF.sleep(1000);
      }
    }
  }
}
