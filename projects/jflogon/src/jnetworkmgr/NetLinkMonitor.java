package jnetworkmgr;

/**
 * NetLinkMonitor
 *
 * Uses 'ip monitor' to detect net link changes and start/stops dhcp clients as needed.
 *
 * Created : Sept 5, 2012
 *
 * @author pquiring
 */

import javaforce.*;

public class NetLinkMonitor extends Thread implements ShellProcessListener {
  public void run() {
    ShellProcess sp = new ShellProcess();
    sp.addListener(new NetLinkMonitor());
    sp.keepOutput(false);
    sp.run(new String[] {"ip", "monitor"}, true);
  }

  /*  Sample output:
2: eth0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc pfifo_fast state DOWN group default
    link/ether xx:xx:xx:xx:xx:xx brd ff:ff:ff:ff:ff:ff
2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP group default
    link/ether xx:xx:xx:xx:xx:xx brd ff:ff:ff:ff:ff:ff
   */

  public void shellProcessOutput(String str) {
    //detect link state changes and tearDown/setup interfaces accordingly
    String lns[] = str.split("\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].indexOf("state DOWN") != -1) {
        int i1 = lns[a].indexOf(" ");
        int i2 = lns[a].substring(i1+1).indexOf(":");
        String dev = lns[a].substring(i1+1).substring(0, i2);
        JFLog.log("jnetworkmgr:link down:" + dev);
        Interface iface = Server.This.getInterface(dev);
        iface.link = false;
        if (iface.dhcp4 || iface.dhcp6) {
          Server.This.tearDownInterface(dev);
        }
      }
      if (lns[a].indexOf("state UP") != -1) {
        int i1 = lns[a].indexOf(" ");
        int i2 = lns[a].substring(i1+1).indexOf(":");
        String dev = lns[a].substring(i1+1).substring(0, i2);
        JFLog.log("jnetworkmgr:link up:" + dev);
        Interface iface = Server.This.getInterface(dev);
        iface.link = true;
        if (iface.dhcp4 || iface.dhcp6) {
          Server.This.setupInterface(iface);
        }
      }
    }
  }
}
