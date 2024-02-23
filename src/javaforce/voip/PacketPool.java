package javaforce.voip;

/** Packet Pool
 *
 * @author pquiring
 */

import java.util.*;

public class PacketPool {

  private int mtu;

  private static final int max_packets = 64;

  private Object availLock = new Object();
  private ArrayList<Packet> avail = new ArrayList<>();

  private Object inuseLock = new Object();
  private ArrayList<Packet> inuse = new ArrayList<>();

  public PacketPool(int mtu) {
    this.mtu = mtu;
  }

  public Packet alloc() {
    Packet packet;
    synchronized (availLock) {
      if (avail.size() > 0) {
        packet = avail.remove(0);
      } else {
        if (inuse.size() > max_packets) {
          return null;
        }
        packet = new Packet();
        packet.data = new byte[mtu];
      }
      synchronized (inuseLock) {
        inuse.add(packet);
      }
    }
    packet.offset = 0;
    packet.length = 0;
    packet.host = null;
    packet.port = 0;
    return packet;
  }

  public void free(Packet packet) {
    synchronized (inuseLock) {
      inuse.remove(packet);
    }
    synchronized (availLock) {
      avail.add(packet);
    }
  }

  public int count() {
    return avail.size() + inuse.size();
  }
}
