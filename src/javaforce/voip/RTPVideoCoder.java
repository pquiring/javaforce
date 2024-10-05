package javaforce.voip;

/** RTPCodec
 *
 * NOTE : Each derived class should be used for encoder or decoded (never both).
 *
 * @author pquiring
 */

public interface RTPVideoCoder {
  public void setid(int id);
  public abstract void encode(byte[] data, int offset, int length, int x, int y, PacketReceiver pr);
  public abstract void decode(byte[] rtp, int offset, int length, PacketReceiver pr);
}
