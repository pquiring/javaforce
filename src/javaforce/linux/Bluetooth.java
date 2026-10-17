package javaforce.linux;

import java.util.*;

import javaforce.*;
import javaforce.ipc.*;

/** Bluetooth control via Bluez over DBus.
 *
 * Currently not working.
 *
 * @author pquiring
 */

public class Bluetooth {
  private static boolean debug = true;

  private DBus dbus;

  private static String BLUEZ_SERVICE = "org.bluez";
  private static String BLUEZ_ROOT_PATH = "/";
  private static String BLUEZ_ADAPTER_PATH = "/org/bluez/";
  private static String ADAPTER_INTERFACE = "org.bluez.Adapter1";
  private static String DEVICE_INTERFACE = "org.bluez.Device1";
  private static String AGENT_MANAGER_INTERFACE = "org.bluez.AgentManager1";
  private static String BLUEZ_AGENT_PATH = "/org/bluez/agent";

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
        //Release
        //RequestConfirmation
        //RequestPasskey || RequestPinCode
        JFLog.log("dispatch:" + method);
        for(Object arg : args) {
          JFLog.log("  arg:" + arg);
        }
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

  public boolean setPowered(String hci, boolean state) {
    if (!dbus_connect()) return false;
    try {
      dbus.invoke(BLUEZ_SERVICE, BLUEZ_ADAPTER_PATH + hci, DBus.DBUS_PROPERTIES, "Set", ADAPTER_INTERFACE, "Powered", new JFVariant<Boolean>(state));
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    dbus_disconnect();
    return false;
  }

  private boolean registerAgent() {
    try {
      devs = new ArrayList<>();
      dbus.invoke(BLUEZ_SERVICE, BLUEZ_ROOT_PATH, AGENT_MANAGER_INTERFACE, "RegisterAgent", new JFObjectPath(BLUEZ_AGENT_PATH), "NoInputNoOutput");
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean unregisterAgent() {
    try {
      devs = new ArrayList<>();
      dbus.invoke(BLUEZ_SERVICE, BLUEZ_ROOT_PATH, AGENT_MANAGER_INTERFACE, "UnregisterAgent");
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private ArrayList<String> devs;

  public String[] scan(String hci) {
    if (!dbus_connect()) return null;
    try {
      devs = new ArrayList<>();
      dbus.subscribe("type='signal', interface='org.freedesktop.DBus.ObjectManager', member='InterfacesAdded'");
      dbus.invoke(BLUEZ_SERVICE, BLUEZ_ADAPTER_PATH + hci, ADAPTER_INTERFACE, "StartDiscovery");
      JF.sleep(2000);
      dbus.invoke(BLUEZ_SERVICE, BLUEZ_ADAPTER_PATH + hci, ADAPTER_INTERFACE, "StopDiscovery");
      dbus_disconnect();
      return devs.toArray(JF.StringArrayType);
    } catch (Exception e) {
      JFLog.log(e);
      dbus_disconnect();
      return null;
    }
  }

  public boolean connect(String hci, String dev) {
    if (!dbus_connect()) return false;
    try {
      registerAgent();
      devs = new ArrayList<>();
      dbus.invoke(BLUEZ_SERVICE, "/org/bluez/" + hci + "/dev_" + dev, DEVICE_INTERFACE, "Pair");
      unregisterAgent();
      dbus_disconnect();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      dbus_disconnect();
      return false;
    }
  }

  public boolean disconnect(String hci, String dev) {
    if (!dbus_connect()) return false;
    try {
      devs = new ArrayList<>();
      dbus.invoke(BLUEZ_SERVICE, "/org/bluez/" + hci + "/dev_" + dev, DEVICE_INTERFACE, "Disconnect");
      dbus_disconnect();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      dbus_disconnect();
      return false;
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      JFLog.log("Usage : Bluetooth {cmd}");
      return;
    }
    Bluetooth bt = new Bluetooth();
    switch (args[0]) {
      case "scan": {
        if (args.length < 2) {
          JFLog.log("Usage : Bluetooth scan {device}");
          return;
        }
        String[] devs = bt.scan(args[1]);
        for(String dev : devs) {
          JFLog.log("Device:" + dev);
        }
        break;
      }
      case "connect": {
        if (args.length < 4) {
          JFLog.log("Usage : Bluetooth connect {device}");
          return;
        }
        bt.connect("hci0", args[1]);
        break;
      }
      case "disconnect": {
        if (args.length < 2) {
          JFLog.log("Usage : Bluetooth disconnect {device}");
          return;
        }
        bt.disconnect("hci0", args[1]);
        break;
      }
    }
  }
}
