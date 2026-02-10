/** Wireless Access Point (WAP)
 *
 * @author pquiring
 *
 */

import java.io.*;

import javaforce.*;
import javaforce.linux.*;

class WAPConnection extends Thread implements ShellProcessListener {
  public String pack;
  public String dev;
  public String ssid;
  public String encType;
  public String key;
  private volatile boolean wpa_success = false;
  private volatile boolean wpa_failed = false;
  private volatile boolean wpa_retry = false;
  private volatile Object wpa_lock = new Object();
  private ShellProcess wpa_supplicant;
  private ShellProcess wpa_log;

  public void run() {
    JFLog.log("connectWAP:dev=" + dev + ",ssid=" + ssid);
    Interface iface = Server.This.getInterface(dev);
    if (!iface.wireless) {
      JFLog.log("WAP:dev is not wireless:" + dev);
      Server.This.jbusClient.call(pack, "wapFailed", "");
      Server.This.pendingWAP = null;
      return;
    }
    iface.pack = pack;
    if (iface.link) {
      //already has a link???
      if (!NetworkControl.wifi_set_ssid(dev, "any")) {
        Server.This.jbusClient.call(pack, "wapFailed", "");
        Server.This.pendingWAP = null;
        return;
      }
      int cnt = 0;
      while (iface.link) {
        JF.sleep(500);
        cnt++;
        if (cnt == 10) {
          JFLog.log("WAP:Unable to drop current link");
          Server.This.jbusClient.call(pack, "wapFailed", "");
          Server.This.pendingWAP = null;
          return;
        }
      }
    }
    if (!encType.equals("OPEN")) {
      if (encType.equals("WEP")) {
        if (key.length() > 0) {
          if (!NetworkControl.wifi_set_ssid(dev, ssid, key)) {
            Server.This.jbusClient.call(pack, "wapFailed", "");
            Server.This.pendingWAP = null;
            return;
          }
        }
      } else {
        //WPA 1/2
        try {
          FileOutputStream fos = new FileOutputStream("/root/" + dev + ".conf");
          StringBuilder str = new StringBuilder();
          str.append("network={\n");
          str.append(" ssid=\"" + ssid + "\"\n");
          str.append(" psk=\"" + key + "\"\n");
          str.append("}\n");
          fos.write(str.toString().getBytes());
          fos.close();
          File log = new File("/root/" + dev + ".log");
          do {
            log.delete();
            log.createNewFile();
            wpa_retry = false;
            //wpa_supplicant doesn't output directly to stdout so I need to use it's log file instead
            new Thread() {
              public void run() {
                JFLog.log("Starting wpa_logger");
                wpa_log = new ShellProcess();
                wpa_log.addListener(WAPConnection.this);
                wpa_log.keepOutput(false);
                wpa_log.run(new String[]{"tail", "-f", "/root/" + dev + ".log"}, true);
              }
            }.start();
            new Thread() {
              public void run() {
                JFLog.log("Starting wpa_supplicant");
                wpa_supplicant = new ShellProcess();
                wpa_supplicant.addListener(WAPConnection.this);
                wpa_supplicant.keepOutput(false);
                wpa_supplicant.run(new String[]{"wpa_supplicant", "-i" + dev, "-c/root/" + dev + ".conf", "-f/root/" + dev + ".log"}, true);
              }
            }.start();
            //wait for success or failure
            synchronized (wpa_lock) {
              wpa_lock.wait();
            }
            if (!wpa_success) {
              if (wpa_log != null) {
                wpa_log.destroy();
                wpa_log = null;
              }
              if (wpa_supplicant != null) {
                wpa_supplicant.destroy();
                wpa_supplicant = null;
                JF.sleep(250);
                NetworkControl.up(dev);
              }
            }
          } while (wpa_retry);
          if ((!wpa_success) || (wpa_failed)) {
            if (wpa_log != null) {
              wpa_log.destroy();
              wpa_log = null;
            }
            if (wpa_supplicant != null) {
              wpa_supplicant.destroy();
              wpa_supplicant = null;
              JF.sleep(250);
              NetworkControl.up(dev);
            }
            Server.This.jbusClient.call(pack, "wapFailed", "");
            Server.This.pendingWAP = null;
            return;
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }

  public void init(String pack, String dev, String ssid, String encType, String key) {
    this.pack = pack;
    this.dev = dev;
    this.ssid = ssid;
    this.encType = encType;
    this.key = key;
  }

  public void close() {
    if (wpa_log != null) {
      wpa_log.destroy();
      wpa_log = null;
    }
    if (wpa_supplicant != null) {
      wpa_supplicant.destroy();
      wpa_supplicant = null;
      JF.sleep(250);
      NetworkControl.up(dev);
      wpa_failed = true;
      synchronized (wpa_lock) {
        wpa_lock.notify();
      }
    }
  }

  public void shellProcessOutput(String string) {
    String[] lns = string.split("\n");
    //      JFLog.log(string);  //test
    for (int a = 0; a < lns.length; a++) {
      if (lns[a].startsWith("CTRL-EVENT-DISCONNECTED")) {
        wpa_failed = true;
        synchronized (wpa_lock) {
          wpa_lock.notify();
        }
        continue;
      }
      if (lns[a].startsWith("CTRL-EVENT-CONNECTED")) {
        wpa_success = true;
        synchronized (wpa_lock) {
          wpa_lock.notify();
        }
        continue;
      }
      if (lns[a].endsWith("Device or resource busy")) {
        JFLog.log("wpa_supplicant busy : retrying");
        wpa_retry = true;
        synchronized (wpa_lock) {
          wpa_lock.notify();
        }
        continue;
      }
      if (lns[a].endsWith("Network is down")) {
        NetworkControl.up(dev);
      }
    }
  }
}
