package javaforce.linux;

/** Network Control
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;

public class NetworkControl {
  public static int logid = 0;
  public static void setLog(int id) {
    logid = id;
  }
  /** List interfaces. */
  public static String[] list() {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/ip", "-a", "addr"}, true);
    JFLog.log(logid, output);
    if (sp.getErrorLevel() != 0) return null;
    ArrayList<String> devs = new ArrayList<>();
    String[] lns = output.split("\n");
    for(String ln : lns) {
      if (ln.trim().length() == 0) continue;
      if (ln.startsWith(" ")) continue;
      String[] fs = ln.split(" ");
      devs.add(fs[1]);
    }
    return devs.toArray(JF.StringArrayType);
  }
  public static boolean isUp(String dev) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/ip", "-a", "addr"}, true);
    JFLog.log(logid, output);
    if (sp.getErrorLevel() != 0) return false;
    String[] lns = output.split("\n");
    for(String ln : lns) {
      if (ln.startsWith(" ")) continue;
      String[] fs = ln.split(" ");
      String iface = fs[1];
      if (!iface.equals(dev)) continue;
      int len = fs.length;
      for(int i=0;i<len;i++) {
        String f = fs[i];
        switch (f) {
          case "state":
            i++;
            String state = fs[i];
            return state.equals("UP");
        }
      }
    }
    return false;
  }
  /** Set interface up. */
  public static boolean up(String dev) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/ip", "link", "set", dev, "up"}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  /** Set interface down. */
  public static boolean down(String dev) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/ip", "link", "set", dev, "down"}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  /** Wireless scan for SSIDs. */
  public static String[] wifi_scan(String dev) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/iwlist", dev, "scan"}, true);
    JFLog.log(logid, output);
    return output.split("\n");
  }
  /** Set wireless SSID. */
  public static boolean wifi_set_ssid(String dev, String ssid) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/iw", "dev", dev, "connect", ssid}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  /** Set wireless SSID with WEP encryption. */
  public static boolean wifi_set_ssid(String dev, String ssid, String keys0) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/iw", "dev", dev, "connect", ssid, "keys", "0:" + keys0}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  /** Get routing table. */
  public static Route[] routes_get() {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/ip", "route"}, true);
    JFLog.log(logid, output);
    String[] lns = output.split("\n");
    int cnt = lns.length;
    //ip route : <dest>[/<mask>] [via <gw>] [dev <dev>] proto dhcp src <ip> metric <#>
    //skip 2 lines (headers)
    ArrayList<Route> routes = new ArrayList<>();
    for(int i = 2; i < cnt; i++) {
      String ln = lns[i];
      String[] fs = ln.split(" +");  //space greedy
      Route route = new Route();
      route.dest_mask = new Subnet4(fs[0]);
      for(int p = 1;p < fs.length;p++) {
        String f = fs[p];
        switch (f) {
          case "via":
            p++;
            route.gateway = new IP4(fs[p]);
            break;
          case "dec":
            p++;
            route.dev = fs[p];
            break;
          case "metric":
            p++;
            route.metric = Integer.valueOf(fs[p]);
            break;
        }
      }
      routes.add(route);
    }
    return routes.toArray(Route.RouteArrayType);
  }
  public static boolean route_add(Route route) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/ip", "route", "add", route.dest_mask.toStringCIDR(), "via", route.gateway.toString(), "dev", route.dev}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean route_remove(Route route) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/ip", "route", "del", route.dest_mask.toStringCIDR()}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean route_edit(Route route) {
    route_remove(route);
    return route_add(route);
  }
  private static String networkd = "/usr/lib/systemd/network";
  /** Retrieves interface config from networkd. */
  public static NetworkConfig getConfig(String dev) {
    try {
      String file = networkd + "/20-" + dev + ".network";
      FileInputStream fis = new FileInputStream(file);
      String[] cfg = new String(fis.readAllBytes()).split("\n");
      fis.close();
      return NetworkConfig.fromNetworkd(cfg);
    } catch (FileNotFoundException e1) {
      return null;
    } catch (Exception e2) {
      JFLog.log(e2);
    }
    return null;
  }
  /** Saves networkd config file for interface. */
  public static void setConfig(String dev, NetworkConfig cfg) {
    try {
      String[] lns = cfg.toNetworkd();
      StringBuilder sb = new StringBuilder();
      for(String ln : lns) {
        sb.append(ln);
        sb.append("\n");
      }
      String file = networkd + "/20-" + dev + ".network";
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (Exception e2) {
      JFLog.log(e2);
    }
  }
  /** Forces networkd to reload config files. */
  public static boolean reload() {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/networkctl", "reload"}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }

  public static String gethostname() {
    return Linux.getHostname();
  }

  public static void sethostname(String hostname) {
    Linux.setHostname(hostname);
  }
}
