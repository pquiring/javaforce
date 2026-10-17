package javaforce.linux;

import java.util.*;

import javaforce.*;
import javaforce.ipc.*;
import javaforce.net.*;

/** WiFi control via wpa_supplicant over DBus.
 *
 * @author pquiring
 */

public class WiFi {
  private static boolean debug = false;

  private static String WPAS_DBUS_SERVICE = "fi.w1.wpa_supplicant1";
  private static String WPAS_DBUS_PATH = "/fi/w1/wpa_supplicant1";
  private static String WPAS_DBUS_INTERFACE = "fi.w1.wpa_supplicant1";
  private static String WPAS_DBUS_IFACE_INTERFACE = "fi.w1.wpa_supplicant1.Interface";
  private static String WPAS_DBUS_IFACE_BSS = "fi.w1.wpa_supplicant1.BSS";

  private DBus dbus;

  private boolean dbus_connect() {
    DBus.debug = debug;
    if (dbus != null) {
      dbus_disconnect();
    }
    dbus = new DBus(new EndPoint() {
      public String getEndPointName() {
        return null;
      }
      public Object dispatch(String object, String iface, String method, Object[] args) throws Exception {
        return null;
      }
    });
    return dbus.connect();
  }

  private void dbus_disconnect() {
    if (dbus != null) {
      dbus.disconnect();
      dbus = null;
    }
  }

  @SuppressWarnings("unchecked")
  private void print(JFDictionary dict, String indent) {
    String[] keys = (String[])dict.map.keySet().toArray(new String[0]);
    for(int idx=0;idx<keys.length;idx++) {
      String key = keys[idx];
      JFLog.log(indent + "key=" + key);
      Object value = dict.map.get(keys[idx]);
      JFLog.log(indent + "value=" + value);
      if (value instanceof JFVariant) {
        JFVariant v = (JFVariant)value;
        value = v.value;
        JFLog.log(indent + "value=" + value);
      }
      if (value instanceof JFDictionary) {
        JFLog.log(indent + "dict{");
        print((JFDictionary)value, indent + "  ");
        JFLog.log(indent + "}");
      }
      if (value instanceof String[]) {
        String[] values = (String[])value;
        for(String val : values) {
          JFLog.log(indent + "value=" + val);
        }
      }
      if (value instanceof byte[]) {
        String str = new String((byte[])value);
        JFLog.log(indent + "value=" + str);
      }
    }
  }

  private String getInterface(String dev) throws Exception {
    JFObjectPath iface_path = (JFObjectPath)dbus.invoke(WPAS_DBUS_SERVICE, WPAS_DBUS_PATH, WPAS_DBUS_INTERFACE, "GetInterface", dev);
    if (iface_path == null) throw new Exception("WiFi:GetInterface failed!");
    if (debug) JFLog.log("iface_path=" + iface_path.value);
    return iface_path.value;
  }

  /** Scan available AccessPoints on wireless device.
   * @param dev = wireless device
   */
  @SuppressWarnings("unchecked")
  public AccessPoint[] scan(String dev) {
    if (!dbus_connect()) return null;
    try {
      ArrayList<AccessPoint> list = new ArrayList<>();
      //get iface_path
      String iface_path = getInterface(dev);
      //initial scan
      JFDictionary<String, JFVariant> args = new JFDictionary<>(String.class, JFVariant.class);  //TYPE_ARRAY_DICT
      args.map.put("Type", new JFVariant<String>("active"));
      String msg = (String)dbus.invoke(WPAS_DBUS_SERVICE, iface_path, WPAS_DBUS_IFACE_INTERFACE, "Scan", args);
      //wait 2 seconds
      JF.sleep(2000);
      //read back results
      JFDictionary dict = (JFDictionary)dbus.invoke(WPAS_DBUS_SERVICE, iface_path, DBus.DBUS_PROPERTIES, "GetAll", WPAS_DBUS_IFACE_INTERFACE);
      if (debug) print(dict, "");
      String[] keys = (String[])dict.map.keySet().toArray(new String[0]);
      for(int idx=0;idx<keys.length;idx++) {
        String key = keys[idx];
        if (key.equals("BSSs")) {
          JFVariant BSSs = (JFVariant)dict.map.get(keys[idx]);
          JFObjectPath[] BSSsList = (JFObjectPath[])BSSs.value;
          for(JFObjectPath BSS_path : BSSsList) {
            if (debug) JFLog.log("Get BSS:" + BSS_path.value);
            JFDictionary bss_dict = (JFDictionary)dbus.invoke(WPAS_DBUS_SERVICE, BSS_path.value, DBus.DBUS_PROPERTIES, "GetAll", WPAS_DBUS_IFACE_BSS);
            if (debug) print(bss_dict, "");
            String[] bss_keys = (String[])bss_dict.map.keySet().toArray(new String[0]);
            for(int bidx=0;bidx<bss_keys.length;bidx++) {
              String bkey = bss_keys[bidx];
              if (bkey.equals("SSID")) {
                JFVariant SSID = (JFVariant)bss_dict.map.get(bss_keys[bidx]);
                String ssid = new String((byte[])SSID.value);
                if (debug) JFLog.log("SSID=" + ssid);
                AccessPoint ap = new AccessPoint();
                ap.ssid = ssid;
                ap.dev = dev;
                list.add(ap);
              }
            }
          }
        }
      }
      JFLog.log("Scan complete!");
      dbus_disconnect();
      return list.toArray(new AccessPoint[0]);
    } catch (Exception e) {
      JFLog.log(e);
      dbus_disconnect();
      return null;
    }
  }

  /** Connect to AccessPoint.
   * @param ap = Access Point
   */
  public boolean connect(AccessPoint ap) {
    if (!dbus_connect()) return false;
    try {
      //get iface_path
      String iface_path = getInterface(ap.dev);
      //AddNetwork
      JFDictionary<String, JFVariant> args = new JFDictionary<>(String.class, JFVariant.class);  //TYPE_ARRAY_DICT
      args.map.put("ssid", new JFVariant<String>(ap.ssid));
      args.map.put("psk", new JFVariant<String>(ap.passwd));
      args.map.put("key_mgmt", new JFVariant<String>("WPA-PSK"));
      JFObjectPath network_path = (JFObjectPath)dbus.invoke(WPAS_DBUS_SERVICE, iface_path, WPAS_DBUS_IFACE_INTERFACE, "AddNetwork", args);
      //SelectNetwork
      String result = (String)dbus.invoke(WPAS_DBUS_SERVICE, iface_path, WPAS_DBUS_IFACE_INTERFACE, "SelectNetwork", network_path);
      //TODO : confirm action
      dbus_disconnect();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      dbus_disconnect();
    }
    return false;
  }

  /** Disconnect from AccessPoint.
   * @param ap = Access Point
   */
  public boolean disconnect(AccessPoint ap) {
    if (!dbus_connect()) return false;
    try {
      //get iface_path
      String iface_path = getInterface(ap.dev);
      //Disconnect
      String result = (String)dbus.invoke(WPAS_DBUS_SERVICE, iface_path, WPAS_DBUS_IFACE_INTERFACE, "Disconnect");
      //TODO : confirm action
      dbus_disconnect();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      dbus_disconnect();
    }
    return false;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      JFLog.log("Usage : Wifi {cmd}");
      return;
    }
    WiFi wifi = new WiFi();
    switch (args[0]) {
      case "scan": {
        if (args.length < 2) {
          JFLog.log("Usage : Wifi scan {device}");
          return;
        }
        AccessPoint[] aps = wifi.scan(args[1]);
        for(AccessPoint ap : aps) {
          JFLog.log("AccessPoint:" + ap);
        }
        break;
      }
      case "connect": {
        if (args.length < 4) {
          JFLog.log("Usage : Wifi connect {device} {ssid} {password}");
          return;
        }
        AccessPoint ap = new AccessPoint();
        ap.dev = args[1];
        ap.ssid = args[2];
        ap.passwd = args[3];
        wifi.connect(ap);
        break;
      }
      case "disconnect": {
        if (args.length < 2) {
          JFLog.log("Usage : Wifi disconnect {device}");
          return;
        }
        AccessPoint ap = new AccessPoint();
        ap.dev = args[1];
        wifi.disconnect(ap);
        break;
      }
    }
  }
}
