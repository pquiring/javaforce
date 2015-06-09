package jnetworkmgr;

/** jNetwork Manager
 *
 * Created : May 3, 2012
 *
 * I've decided to replace NetworkManager with my own implementation.
 * NetworkManager is a well developed system but interfacing with it from Java is difficult.
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

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
  public ArrayList<DHCPClient> dhcpClients = new ArrayList<DHCPClient>();  //for active interfaces

  public static void main(String args[]) {
    //this is currently not used : unless jnetworkmgr becomes a seperate package
    JFLog.init("/var/log/jnetworkmgr.log", true);
    new Server().start();
  }

  public void start() {
    try {
      This = this;
      startup = true;
      jbusClient = new JBusClient("org.jflinux.jnetworkmgr", new JBusMethods());
      jbusClient.start();
      loadConfig();
      Interface iface = getInterface("static");  //psuedo-interface for static values
      iface.domain_name = config.domain;
      iface.domain_name_servers = config.dns1 + " " + config.dns2 + " " + config.dns3;
      doDNS(iface);
      listIFs();
      for(int a=0;a<interfaceList.size();a++) {
        startIF(interfaceList.get(a).dev);
      }
      createWAPTimer();
      new NetLinkMonitor().start();
      startup = false;
    } catch (Exception e) {
      JFLog.log(e);
    }
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
  private String configFolder = "/etc/jconfig.d/";
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
    JFLog.log("jnetworkmgr:Starting:" + dev);
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"ifconfig", dev, "up"}, true);
    if (sp.getErrorLevel() != 0) {
      JFLog.log("jnetworkmgr:Failed to start interface:" + dev + ":Output=" + output);
      return;
    }
    Interface iface = getInterface(dev);
    iface.active = true;
    updateLink(iface);
    if (!iface.wireless) {  //do not setup wireless interfaces now
      setupInterface(iface);
    }
  }

  public void setupInterface(Interface iface) {
    //ensure interface link is active
    if (!iface.link) {
      JFLog.log("Warning:jnetworkmgr:setting up interface but link is down:" + iface.dev);
    }
    ShellProcess sp = new ShellProcess();
    if (iface.dhcp4) {
      DHCPClient client = new DHCPClient(iface, true);
      client.start();
      dhcpClients.add(client);
    } else {
      //set static IP4
      sp.run(new String[] {"ifconfig", iface.dev, iface.ip4, "netmask", iface.mask4}, true);
      iface.routers = iface.gateway4;
      iface.domain_name = config.domain;
      iface.domain_name_servers = config.dns1 + " " + config.dns2 + " " + config.dns3;
      doRouting(iface);
      doDNS(iface);
      if (pendingWAP != null && iface.pack != null) {
        This.jbusClient.call(iface.pack, "wapSuccess", "");
        pendingWAP = null;
        iface.pack = null;
      }
    }
    if (iface.dhcp6) {
      DHCPClient client = new DHCPClient(iface, false);
      client.start();
      dhcpClients.add(client);
    } else {
      if (!iface.disableIP6) {
        //set static IP6
        sp.run(new String[] {"ifconfig", iface.dev, "add", iface.ip6 + "/64"}, true);
        //TODO : IP6???
//        doRouting6(iface);
//        doDNS6(iface);
      }
    }
    iface.setup = true;
  }

  private void stopIF(String dev) {
    JFLog.log("jnetworkmgr:Stoping:" + dev);
    Interface iface = getInterface(dev);
    ShellProcess sp = new ShellProcess();
    sp.run(new String[] {"ifconfig", dev, "0.0.0.0"}, true);  //remove IP first
    String output = sp.run(new String[] {"ifconfig", dev, "down"}, true);
    if (sp.getErrorLevel() != 0) {
      JFLog.log("jnetworkmgr:Failed to stop interface:" + dev + ":Output=" + output);
    }
    tearDownInterface(dev);
    iface.active = false;
  }

  public void tearDownInterface(String dev) {
    Interface iface = getInterface(dev);
    for(int a=0;a<dhcpClients.size();) {
      if (dhcpClients.get(a).getDevice().equals(dev)) {
        dhcpClients.get(a).close();
        dhcpClients.remove(a);
      } else {
        a++;
      }
    }
    undoRouting(iface, true);
    undoDNS(iface, true);
    //remove from priority chain
    if (iface.higherRoute != null) {
      iface.higherRoute.lowerRoute = iface.lowerRoute;
    }
    if (iface.lowerRoute != null) {
      iface.lowerRoute.higherRoute = iface.higherRoute;
    }
    if (iface.higherDNS != null) {
      iface.higherDNS.lowerDNS = iface.lowerDNS;
    }
    if (iface.lowerDNS != null) {
      iface.lowerDNS.higherDNS = iface.higherDNS;
    }
    iface.setup = false;
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

  /** Configures routing table */
  public void doRouting(Interface iface) {
    if (highestRoute == null) {
      highestRoute = iface;
    } else {
      if (iface.higherRoute != null) {
        //is not the highest route
        return;
      }
      if ((iface.lowerRoute == null) && (highestRoute != iface)) {
        highestRoute.higherRoute = iface;
        iface.lowerRoute = highestRoute;
        highestRoute = iface;
      }
    }
    String routers[] = iface.routers.split(" ");
    exec(new String[] {"route", "add", "-net", "0.0.0.0", "gw", routers[0]});
    if (iface.static_routes != null) {
      String staticRoutes[] = iface.static_routes.split(" ");
      //TODO : do static routes???
    }
  }

  /** Configures /etc/resolv.conf */
  public void doDNS(Interface iface) {
//    JFLog.log("doDNS:" + iface.dev + ":highest=" + (highestDNS == null ? "null" : highestDNS.dev) + ":lowerDNS=" + (iface.lowerDNS == null ? "null" : iface.lowerDNS.dev));
    if (highestDNS == null) {
      highestDNS = iface;
    } else {
      if (iface.higherDNS != null) {
        JFLog.log("doDNS:Not highest DNS");
        return;
      }
      if ((iface.lowerDNS == null) && (highestDNS != iface)) {
        highestDNS.higherDNS = iface;
        iface.lowerDNS = highestDNS;
        highestDNS = iface;
      }
    }
//    JFLog.log("Applying DNS:" + iface.dev + ":" + iface.domain_name_servers);
    try {
      String dns[] = iface.domain_name_servers.split(" ");
      FileOutputStream fos = new FileOutputStream("/etc/resolv.conf");
      fos.write(("domain " + iface.domain_name + "\n").getBytes());
      fos.write(("search " + iface.domain_name + "\n").getBytes());
      for(int a=0;a<dns.length;a++) {
        if (dns[a].length() == 0) continue;
        fos.write(("nameserver " + dns[a] + "\n").getBytes());
      }
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Unconfigure routing table */
  private void undoRouting(Interface iface, boolean useLower) {
    if (highestRoute != iface) return;
    String routers[] = iface.routers.split(" ");
    exec(new String[] {"route", "del", "-net", "0.0.0.0"});
//    String staticRoutes[] = iface.static_routes.split(" ");
    //TODO : undo static routes???
    //find next interface with lower priority and switch to it's routing
    if (useLower) {
      if (iface.lowerRoute != null) {
        //remove this from chain - never go back up after lowering
        iface.lowerRoute.higherRoute = null;
        highestRoute = iface.lowerRoute;
        doRouting(iface.lowerRoute);
      }
    }
  }

  private void removeDNS() {
    //no interface available
    try {
      FileOutputStream fos = new FileOutputStream("/etc/resolv.conf");
      fos.write("#no interface currently available".getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Unconfigure /etc/resolv.conf */
  public void undoDNS(Interface iface, boolean useLower) {
//    JFLog.log("undoDNS:" + iface.dev + ":highest=" + (highestDNS == null ? "null" : highestDNS.dev) + ":lowerDNS=" + (iface.lowerDNS == null ? "null" : iface.lowerDNS.dev));
    if (highestDNS != iface) return;
    if (useLower) {
      if (iface.lowerDNS != null) {
        //remove this from chain - never go back up after lowering
        iface.lowerDNS.higherDNS = null;
        highestDNS = iface.lowerDNS;
        doDNS(iface.lowerDNS);
      } else {
        removeDNS();
      }
    } else {
//      removeDNS();  //not needed since this change is temporary
    }
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
    JFLog.log("jnetworkmgr.script:" + reason + " on " + ifaceName);
    Interface iface = getInterface(ifaceName);
    if (reason.equals("MEDIUM")) {
      //ifconfig $interface medium $medium
      exec(new String[] {"ifconfig", ifaceName, "medium", medium});
      return;
    }
    if (reason.equals("EXPIRE") || reason.equals("FAIL")) {
      undoRouting(iface, true);
      undoDNS(iface, true);
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
      //delete anything that has changed new_ -> old_
      if ((!old_routers.equals(new_routers) && (old_routers.length() > 0))
        || ((!old_static_routes.equals(new_static_routes)) && (old_static_routes.length() > 0))) {
          undoRouting(iface, false);
      }
      if (!old_domain_name_servers.equals(new_domain_name_servers)) {
        undoDNS(iface, false);
      }
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
      doRouting(iface);
      doDNS(iface);
      return;
    }
    //ignored : STOP, RELEASE, NBI, TIMEOUT
  }

  private void getWAPList() {
    String newWapList = "";
    ShellProcess sp = new ShellProcess();
    for(int a=0;a<interfaceList.size();a++) {
      Interface iface = interfaceList.get(a);
      if (!iface.wireless) continue;
      String output = sp.run(new String[] {"iwlist", iface.dev, "scan"}, false);
      if (output == null) output = "";
      output = output.replaceAll("\n", "|");
      output = output.replaceAll("\"", "\'");
      String wapScan = output;
      newWapList += genWAPList(iface.dev, wapScan);
    }
    wapList = newWapList;
    jbusClient.call("org.jflinux.jsystemmgr", "broadcastWAPList", quote(wapList));
  }

  private String genWAPList(String dev, String wapScan) {
    int cnt = 0;
    String list = "";
    String lns[] = wapScan.split("[|]");
    String wap = null, encType = "?";
    for(int a=0;a<lns.length;a++) {
      String ln = lns[a].trim();
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
  private String vpnConfigFile = "/etc/jconfig.d/vpn.xml";

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

  private boolean isVPNactive(String name) {
    for(int a=0;a<vpnConnections.size();a++) {
      if (vpnConnections.get(a).name.equals(name)) return true;
    }
    return false;
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
    public void connectVPN(String pack, String id, String host, String user, String pass, String domain,
      String caps, String capsOpts, String routes, String routeOpts, String domainsearch) {

      VPNConnection conn = new VPNConnection();
      conn.id = id;
      conn.pack = pack;
      conn.host = host;
      conn.user = user;
      conn.pass = pass;
      conn.domain = domain;
      conn.caps = caps;
      conn.capsOpts = capsOpts;
      conn.routes = routes;
      conn.routeOpts = routeOpts;
      conn.domainsearch = domainsearch;
      vpnConnections.add(conn);
      conn.start();
    }
    public void disconnectVPN(String id) {
      for(int a=0;a<vpnConnections.size();a++) {
        if (vpnConnections.get(a).id.equals(id)) {
          vpnConnections.get(a).close();
          vpnConnections.remove(a);
          return;
        }
      }
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
      tearDownInterface(dev);  //stop dhcp clients (if any)
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
      if (true) {
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
      if (true) {
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
      if (true) {
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
      if (true) {
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
    public void getVPNList(String pack) {
      String vpnList = "";
      loadVPNConfig();
      for(int a=0;a<vpnConfig.vpn.length;a++) {
        String name = vpnConfig.vpn[a].name;
        if (isVPNactive(name)) name += " *";
        vpnList += name + "|";
      }
      jbusClient.call(pack, "setVPNList", quote(vpnList));
    }
    public void connectVPN(String pack, String name) {
      for(int a=0;a<vpnConfig.vpn.length;a++) {
        if (vpnConfig.vpn[a].name.equals(name)) {
          if (isVPNactive(name)) {
            disconnectVPN(name);
          } else {
            VPN vpn = vpnConfig.vpn[a];
            VPNConnection conn = new VPNConnection();
            conn.name = name;
            conn.id = "" + Math.abs(new Random().nextInt());
            conn.pack = pack;
            conn.host = vpn.host;
            conn.user = vpn.user;
            conn.pass = vpn.pass;
            conn.domain = vpn.domain;
            conn.caps = vpn.caps;
            conn.capsOpts = vpn.capsOpts;
            conn.routes = vpn.routes;
            conn.routeOpts = vpn.routeOpts;
            conn.domainsearch = vpn.domainsearch;
            vpnConnections.add(conn);
            conn.start();
          }
          break;
        }
      }
    }
    public void cancelVPN() {
      if (pendingVPN == null) return;
      pendingVPN.close();
      pendingVPN = null;
    }
    public void closeAllVPN() {
      while (vpnConnections.size() > 0) {
        disconnectVPN(vpnConnections.get(0).name);
      }
    }
  }
}
