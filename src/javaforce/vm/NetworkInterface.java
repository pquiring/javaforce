package javaforce.vm;

/** Network physical interface.
 *
 */

import java.io.*;

import javaforce.*;

public class NetworkInterface implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String ip;
  public String netmask;
  public String mac;

  protected NetworkInterface(String name) {
    this.name = name;
  }

  private native static String[] nlistPhys();
  /** List server physical network interfaces. */
  public static NetworkInterface[] listPhysical() {
    String[] list = nlistPhys();
    if (list == null) list = new String[0];
    NetworkInterface[] nics = new NetworkInterface[list.length];
    for(int idx = 0;idx<list.length;idx++) {
      nics[idx] = new NetworkInterface(list[idx]);
    }
    getInfo(nics);
    return nics;
  }

  private static void getInfo(NetworkInterface[] nics) {
    ShellProcess p = new ShellProcess();
    p.keepOutput(true);
    String output = p.run(new String[] {"/usr/bin/ip", "addr"}, true);
    if (output == null) return;
    /*
1: eth0: <...>
    link/ether 00:11:22:33:44:55 brd ff:ff:ff:ff:ff:ff ...
    inet 192.168.1.2/24 ...
2: eth2: <...>
    link/ether 00:11:22:33:44:55 brd ff:ff:ff:ff:ff:ff ...
    inet 192.168.2.2/24 ...
    */
    String[] lns = output.split("\n");
    NetworkInterface nic = null;
    int idx = 1;
    for(int a=0;a<lns.length;a++) {
      String ln = lns[a].trim();
      if (ln.length() == 0) continue;
      String start = Integer.toString(idx) + ":";
      if (ln.startsWith(start)) {
        String[] f = ln.split("[:]");
        String name = f[1].trim();
        nic = null;
        for(NetworkInterface n : nics) {
          if (n.name.equals(name)) {
            nic = n;
            break;
          }
        }
        idx++;
      }
      if (nic != null && ln.startsWith("link/ether")) {
        //MAC
        String[] f = ln.split("[ ]");
        nic.mac = f[1];
      }
      if (nic != null && ln.startsWith("inet ")) {
        //IP4/NETMASK
        String[] f = ln.split("[ ]");
        String ip_mask = f[1];
        int slash = ip_mask.indexOf('/');
        if (slash == -1) continue;
        nic.ip = ip_mask.substring(0, slash);
        nic.mac = ip_mask.substring(slash + 1);
      }
    }
  }
}
