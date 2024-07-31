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
              if (!cisco.queryMACTable(_dev)) {
                JFLog.log("Failed to query MAC table:" + dev);
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
      analyzeMACTables(devs);
      Config.save();
      //wait 5 mins
      if (debug) {
        JFLog.log("QueryHardware.run():sleep 5 mins");
      }
      for(int a=0;a<5 * 60 && Status.active;a++) {
        if (scan_now) break;
        JF.sleep(1000);
      }
      scan_now = false;
    }
  }
  private void analyzeMACTables(Device[] devs) {
    if (debug) {
      JFLog.log("QueryHardware.analyzeMACTables()");
    }
    //update switch uplinks
    for(Device dev : devs) {
      if (dev.type == Device.TYPE_UNKNOWN) continue;
      if (dev.hardware == null) continue;
      switch (dev.type) {
        case Device.TYPE_UNKNOWN:
          break;
        case Device.TYPE_CISCO:
          MACTableEntry[] mtes = dev.hardware.getMACTable();
          for(MACTableEntry mte : mtes) {
            Device child = Config.current.getDevice(mte.mac);
            if (child == null) break;
            if (child.hardware != null) {
              Port port = dev.getPort(mte.port, false);
              if (port == null) break;
              port.isUplink = true;
            }
          }
          break;
      }
    }
    //update device locations
    for(Device dev : devs) {
      if (dev.type == Device.TYPE_UNKNOWN) continue;
      if (dev.hardware == null) continue;
      switch (dev.type) {
        case Device.TYPE_UNKNOWN:
          break;
        case Device.TYPE_CISCO:
          MACTableEntry[] mtes = dev.hardware.getMACTable();
          for(MACTableEntry mte : mtes) {
            Device child = Config.current.getDevice(mte.mac);
            if (child == null) break;
            Port port = dev.getPort(mte.port, false);
            if (port == null) break;
            if (port.isUplink) break;
            child.loc = dev.mac + ":" + mte.port;
          }
          break;
      }
    }
  }
}
