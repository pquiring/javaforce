/** ScanNetwork
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.net.PacketCapture;

public class ScanNetwork {
  private String nifip;
  private String first;
  private String last;

  private static final boolean debug = true;

  public static byte[] scan(String nifip, String first, String last) {
    ScanNetwork scan = new ScanNetwork();
    scan.nifip = nifip;
    scan.first = first;
    scan.last = last;
    return scan.run();
  }

  private byte[] run() {
    PacketCapture cap = new PacketCapture();
    cap.debug = debug;
    String nif = cap.findInterface(nifip);
    if (nif == null) {
      JFLog.log("Error:Network Interface not found for IP:" + nifip);
      return null;
    }
    if (debug) {
      JFLog.log("ScanNetwork:nif=" + nif + ",ip=" + nifip);
    }
    long id = cap.start(nif, nifip);
    cap.compile(id, "arp");
    byte[] mac;
    byte[] ip_self = PacketCapture.decode_ip(nifip);
    byte[] ip_first = PacketCapture.decode_ip(first);
    byte[] ip_last = PacketCapture.decode_ip(last);
    int length = PacketCapture.get_ip_range_length(ip_first, ip_last);
    if (length == -1) {
      JFLog.log("Error:ScanNetwork() : invalid range");
      return null;
    }
    byte[] map = new byte[length * 7];
    PacketCapture.increment_ip(ip_last);
    int pos = 0;
    if (debug) {
      JFLog.log("Scanning:" + first + " to " + last);
    }
    int up = 0, down = 0;
    while (Status.active && !PacketCapture.compare_ip(ip_first, ip_last)) {
      try {
        boolean ok = false;
        if (debug) {
          JFLog.log("ARP:" + PacketCapture.build_ip(ip_first));
        }
        if (PacketCapture.compare_ip(ip_first, ip_self)) {
          //do not query self
          //TODO : get MAC of self
          mac = null;
        } else {
          mac = cap.arp(id, PacketCapture.build_ip(ip_first), 2000);
        }
        ok = mac != null;
        JF.sleep(250);  //avoid hammering the network
        if (ok) {
          if (debug) {
            JFLog.log("IP=" + PacketCapture.build_ip(ip_first) + ":MAC=" + PacketCapture.build_mac(mac));
          }
          map[pos++] = 1;
          for(int a=0;a<6;a++) {
            map[pos++] = mac[a];
          }
          up++;
        } else {
          if (debug) {
            JFLog.log("IP=" + PacketCapture.build_ip(ip_first) + ":N/A");
          }
          map[pos++] = 0;
          down++;
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      PacketCapture.increment_ip(ip_first);
    }
    cap.stop(id);
    if (!Status.active) return null;
    if (debug) {
      JFLog.log("Scan Result:up=" + up + ":down=" + down);
    }
    return Arrays.copyOf(map, pos);
  }
}
