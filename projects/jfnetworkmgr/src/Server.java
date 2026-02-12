/** jfNetworkManager
 *
 * Created : May 3, 2012
 *
 * Depends on systemd-networkd.
 * wpa_supplicant for WIFI
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;
import javaforce.net.*;
import javaforce.linux.*;

public class Server {
  private String wapList = "";
  private Timer wapTimer;

  public VPNConnection pendingVPN;
  public WAPConnection pendingWAP;

  public static Server This;
  public JBusClient jbusClient;
  public ArrayList<VPNConnection> vpnConnections = new ArrayList<VPNConnection>();
  public ArrayList<WAPConnection> wapConnections = new ArrayList<WAPConnection>();

  private static final boolean bluez3 = false;  //no longer available

  public static void main(String args[]) {
    //this is currently not used : unless jfnetworkmgr becomes a seperate package
    JFLog.init("/var/log/jfnetworkmgr.log", true);
    new Server().start();
  }

  public static void serviceStart(String[] args) {
    main(args);
  }

  public static void serviceStop() {
    if (This != null) {
      This.stop();
    }
  }

  public void start() {
    try {
      This = this;
      jbusClient = new JBusClient("org.jflinux.jfnetworkmgr", new JBusMethods());
      jbusClient.start();
      createWAPTimer();
      checkInterfaces();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void stop() {
    if (jbusClient != null) {
      jbusClient.close();
      jbusClient = null;
    }
    cancelWAPTimer();
  }

  private String[] listIFs() {
    return NetworkControl.list();
  }

  public static String mask(String ipstr, String maskstr) {
    try {
      InetAddress ip = InetAddress.getByName(ipstr);
      byte ip4[] = ip.getAddress();
      InetAddress mask = InetAddress.getByName(maskstr);
      byte mask4[] = mask.getAddress();
      for(int a=0;a<ip4.length;a++) {
        ip4[a] &= mask4[a];
      }
      InetAddress ret = InetAddress.getByAddress(ip4);
      return ret.getHostAddress();
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  private void startIF(String dev) {
    JFLog.log("jfnetworkmgr:Starting:" + dev);
    if (!NetworkControl.up(dev)) {
      JFLog.log("jfnetworkmgr:Failed to start interface:" + dev);
      return;
    }
  }

  private void stopIF(String dev) {
    JFLog.log("jfnetworkmgr:Stoping:" + dev);
    if (!NetworkControl.down(dev)) {
      JFLog.log("jfnetworkmgr:Failed to stop interface:" + dev);
    }
  }

  private boolean isIFactive(String dev) {
    return NetworkControl.isUp(dev);
  }

  private void checkInterfaces() {
    String[] devs = listIFs();
    if (devs == null) return;
    boolean reload = false;
    for(String dev : devs) {
      NetworkConfig cfg = NetworkControl.getConfig(dev);
      if (cfg == null) {
        cfg = new NetworkConfig(dev);
        NetworkControl.setConfig(dev, cfg);
        reload = true;
      }
    }
    if (reload) {
      NetworkControl.reload();
    }
  }

  private void getWAPList() {
    String newWapList = "";
    String[] ifaces = listIFs();
    for(String iface : ifaces) {
      if (!iface.startsWith("w")) continue;
      String[] output = NetworkControl.wifi_scan(iface);
      newWapList += genWAPList(iface, output);
    }
    wapList = newWapList;
    jbusClient.call("org.jflinux.jfsystemmgr", "broadcastWAPList", quote(wapList));
  }

  private String genWAPList(String dev, String[] scan) {
    int cnt = 0;
    String list = "";
    String wap = null, encType = "?";
    for(int a=0;a<scan.length;a++) {
      String ln = scan[a].replaceAll("\"", "\'").trim();
      if (ln.startsWith("Cell ")) {
        if (wap != null) {
          list += wap;
          if (isWAPactive(wap)) list += " *";
          list += "|" + encType + "|";
          cnt++;
        }
        wap = null;
        encType = "OPEN";
        continue;
      }
      if (ln.startsWith("ESSID:")) {
        wap = ln.substring(7,ln.length() - 1);  //remove quotes
        if (wap.length() == 0) {
          wap = null;
          encType = null;
          continue;
        }
        continue;
      }
      if (ln.startsWith("Encryption key:")) {
        //comes before ESSID in list
        String value = ln.substring(15);
        if (value.equals("on")) {
          encType = "WEP";  //unless WPA is found later down
        }
        continue;
      }
      if (ln.endsWith("WPA Version 1")) {
        encType = "WPA";
        continue;
      }
      if (ln.endsWith("WPA2 Version 1")) {
        encType = "WPA";
        continue;
      }
    }
    if (wap != null) {
      list += wap;
      if (isWAPactive(wap)) list += " *";
      list += "|" + encType + "|";
      cnt++;
    }
    return dev + "|" + cnt + "|" + list;
  }
  private boolean checkWireless() {
    String[] ifaces = listIFs();
    for(String iface : ifaces) {
      if (iface.startsWith("w")) return true;
    }
    return false;
  }
  private void createWAPTimer() {
    wapTimer = new java.util.Timer();
    wapTimer.schedule(new TimerTask() {
      public void run() {
        //update wireless list if not connected to any every minute
        if ((wapConnections.isEmpty()) && checkWireless()) getWAPList();
      }
    }, 0, 60 * 1000);
  }
  private void cancelWAPTimer() {
    if (wapTimer != null) {
      wapTimer.cancel();
      wapTimer = null;
    }
  }

  public static class VPN {
    public String name;
    public String host;
    public String caps;  //windows = pre-defined, else: pap, mschap, etc.
    public String capsOpts;  //flags
    public String routes;  //comma list
    public String routeOpts;  //flags
    public String user, pass, domain;
    public String domainsearch;
  }

  public static class VPNConfig {
    public VPN vpn[];
  }

  private VPNConfig vpnConfig;
  private String vpnConfigFile = "/etc/jfconfig.d/vpn.xml";

  private synchronized void loadVPNConfig() {
    defaultVPNConfig();
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(vpnConfigFile);
      xml.read(fis);
      xml.writeClass(vpnConfig);
    } catch (FileNotFoundException e1) {
      defaultVPNConfig();
    } catch (Exception e2) {
      JFLog.log(e2);
      defaultVPNConfig();
    }
  }

  private void defaultVPNConfig() {
    vpnConfig = new VPNConfig();
    vpnConfig.vpn = new VPN[0];
  }

  private boolean isWAPactive(String ssid) {
    for(int a=0;a<wapConnections.size();a++) {
      if (wapConnections.get(a).ssid.equals(ssid)) return true;
    }
    return false;
  }

  private static String quote(String str) {
    return "\"" + str + "\"";
  }

  private String bluetoothctlPrompt = ".*\\p{Punct}bluetooth\\p{Punct}.*\\p{Punct}";  //ESC[0;49m[bluetooth]ESC[0m#

  public class JBusMethods {
//System API
    public void setHostname(String hostname) {
      Linux.setHostname(hostname);
    }
//Network API
    public void notifyUp(String dev) {}
    public void notifyDown(String dev) {}
    public void ifUp(String dev) {
      JFLog.log("ifUp:" + dev);
      if (isIFactive(dev)) {JFLog.log("already up"); return;}
      startIF(dev);
    }
    public void ifDown(String dev) {
      JFLog.log("ifDown:" + dev);
      if (!isIFactive(dev)) {JFLog.log("already down"); return;}
      stopIF(dev);
    }
    public void setConfig(String dev, String cfg) {
      NetworkConfig nc = NetworkConfig.fromNetworkd(cfg.split("\n"));
      NetworkControl.setConfig(dev, nc);
    }
//WIFI API
    public void getWAPList(String pack) {
      jbusClient.call(pack, "setWAPList", quote(wapList));
    }
    public void connectWAP(String pack, String dev, String ssid, String encType, String key) {
      if (pendingWAP != null) return;
      WAPConnection wap = new WAPConnection();
      wap.init(pack,dev,ssid,encType,key);
      pendingWAP = wap;
      wap.start();
      wapConnections.add(wap);
    }
    public void disconnectWAP(String pack, String dev) {
      ShellProcess sp = new ShellProcess();
      sp.run(new String[] {"iwconfig", dev, "essid", "any"}, false);
      //stop wpa_supplicant if used
      for(int a=0;a<wapConnections.size();) {
        if (wapConnections.get(a).dev.equals(dev)) {
          wapConnections.get(a).close();
          wapConnections.remove(a);
        } else {
          a++;
        }
      }
    }
    public void cancelWAP() {
      if (pendingWAP == null) return;
      pendingWAP.close();
      pendingWAP = null;
    }
//BlueTooth API
    public void getBTdevices(String pack) {
      ShellProcess sp = new ShellProcess();
//      ShellProcess.log = true;
//      ShellProcess.logPrompt = true;
      String list = "";
      if (bluez3) {
        sp.addRegexResponse(bluetoothctlPrompt, "show\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "exit\n", false);
        String output = sp.run(new String[] {"bluetoothctl"}, false);
        JFLog.log("output=" + output);
        String lns[] = output.split("\n");
        for(int a=0;a<lns.length;a++) {
          if (lns[a].startsWith("Controller")) {
            if (list.length() > 0) list += "|";
            list += lns[a].substring(11, 11+17);
          } else if (lns[a].indexOf("Powered") != -1) {
            if (list.length() > 0) list += "|";
            list += lns[a].indexOf("yes") != -1 ? "UP" : "DOWN";
          }
        }
      } else {
        String output = sp.run(new String[] {"hciconfig"}, false);
        String lns[] = output.split("\n");
        for(int a=0;a<lns.length;a++) {
          if (lns[a].startsWith(" ")) {
            if (lns[a].trim().startsWith("UP")) {
              if (list.length() > 0) list += "|";
              list += "UP";
            }
            if (lns[a].trim().startsWith("DOWN")) {
              if (list.length() > 0) list += "|";
              list += "DOWN";
            }
          } else {
            String dev = lns[a];
            int idx = dev.indexOf(" ");
            if (idx != -1) dev = dev.substring(0, idx);
            idx = dev.indexOf(":");
            if (idx != -1) dev = dev.substring(0, idx);
            if (list.length() > 0) list += "|";
            list += dev;
          }
        }
      }
      jbusClient.call(pack, "setBTdevices", quote(list));
    }
    //enable bluetook controlling device
    public void enableBTdevice(String pack, String cmac) {
      final ShellProcess sp = new ShellProcess();
      if (bluez3) {
        sp.addRegexResponse(bluetoothctlPrompt, "select " + cmac + "\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "power on\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "exit\n", false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          public void run() {
            try {
              sp.getOutputStream().write(("exit\n").getBytes());
              sp.getOutputStream().flush();
            } catch (Exception e) {}
          }
        }, 5 * 1000, 1000);
        String output = sp.run(new String[] {"bluetoothctl"}, false);
//JFLog.log("enableBTdevice.output=" + output);
        timer.cancel();
        if (output.indexOf("succeeded") != -1) {
          jbusClient.call(pack, "btSuccess", "");
        } else {
          jbusClient.call(pack, "btFailed", "");
        }
      } else {
        String output = sp.run(new String[] {"hciconfig", cmac/*dev*/, "up"}, false);
        if (sp.getErrorLevel() == 0) {
          jbusClient.call(pack, "btSuccess", "");
        } else {
          jbusClient.call(pack, "btFailed", "");
        }
      }
    }
    //disable bluetook controlling device
    public void disableBTdevice(String pack, String cmac) {
      final ShellProcess sp = new ShellProcess();
      if (bluez3) {
        sp.addRegexResponse(bluetoothctlPrompt, "select " + cmac + "\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "power off\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "exit\n", false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          public void run() {
            try {
              sp.getOutputStream().write(("exit\n").getBytes());
              sp.getOutputStream().flush();
            } catch (Exception e) {}
          }
        }, 5 * 1000, 1000);
        String output = sp.run(new String[] {"bluetoothctl"}, false);
        timer.cancel();
        if (output.indexOf("succeeded") != -1) {
          jbusClient.call(pack, "btSuccess", "");
        } else {
          jbusClient.call(pack, "btFailed", "");
        }
      } else {
        String output = sp.run(new String[] {"hciconfig", cmac/*dev*/, "down"}, false);
        if (sp.getErrorLevel() == 0) {
          jbusClient.call(pack, "btSuccess", "");
        } else {
          jbusClient.call(pack, "btFailed", "");
        }
      }
    }
    //connect to end device thru controller
    public synchronized void connectBT(String pack, String cmac, final String mmac) {
      final ShellProcess sp = new ShellProcess();
      if (bluez3) {
        //use bluetoothctl
        sp.addListener(new ShellProcessListenerAdapter() {
          String output = "";
          boolean scanComplete = false;
          boolean connectComplete = false;
          public void shellProcessOutput(String string) {
            if (!connectComplete && output.indexOf("Pairing successful") != -1 && output.indexOf("Connection successful") != -1) {
              sp.addRegexResponse(bluetoothctlPrompt, "exit\n", false);
              connectComplete = true;
              //need to wake up ShellProcess to make it process prompt/response again
              try {
                sp.getOutputStream().write("version\n".getBytes());
                sp.getOutputStream().flush();
              } catch (Exception e) {}
              return;
            }
            if (scanComplete) return;
            output += string;
            if (output.indexOf(mmac) != -1) {
              sp.addRegexResponse(bluetoothctlPrompt, "trust " + mmac + "\n", false);
              sp.addRegexResponse(bluetoothctlPrompt, "pairable on\n", false);
              sp.addRegexResponse(bluetoothctlPrompt, "pair " + mmac + "\n", false);
              sp.addRegexResponse(bluetoothctlPrompt, "connect " + mmac + "\n", false);
              scanComplete = true;
              //need to wake up ShellProcess to make it process prompt/response again
              try {
                sp.getOutputStream().write("version\n".getBytes());
                sp.getOutputStream().flush();
              } catch (Exception e) {}
            }
          }
        });
        sp.addRegexResponse(bluetoothctlPrompt, "select " + cmac + "\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "scan on\n", false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          public void run() {
            try {
              sp.getOutputStream().write(("exit\n").getBytes());
              sp.getOutputStream().flush();
            } catch (Exception e) {}
          }
        }, 10 * 1000, 1000);
        String output = sp.run(new String [] {"bluetoothctl"}, false);
        timer.cancel();
//JFLog.log("connectBT.output=" + output);
        if (output.indexOf("Connection successful") != -1) {
          jbusClient.call(pack, "btSuccess", "");
        } else {
          jbusClient.call(pack, "btFailed", "");
        }
      } else {
        //use hcitool (not working yet) [use to use hidd --connect but removed in bluez5.x)
        String output = sp.run(new String[] {"hcitool", "cc", mmac}, false);
        if (sp.getErrorLevel() == 0) {
          jbusClient.call(pack, "btSuccess", "");
        } else {
          jbusClient.call(pack, "btFailed", "");
        }
      }
    }
    //disconnect from end device thru controller
    public void disconnectBT(String pack, String cmac, String mmac) {
      final ShellProcess sp = new ShellProcess();
      if (true) {
        //use bluetoothctl
        sp.addRegexResponse(bluetoothctlPrompt, "select " + cmac + "\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "remove " + mmac + "\n", false);
        sp.addRegexResponse(bluetoothctlPrompt, "exit\n", false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          public void run() {
            try {
              sp.getOutputStream().write(("exit\n").getBytes());
              sp.getOutputStream().flush();
            } catch (Exception e) {}
          }
        }, 5 * 1000, 1000);
        String output = sp.run(new String [] {"bluetoothctl"}, false);
        timer.cancel();
        //TODO : check output
        jbusClient.call(pack, "btSuccess", "");
      } else {
        String output = sp.run(new String[] {"hcitool", "dc", mmac}, false);
        if (sp.getErrorLevel() == 0) {
          jbusClient.call(pack, "btSuccess", "");
        } else {
          jbusClient.call(pack, "btFailed", "");
        }
      }
    }
  }
}
