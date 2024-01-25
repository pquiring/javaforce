package javaforce.voip;

/** RTPCodec
 *
 * @author pquiring
 */

import java.util.*;

public abstract class RTPCodec {
  public Random random = new Random();
  public abstract void encode(byte[] data, int x, int y, int id, PacketReceiver pr);
  public abstract void decode(byte[] rtp, int offset, int length, PacketReceiver pr);
}
