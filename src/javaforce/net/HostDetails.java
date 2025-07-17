package javaforce.net;

/** Host Details
 *
 * @author pquiring
 */

public class HostDetails {
  public String domain;
  public String hostname;
  public String ip4, ip6;

  public HostDetails() {
  }

  public HostDetails(String hostname, String ip4) {
    this.hostname = hostname;
    this.ip4 = ip4;
  }

  public HostDetails(String domain, String hostname, String ip4) {
    this.domain = domain;
    this.hostname = hostname;
    this.ip4 = ip4;
  }

  public HostDetails(String domain, String hostname, String ip4, String ip6) {
    this.domain = domain;
    this.hostname = hostname;
    this.ip4 = ip4;
    this.ip6 = ip6;
  }

  public IP4 toIP4() {
    return new IP4(ip4);
  }

  public IP6 toIP6() {
    return new IP6(ip6);
  }
}
