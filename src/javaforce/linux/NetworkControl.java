package javaforce.linux;

/** Network Control
 *
 * @author pquiring
 */

import javaforce.*;

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
}
