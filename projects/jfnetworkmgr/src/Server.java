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
import javaforce.linux.*;

public class Server {
  private boolean startup;
  private String wapList = "";
  private Timer wapTimer;

  public VPNConnection pendingVPN;
  public WAPConnection pendingWAP;

  public static Server This;
  public JBusClient jbusClient;
  public ArrayList<VPNConnection> vpnConnections = new ArrayList<VPNConnection>();
  public ArrayList<WAPConnection> wapConnections = new ArrayList<WAPConnection>();
  public ArrayList<Interface> interfaceList = new ArrayList<Interface>();  //active interfaces

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
      startup = true;
      jbusClient = new JBusClient("org.jflinux.jfnetworkmgr", new JBusMethods());
      jbusClient.start();
      loadConfig();
      Interface iface = getInterface("static");  //psuedo-interface for static values
      iface.domain_name = config.domain;
      iface.domain_name_servers = config.dns1 + " " + config.dns2 + " " + config.dns3;
      listIFs();
      for(int a=0;a<interfaceList.size();a++) {
        startIF(interfaceList.get(a).dev);
      }
      createWAPTimer();
      startup = false;
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

  private void updateLink(Interface iface) {
    try {
      FileInputStream fis = new FileInputStream("/sys/class/net/" + iface.dev + "/carrier");
      char carrier = (char)fis.read();
      fis.close();
      iface.link = (carrier == '1');
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void listIFs() {
    ShellProcess sp = new ShellProcess();
    String ifconfig = sp.run(new String[] {"ifconfig", "-a"}, false);
    String lns[] = ifconfig.split("\n");
    String iwconfig = sp.run(new String[] {"iwconfig"}, false);
    String wlns[] = iwconfig.split("\n");
    for(int a=0;a<lns.length;a++) {
      if (!lns[a].startsWith(" ")) {
        int idx = lns[a].indexOf(" ");
        if (idx == -1) continue;
        String dev = lns[a].substring(0, idx);
        idx = dev.indexOf(":");  //fedora has a ':' after name
        if (idx != -1) {
          dev = dev.substring(0, idx);
        }
        if (dev.equals("lo")) continue;
        Interface iface = getInterface(dev);
        for(int w=0;w<wlns.length;w++) {
          if (wlns[w].startsWith(dev)) {
            if (wlns[w].indexOf("ESSID") != -1) {
              iface.wireless = true;
            }
            break;
          }
        }
        interfaceList.add(iface);
        JFLog.log("interface:" + iface.dev + ":wireless=" + iface.wireless);
      }
    }
  }

  public static class Config {
    public Interface iface[];
    public String dns1, dns2, dns3;
    public String hostname, domain;
  }

  private Config config;
  private String configFolder = "/etc/jfconfig.d/";
  private String configFile = "network.xml";
  private Interface highestRoute = null;  //interface with highest priority
  private Interface highestDNS = null;  //interface with highest priority

  private void loadConfig() {
    defaultConfig();
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(configFolder + configFile);
      xml.read(fis);
      xml.writeClass(config);
      fis.close();
    } catch (FileNotFoundException e1) {
      defaultConfig();
    } catch (Exception e2) {
      JFLog.log(e2);
      defaultConfig();
    }
  }

  private void defaultConfig() {
    config = new Config();
    config.iface = new Interface[0];
    config.hostname = "localhost";
    config.domain = "localdomain";
    config.dns1 = "";
    config.dns2 = "";
    config.dns3 = "";
  }

  public void updateInterface(Interface iface) {
    //update config options
    Interface update = null;
    for(int a=0;a<config.iface.length;a++) {
      Interface i = config.iface[a];
      if (i.dev.equals(iface.dev)) {
        update = i;
        break;
      }
    }
    if (update == null) return;
    iface.dhcp4 = update.dhcp4;
    iface.dhcp6 = update.dhcp6;
    iface.disableIP6 = update.disableIP6;
    iface.ip4 = update.ip4;
    iface.mask4 = update.mask4;
    iface.gateway4 = update.gateway4;
    iface.ip6 = update.ip6;
    iface.gateway6 = update.gateway6;
  }

  public Interface getInterface(String dev) {
    Interface iface;
    loadConfig();  //redo in case of changes
    //try interfaceList first
    for(int a=0;a<interfaceList.size();a++) {
      iface = interfaceList.get(a);
      if (iface.dev.equals(dev)) {
        updateInterface(iface);
        return iface;
      }
    }
    //try config next
    for(int a=0;a<config.iface.length;a++) {
      iface = config.iface[a];
      if (iface.dev.equals(dev)) {
        return iface;
      }
    }
    //return a new empty instance of Interface
    iface = new Interface();
    iface.dev = dev;
    return iface;
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
    Interface iface = getInterface(dev);
    iface.active = true;
    updateLink(iface);
  }

  private void stopIF(String dev) {
    JFLog.log("jfnetworkmgr:Stoping:" + dev);
    Interface iface = getInterface(dev);
    if (!NetworkControl.down(dev)) {
      JFLog.log("jfnetworkmgr:Failed to stop interface:" + dev);
    }
    iface.active = false;
  }

  private boolean isIFactive(String dev) {
    for(int a=0;a<interfaceList.size();a++) {
      Interface iface = interfaceList.get(a);
      if (iface.dev.equals(dev)) {
        return iface.active;
      }
    }
    return false;
  }

  public static void dhcpSuccess(Interface iface) {
    if (Server.This.pendingWAP != null && iface.pack != null) {
      This.jbusClient.call(iface.pack, "wapSuccess", "");
      Server.This.pendingWAP = null;
      iface.pack = null;
    }
  }

  private void exec(String args[]) {
    try { Runtime.getRuntime().exec(args); } catch (Exception e) {JFLog.log(e);}
  }

  private void processScript(String argString) {
    String args[] = argString.split(",");
    String reason = null, ifaceName = null, medium = null;
    String new_ip_address = null, new_subnet_mask = null, new_domain_name = null, new_domain_name_servers = null, new_routers = null, new_static_routes = null;
    String old_ip_address = null, old_subnet_mask = null, old_domain_name = null, old_domain_name_servers = null, old_routers = null, old_static_routes = null;
    for(int a=0;a<args.length;a++) {
      if (args[a].startsWith("reason=")) {reason = args[a].substring(7); continue;}
      if (args[a].startsWith("interface=")) {ifaceName = args[a].substring(10); continue;}
      if (args[a].startsWith("medium=")) {medium = args[a].substring(7); continue;}

      if (args[a].startsWith("new_ip_address=")) {new_ip_address = args[a].substring(15); continue;}
      if (args[a].startsWith("new_subnet_mask=")) {new_subnet_mask = args[a].substring(16); continue;}
      if (args[a].startsWith("new_domain_name=")) {new_domain_name = args[a].substring(16); continue;}
      if (args[a].startsWith("new_domain_name_servers=")) {new_domain_name_servers = args[a].substring(24); continue;}
      if (args[a].startsWith("new_routers=")) {new_routers = args[a].substring(12); continue;}
      if (args[a].startsWith("new_static_routes=")) {new_static_routes = args[a].substring(18); continue;}

      if (args[a].startsWith("old_ip_address=")) {old_ip_address = args[a].substring(15); continue;}
      if (args[a].startsWith("old_subnet_mask=")) {old_subnet_mask = args[a].substring(16); continue;}
      if (args[a].startsWith("old_domain_name=")) {old_domain_name = args[a].substring(16); continue;}
      if (args[a].startsWith("old_domain_name_servers=")) {old_domain_name_servers = args[a].substring(24); continue;}
      if (args[a].startsWith("old_routers=")) {old_routers = args[a].substring(12); continue;}
      if (args[a].startsWith("old_static_routes=")) {old_static_routes = args[a].substring(18); continue;}
    }
    JFLog.log("jfnetworkmgr.script:" + reason + " on " + ifaceName);
    Interface iface = getInterface(ifaceName);
    if (reason.equals("MEDIUM")) {
      //ifconfig $interface medium $medium
      exec(new String[] {"ifconfig", ifaceName, "medium", medium});
      return;
    }
    if (reason.equals("EXPIRE") || reason.equals("FAIL")) {
      reason = "PREINIT";
    }
    if (reason.equals("PREINIT")) {
      //ifconfig $interface 0.0.0.0
      exec(new String[] {"ifconfig", ifaceName, "0.0.0.0"});
      //ifconfig $interface broadcast
      exec(new String[] {"ifconfig", ifaceName, "broadcast"});
      return;
    }
    if (reason.equals("REBIND")) {
      //if IP has changed clear ARP table
      if (!old_ip_address.equals(new_ip_address)) {
        exec(new String[] {"ip", "neigh", "flush", "dev", ifaceName});
      }
      reason = "RENEW";
    }
    if (reason.equals("RENEW")) {
      reason = "BOUND";
    }
    if (reason.equals("BOUND") || reason.equals("REBOOT")) {
      //ifconfig $interface $new_ip_address
      exec(new String[] {"ifconfig", ifaceName, new_ip_address});
      iface.ip4 = new_ip_address;
      //ifconfig $interface netmask $new_subnet_mask
      exec(new String[] {"ifconfig", ifaceName, new_subnet_mask});
      iface.mask4 = new_subnet_mask;
      iface.domain_name = new_domain_name;
      iface.domain_name_servers = new_domain_name_servers;
      iface.routers = new_routers;
      iface.static_routes = new_static_routes;
      return;
    }
    //ignored : STOP, RELEASE, NBI, TIMEOUT
  }

  private void getWAPList() {
    String newWapList = "";
    for(int a=0;a<interfaceList.size();a++) {
      Interface iface = interfaceList.get(a);
      if (!iface.wireless) continue;
      String[] output = NetworkControl.wifi_scan(iface.dev);
      newWapList += genWAPList(iface.dev, output);
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
    for(int a=0;a<interfaceList.size();a++) {
      if (interfaceList.get(a).wireless) return true;
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
    public void notifyUp(String dev) {
      if (startup) return;
      //TODO : start dhcp client ???
    }
    public void notifyDown(String dev) {
      if (startup) return;
      //TODO : stop dhcp client ???
    }
    public void script(String args) {
      processScript(args);
    }
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
