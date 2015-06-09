package jnetworkmgr;

/**
 * Created : May 3, 2012
 *
 * @author pquiring
 */

import javaforce.*;

public class DHCPClient extends Thread implements ShellProcessListener {
  private Interface iface;
  private boolean ip4;
  private ShellProcess sp;
  private StringBuilder output = new StringBuilder();;

  public DHCPClient(Interface iface, boolean ip4) {
    this.iface = iface;
    this.ip4 = ip4;
  }

  public String getDevice() { return iface.dev; }

  public void run() {
    //start dhclient
    JFLog.log("DHCPCLient" + (ip4 ? "4:" : "6:") + iface.dev);
    sp = new ShellProcess();
    sp.addListener(this);
    sp.keepOutput(false);
    sp.run(new String[] {"dhclient", "-d", ip4 ? "-4" : "-6", iface.dev, "-sf", "/etc/dhcp/dhclient-script"}, true);
  }

  public void close() {
    sp.destroy();
  }

  public void shellProcessOutput(String string) {
//    JFLog.log("dhcp:" + string);  //debug
    output.append(string);
    while (true) {
      int idx = output.indexOf("\n");
      if (idx == -1) return;
      String msg = output.substring(0, idx);
      output.delete(0, idx+1);
      if (msg.indexOf("Network is down") != -1) {
        close();
        return;
      }
      if (msg.startsWith("bound to ")) {
        Server.dhcpSuccess(iface);
      }
    }
  }
}
