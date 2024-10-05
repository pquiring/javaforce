package javaforce.voip;

/** RTPVideoCoder
 *
 * Base interface for all video codec encoders/decoders.
 *
 * Encoders build RTP packets from encoded video data.
 * Decoders build encoded video data from RTP packets.
 *
 * NOTE : Each derived class should be used for encoder or decoded (never both).
 *
 * @author pquiring
 */

public interface RTPVideoCoder {
  /** Sets RTP payload ID.
   * This value is exchanged in SDP packets.
   * For some video codecs this value is fixed and ignored.
   */
  public void setid(int id);
  public void encode(byte[] data, int offset, int length, int x, int y, PacketReceiver pr);
  public void decode(byte[] rtp, int offset, int length, PacketReceiver pr);
}
