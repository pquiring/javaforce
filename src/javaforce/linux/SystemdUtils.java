package javaforce.linux;

/** Tools for systemd
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;

public class SystemdUtils {
  public static void main(String[] args) {
    Linux.detectDistro();
    if (args.length == 0) {
      usage();
      return;
    }
    switch (args[0]) {
      case "network": network(); break;
      default: usage();
    }
  }
  private static void usage() {
    System.out.println("systemd-utils [cmd]");
    System.out.println("  cmds:");
    System.out.println("    network - setup systemd-networkd, resolved and remove legacy packages");
  }
  private static void network() {
    switch (Linux.distro) {
      case Debian: {
        switch (Linux.derived) {
          case Unknown:
            network_debian();
            return;
          case Ubuntu:
            //TODO : netplan
            break;
        }
        break;
      }
      case Fedora: {
        //TODO
        break;
      }
      case Arch: {
        //TODO
        break;
      }
    }
    JFLog.log("This distro not supported yet");
  }
  private static class NIC {
    public String name;
    public String method;  //dhcp or static
    public String address;
    public String netmask;
    public String gateway;
    /** Return address and netmask in CIDR notation. */
    public String getAddressCIDR() {
      Subnet4 subnet = new Subnet4(address, netmask);
      return subnet.toStringCIDR();
    }
  }
  private static void network_debian() {
    //remove old packages : apt remove ifupdown network-manager dhcpcd-base
    JF.exec(new String[] {"apt", "remove", "ifupdown", "network-manager", "dhcpd-base"});
    //convert /etc/interfaces
    try {
      File file = new File("/etc/network/interfaces");
      if (!file.exists()) {
        JFLog.log("setup already complete");
        return;
      }
      FileInputStream fis = new FileInputStream(file);
      String[] lns = new String(fis.readAllBytes()).split("\n");
      fis.close();
      /*
      iface lo inet loopback
      iface eno1 inet {static | dhcp}
        address 192.168.1.2
        netmask 255.255.255.0
        gateway 192.168.1.1
      */
      ArrayList<NIC> nics = new ArrayList<>();
      NIC nic = null;
      for(String ln : lns) {
        String[] fs = ln.trim().split(" ");
        if (fs.length == 0) continue;
        switch (fs[0]) {
          case "iface":
            String name = fs[1];
            String method = fs[3];  //static of dhcp
            if (!name.equals("lo")) {
              nic = new NIC();
              nics.add(nic);
              nic.name = name;
              nic.method = method;
            }
            break;
          case "address": nic.address = fs[1]; break;
          case "netmask": nic.netmask = fs[1]; break;
          case "gateway": nic.gateway = fs[1]; break;
        }
      }
      write_nics(nics);
      file.delete();
    } catch (Exception e) {
      JFLog.log(e);
    }
    network_resolved();
    JF.exec(new String[] {"apt", "install", "systemd-resolved"});
  }
  private static void write_nics(ArrayList<NIC> nics) {
    //write .network files to /usr/lib/systemd/network
    for(NIC nic : nics) {
      File file = new File("/usr/lib/systemd/network/50-" + nic.name + ".network");
      StringBuilder cfg = new StringBuilder();
      cfg.append("[Match]\n");
      cfg.append("Name=" + nic.name + "\n");
      cfg.append("[Network]\n");
      switch (nic.method) {
        case "dhcp":
          cfg.append("DHCP=ipv4\n");
          break;
        case "static":
          cfg.append("Address=" + nic.getAddressCIDR() + "\n");
          cfg.append("Gateway=" + nic.gateway + "\n");
          break;
      }
      byte[] data = cfg.toString().getBytes();
      try {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  private static void network_resolved() {
    //transfer DNS settings from /etc/resolv.conf -> /etc/systemd/resolved.conf
    /*
/etc/resolv.conf:
nameserver x.x.x.x
search [domain]
    */
    ArrayList<String> dnses = new ArrayList<>();
    ArrayList<String> domains = new ArrayList<>();
    try {
      File file = new File("/etc/resolv.conf");
      FileInputStream fis = new FileInputStream(file);
      String[] lns = new String(fis.readAllBytes()).split("\n");
      fis.close();
      for(String ln : lns) {
        ln = ln.trim();
        String[] fs = ln.split(" ");
        if (fs.length == 0) continue;
        switch (fs[0]) {
          case "nameserver":
            for(int i=1;i<fs.length;i++) {
              dnses.add(fs[i]);
            }
            break;
          case "search":
            for(int i=1;i<fs.length;i++) {
              domains.add(fs[i]);
            }
            break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    /*
    [Resolve]
    DNS=x.x.x.x
    Domains=[domains]
    */
    StringBuilder cfg = new StringBuilder();
    cfg.append("[Resolve]\n");
    {
      cfg.append("DNS=");
      boolean first = true;
      for(String dns : dnses) {
        if (first) first = false; else cfg.append(" ");
        cfg.append(dns);
      }
      cfg.append("\n");
    }
    {
      cfg.append("Domains=");
      boolean first = true;
      for(String domain : domains) {
        if (first) first = false; else cfg.append(" ");
        cfg.append(domain);
      }
      cfg.append("\n");
    }
    try {
      File file = new File("/etc/systemd/resolved.conf");
      if (file.exists()) {
        JFLog.log("systemd-resolved already setup");
        return;
      }
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(cfg.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
