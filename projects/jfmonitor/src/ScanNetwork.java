/**
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.net.PacketCapture;

public class ScanNetwork {
  private String nic;
  private String first;
  private String last;

  private static final boolean debug = false;

  public static byte[] scan(String nic, String first, String last) {
    ScanNetwork scan = new ScanNetwork();
    scan.nic = nic;
    scan.first = first;
    scan.last = last;
    return scan.run();
  }

  public byte[] run() {
    PacketCapture cap = new PacketCapture();
    long id = cap.start(cap.findInterface(nic), nic);
    byte[] mac;
    byte[] ip_first = PacketCapture.decode_ip(first);
    byte[] ip_last = PacketCapture.decode_ip(last);
    int length = PacketCapture.get_ip_range_length(ip_first, ip_last);
    if (length == -1) return null;
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
        mac = cap.arp(id, PacketCapture.build_ip(ip_first), 2000);
        ok = mac != null;
        JF.sleep(1000);
        if (ok) {
          map[pos++] = 1;
          for(int a=0;a<6;a++) {
            map[pos++] = mac[a];
          }
          up++;
        } else {
          map[pos++] = 0;
          down++;
        }
      } catch (Exception e) {}
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
