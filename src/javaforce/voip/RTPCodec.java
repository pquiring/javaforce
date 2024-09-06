package javaforce.voip;

/** RTPCodec
 *
 * NOTE : Each derived class should be used for encoder or decoded (never both).
 *
 * @author pquiring
 */

import java.util.*;

public abstract class RTPCodec {
  public Random random = new Random();
  public abstract void encode(byte[] data, int offset, int length, int x, int y, int id, PacketReceiver pr);
  public abstract void decode(byte[] rtp, int offset, int length, PacketReceiver pr);
}
