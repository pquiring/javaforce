package javaforce.voip;

/** RTPCodec
 *
 * @author pquiring
 */

import java.util.*;

public abstract class RTPCodec {
  public Random random = new Random();
  public abstract byte[][] encode(byte data[], int x, int y, int id);
  public abstract Packet decode(byte rtp[], int offset, int length);
}
