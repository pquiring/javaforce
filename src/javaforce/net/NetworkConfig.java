package javaforce.net;

/** Network Interface Settings
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class NetworkConfig {
  public String dev;

  public boolean dhcp4 = true;
  public String ip4;
  public String mask4;
  public String gateway4;

  public boolean enable_ip6 = false;

  public boolean dhcp6 = true;
  public String ip6;
  public String gateway6;

  public String[] dns;   //optional

/*
  public boolean wireless;
  public boolean active;  // ifup / ifdown
  public boolean link;  // /sys/class/net/{dev}/carrier
*/

  public NetworkConfig() {}

  public NetworkConfig(String dev) {
    this.dev = dev;
  }

  /** Create NetworkConfig from networkd .network file. */
  public static NetworkConfig fromNetworkd(String[] cfg) {
    NetworkConfig nc = new NetworkConfig();
    ArrayList<String> dns = new ArrayList<>();
    for(String ln : cfg) {
      int idx = ln.indexOf('=');
      if (idx == -1) continue;
      String key = ln.substring(0, idx).trim().toLowerCase();
      String value = ln.substring(idx + 1).trim();
      switch (key) {
        case "name": nc.dev = value; break;
        case "address":
          if (value.contains(".")) {
            nc.ip4 = value;
            nc.dhcp4 = false;
          } else {
            nc.ip6 = value;
            nc.dhcp6 = false;
          }
          break;
        case "gateway":
          if (value.contains("."))
            nc.gateway4 = value;
          else
            nc.gateway6 = value; break;
        case "dns":
          dns.add(value);
          break;
      }
    }
    nc.dns = dns.toArray(JF.StringArrayType);
    return nc;
  }

  /** Create networkd .network file from NetworkConfig */
  public String[] toNetworkd() {
    ArrayList<String> cfg = new ArrayList<>();
    cfg.add("[Match]");
    cfg.add("Name=" + dev);
    cfg.add("[Network]");
    if (dhcp4) {
      cfg.add("DHCP=yes");
    } else {
      cfg.add("Address=" + ip4);
      cfg.add("Gateway=" + gateway4);
    }
    if (!enable_ip6) {
      if (dhcp6) {
        cfg.add("DHCP=yes");
      } else {
        cfg.add("Address=" + ip6);
        cfg.add("Gateway=" + gateway6);
      }
    }
    if (dns != null) {
      for(String svr : dns) {
        cfg.add("DNS=" + svr);
      }
    }
    return cfg.toArray(JF.StringArrayType);
  }

  public void addDNS(String svr) {
    if (dns == null) {
      dns = new String[1];
      dns[0] = svr;
    } else {
      String[] newdns = new String[dns.length + 1];
      System.arraycopy(dns, 0, newdns, 0, dns.length);
      dns = newdns;
      dns[dns.length - 1] = svr;
    }
  }
}
