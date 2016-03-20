

/**
 * Created : Apr 30, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import javaforce.*;

public class VPNConnection extends Thread implements ShellProcessListener {
  public String name;  //friendly name (not username)
  public String pack, id;
  public String host, user, domain, pass;
  public String caps, capsOpts, routes, routeOpts;
  public String domainsearch;

  private ShellProcess sp;
  private String fcaps[], fcapsOpts[], froutes[], frouteOpts[];
  private boolean connected, failed, routingApplied, DNSApplied;

  private String local_ip, remote_ip, dns;
  private File passwdFile;

  private boolean contains(String array[], String value) {
    for(int a=0;a<array.length;a++) {
      if (array[a].equals(value)) return true;
    }
    return false;
  }
  private boolean startsWith(String array[], String value) {
    for(int a=0;a<array.length;a++) {
      if (array[a].startsWith(value)) return true;
    }
    return false;
  }
  private String get(String array[], String value) {
    for(int a=0;a<array.length;a++) {
      if (array[a].startsWith(value)) return array[a].substring(value.length());
    }
    return null;
  }
  private String quote(String in) {
    return "\"" + in + "\"";
  }
  public void run() {
    passwdFile = new File("/etc/ppp/peers/vpn"+id);
    try {
      fcaps = caps.split(",");
      fcapsOpts = capsOpts.split(",");
      froutes = routes.split(";");  //split again with ","
      frouteOpts = routeOpts.split(",");
      FileOutputStream fos = new FileOutputStream(passwdFile);
      Runtime.getRuntime().exec(new String[] {"chmod","600",passwdFile.getAbsolutePath()});  //only root can read
      fos.write(("password " + pass).getBytes());
      fos.close();
      ArrayList<String> cmd = new ArrayList<String>();
      cmd.add("pppd");
      cmd.add("call");
      cmd.add("vpn" + id);
      cmd.add("pty");
      cmd.add("pptp " + host + " --nolaunchpppd --loglevel 0 --logstring jflinux-" + id);
//      cmd.add("ipparam");
//      cmd.add("jflinux-" + id);
      cmd.add("nodetach");
      cmd.add("lock");
      cmd.add("usepeerdns");
      cmd.add("noipdefault");  //get local IP from server
      cmd.add("nodefaultroute");
      cmd.add("noauth");
      cmd.add("user");
      if (domain == null) domain = "";
      cmd.add((domain.length() > 0 ? domain + "\\\\" : "") + user);
      if (contains(fcaps, "windows")) {
        //setup windows caps
        fcaps = new String[] {"chap", "mschap", "mschapv2"};
        fcapsOpts = new String[] {"mppe-all", "mppe-stateful", "echo", "bsd", "deflate", "tcp"};
      }
      if (!contains(fcaps, "eap")) {
        cmd.add("refuse-eap");
      }
      if (!contains(fcaps, "pap")) {
        cmd.add("refuse-pap");
      }
      if (!contains(fcaps, "chap")) {
        cmd.add("refuse-chap");
      }
      if (!contains(fcaps, "mschap")) {
        cmd.add("refuse-mschap");
      }
      if (!contains(fcaps, "mschapv2")) {
        cmd.add("refuse-mschap-v2");
      }
      boolean mppe = false;
      if (contains(fcapsOpts, "mppe-all")) {
        cmd.add("require-mppe");
        mppe = true;
      }
      if (contains(fcapsOpts, "mppe-40")) {
        cmd.add("require-mppe-40");
        mppe = true;
      }
      if (contains(fcapsOpts, "mppe-128")) {
        cmd.add("require-mppe-128");
        mppe = true;
      }
      if ((mppe) && (contains(fcapsOpts, "mppe-stateful"))) {
        cmd.add("mppe-stateful");
      }
      if (contains(fcapsOpts, "echo")) {
        cmd.add("lcp-echo-failure");
        cmd.add("0");
        cmd.add("lcp-echo-interval");
        cmd.add("0");
      }
      if (!contains(fcapsOpts, "bsd")) {
        cmd.add("nobsdcomp");
      }
      if (!contains(fcapsOpts, "deflate")) {
        cmd.add("nodeflate");
      }
      if (!contains(fcapsOpts, "tcp")) {
        cmd.add("novj");
      }
      sp = new ShellProcess();
      sp.addListener(this);
      sp.keepOutput(false);
//test
//String tmp = "";
//for(int a=0;a<cmd.size();a++) tmp += cmd.get(a) + " ";
//JFLog.log("Execute:" + tmp);
//test
      sp.run(cmd.toArray(new String[0]), true);
    } catch (Exception e) {
      Server.This.jbusClient.call(pack, "vpnFailed", quote(id));
      Server.This.pendingVPN = null;
      JFLog.log(e);
    }
  }

  public void close() {
    passwdFile.delete();
    sp.destroy();
    if (routingApplied) {
      delRouting();
    }
    JF.sleep(500);  //this fixes a bug where DNS fails to failback to lower interface
    //something else is writting to /etc/resolv.conf
    if (DNSApplied) {
      delDNS();
    }
  }

/*
Sample output:
failure:
MS-CHAP authentication failed: E=691 Authentication failure
CHAP authentication failed
Connection terminated.

success:
CHAP authentication succeeded
MPPE 128-bit stateless compression enabled
local  IP address 192.168.0.205
remote IP address 192.168.0.200
primary   DNS address 192.168.0.3
*/

  public void shellProcessOutput(String string) {
    JFLog.log(string);  //test
    if (failed) return;
    String lns[] = string.split("\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].indexOf("failed") != -1) {
        passwdFile.delete();
        failed = true;
        Server.This.jbusClient.call(pack, "vpnFailed", quote(id));
        Server.This.pendingVPN = null;
      }
      if (lns[a].indexOf("succeeded") != -1) {
        passwdFile.delete();
        connected = true;
        Server.This.jbusClient.call(pack, "vpnSuccess", quote(id));
        Server.This.pendingVPN = null;
      }
      if (lns[a].startsWith("local  IP address ")) {
        local_ip = lns[a].substring(18);
      }
      if (lns[a].startsWith("remote IP address ")) {
        remote_ip = lns[a].substring(18);
      }
      if (lns[a].startsWith("primary   DNS address ")) {
        dns = lns[a].substring(22);
      }
      if ((!routingApplied) && (local_ip != null) && (remote_ip != null) && (dns != null)) {
        applyRouting();
        routingApplied = true;
        applyDNS();
        DNSApplied = true;
      }
    }
  }

  private void applyRouting() {
    ShellProcess sp = new ShellProcess();
    String output;
    try {
      if (startsWith(frouteOpts, "network=")) {
        String netmask = get(frouteOpts, "network=");
        output = sp.run(new String[] {
          "route", "add", "-net", Server.mask(remote_ip, netmask), "netmask", netmask, "gw", local_ip
        },true);
        if (sp.getErrorLevel() != 0) {
          JFLog.log("VPN:Route Add Failed:" + sp.command);
          JFLog.log(output);
        }
      }
      for(int a=0;a<froutes.length;a++) {
        String rf[] = froutes[a].split(",");
        if (rf.length != 4) continue;
        //address netmask gateway metric
        if (rf[0].length() == 0) continue;
        if (rf[1].length() == 0) continue;
        if (rf[2].length() == 0) continue;
        if (rf[3].length() == 0) rf[3] = "1";
        output = sp.run(new String[] {
          "route", "add", "-net", rf[0], "netmask", rf[1], "gw", rf[2], "metric", rf[3]
        }, true);
        if (sp.getErrorLevel() != 0) {
          JFLog.log("VPN:Route Add Failed:" + sp.command);
          JFLog.log(output);
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void delRouting() {
    ShellProcess sp = new ShellProcess();
    String output;
    try {
      if (startsWith(frouteOpts, "network=")) {
        String netmask = get(frouteOpts, "network=");
        output = sp.run(new String[] {
          "route", "del", Server.mask(remote_ip, netmask)
        }, true);
        if (sp.getErrorLevel() != 0) {
          JFLog.log("VPN:Route Delete Failed:" + sp.command);
          JFLog.log(output);
        }
      }
      for(int a=0;a<froutes.length;a++) {
        String rf[] = froutes[a].split(",");
        if (rf.length != 4) continue;
        //address netmask gateway metric
        if (rf[0].length() == 0) continue;
        if (rf[1].length() == 0) continue;
        if (rf[2].length() == 0) continue;
        if (rf[3].length() == 0) rf[3] = "1";
        output = sp.run(new String[] {
          "route", "del", rf[0]
        }, true);
        if (sp.getErrorLevel() != 0) {
          JFLog.log("VPN:Route Delete Failed:" + sp.command);
          JFLog.log(output);
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  Interface iface = new Interface();

  private void applyDNS() {
    iface.dev = "vpn" + Server.This.vpnConnections.size();
    if (domainsearch.length() > 0) {
      if (!domainsearch.endsWith(".")) domainsearch += ".";
    } else {
      domainsearch = "localdomain.";
    }
    iface.domain_name = domainsearch;
    iface.domain_name_servers = dns;
    Server.This.doDNS(iface);
  }

  private void delDNS() {
    Server.This.undoDNS(iface, true);
  }
}
