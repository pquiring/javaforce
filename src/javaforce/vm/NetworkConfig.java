package javaforce.vm;

/** NetworkConfig
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.net.*;

public class NetworkConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String ip;
  public String netmask;
  public String mac;

  public transient String state;

  public NetworkConfig(String name) {
    this.name = name;
  }

  public static void getInfo(NetworkConfig[] nics) {
    ShellProcess p = new ShellProcess();
    p.keepOutput(true);
    String output = p.run(new String[] {"/usr/bin/ip", "addr"}, true);
    if (output == null) return;
    /*
1: eth0: <...> state UP/DOWN
    link/ether 00:11:22:33:44:55 brd ff:ff:ff:ff:ff:ff ...
    inet 192.168.1.2/24 ...
2: eth2: <...> state UP/DOWN
    link/ether 00:11:22:33:44:55 brd ff:ff:ff:ff:ff:ff ...
    inet 192.168.2.2/24 ...
    */
    String[] lns = output.split("\n");
    NetworkConfig nic = null;
    for(int a=0;a<lns.length;a++) {
      String ln = lns[a].trim();
      if (ln.length() == 0) continue;
      char start = ln.charAt(0);
      if (start >= '0' && start <= '9') {
        String[] fs = ln.split("[:]");
        String name = fs[1].trim();
        nic = null;
        for(NetworkConfig n : nics) {
          if (n.name.equals(name)) {
            nic = n;
            break;
          }
        }
        if (nic != null) {
          nic.state = "down";
          int i1 = ln.indexOf('<');
          int i2 = ln.indexOf('>');
          if (i1 > 0 && i2 > 0) {
            String[] ss = ln.substring(i1+1,i2).split("[,]");
            for(String s : ss) {
              switch (s) {
                case "UP": nic.state = "up"; break;
              }
            }
          }
        }
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
        int cidr = Integer.valueOf(ip_mask.substring(slash + 1));
        nic.netmask = Subnet4.fromCIDR(cidr);
      }
    }
  }

  public boolean set_ip() {
    //assign ip address
    ShellProcess p = new ShellProcess();
    p.keepOutput(true);
    p.run(new String[] {"/usr/bin/ip", "addr", "add", ip + "/" + netmask, "dev", name}, true);
    return p.getErrorLevel() == 0;
  }

  private boolean link(String dir) {
    ShellProcess sp = new ShellProcess();
    sp.run(new String[] {"ip", "link", "set", name, dir}, true);
    return sp.getErrorLevel() == 0;
  }

  public boolean link_up() {
    return link("up");
  }

  public boolean link_down() {
    return link("down");
  }
}
