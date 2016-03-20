

/**
 *
 * @author pquiring
 */
public class Interface {
  public String dev;
  public boolean dhcp4 = true;
  public boolean dhcp6 = false;
  public boolean disableIP6 = true;
  public String ip4;
  public String mask4;
  public String gateway4;
  public String ip6;
  public String gateway6;
  //extra non-config members
  public boolean wireless;
  public boolean active;  // ifup / ifdown
  public boolean link;  // /sys/class/net/*/carrier
  public boolean setup;
  public String pack;
  public String domain_name;
  public String domain_name_servers;
  public String routers;
  public String static_routes;
  Interface higherRoute;
  Interface lowerRoute;
  Interface higherDNS;
  Interface lowerDNS;
}
