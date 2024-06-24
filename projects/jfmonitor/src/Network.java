/** Network
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;

public class Network implements Serializable {
  public static final long serialVersionUID = 1;

  public String host;  //localhost or remote client ip
  public String ip_nic;  //network interface IP
  public String ip_first;
  public String ip_last;
  public String ip_dhcp_first;
  public String ip_dhcp_last;
  public String netmask;  //reserved
  public String desc;
  public ArrayList<IP> ips;

  //transient data
  public transient boolean first;  //ignore first notify event

  public void init() {
    ips = new ArrayList<IP>();
    byte[] _ip_first = PacketCapture.decode_ip(ip_first);
    byte[] _ip_last = PacketCapture.decode_ip(ip_last);
    int length = PacketCapture.get_ip_range_length(_ip_first, _ip_last);
    if (length == -1) return;
    PacketCapture.increment_ip(_ip_last);
    while (!PacketCapture.compare_ip(_ip_first, _ip_last)) {
      IP ip = new IP();
      ip.host = PacketCapture.build_ip(_ip_first);
      ip.mac = "null";
      ips.add(ip);
      PacketCapture.increment_ip(_ip_first);
    }
    first = true;
  }

  public void validate() {
    if (ips == null) {
      ips = new ArrayList<IP>();
    }
    byte[] _ip_first = PacketCapture.decode_ip(ip_first);
    byte[] _ip_last = PacketCapture.decode_ip(ip_last);
    int length = PacketCapture.get_ip_range_length(_ip_first, _ip_last);
    if (ips.size() == length) return;
    //TODO : rebuild devices (copy over what is still in valid range)
    //for now just rebuild losing any notify bits
    init();
    first = true;
  }

  /** Returns unique ID for this network scan. */
  public String getID() {
    return ip_nic + "_" + ip_first + "_" + ip_last;
  }

  public void update(byte[] map) {
    int pos = 0;
    for(IP ip : ips) {
      boolean was_online = ip.online;
      ip.online = map[pos++] == 1;
      if (ip.online) {
        ip.mac = String.format("%02x%02x%02x%02x%02x%02x", map[pos] & 0xff, map[pos+1] & 0xff, map[pos+2] & 0xff, map[pos+3] & 0xff, map[pos+4] & 0xff, map[pos+5] & 0xff);
        pos += 6;
        //check if unknown device
        if (Config.current.notify_unknown_device) {
          Device dev = Config.current.getDevice(ip.mac);
          if (dev == null) {
            Notify.add_unknown_device(ip.mac, desc);
          }
        }
      }
      if (Config.debug) {
        JFLog.log("update:" + ip.online + ":" + ip.mac);
      }
      if (ip.notify && !first) {
        if (was_online && !ip.online) {
          Notify.notify_ip(ip.host, ip.online);
        }
        if (!was_online && ip.online) {
          Notify.notify_ip(ip.host, ip.online);
        }
      }
    }
    first = false;
  }
}
