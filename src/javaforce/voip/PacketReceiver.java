package javaforce.voip;

/** PacketReceiver
 *
 * @author pquiring
 */

public interface PacketReceiver {
  public void onPacket(Packet packet);
}
