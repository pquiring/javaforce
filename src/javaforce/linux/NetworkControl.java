package javaforce.linux;

/** Network Control
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.net.*;

public class NetworkControl {
  public static int logid = 0;
  public static void setLog(int id) {
    logid = id;
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
}
