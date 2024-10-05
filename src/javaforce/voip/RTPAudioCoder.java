package javaforce.voip;

/** RTPAudioCoder
 *
 * Base interface for all audio codec encoders/decoders.
 *
 * Encoders build RTP packets from raw audio samples.
 * Decoders build raw audio samples from RTP packets.
 *
 */

public interface RTPAudioCoder {
  /** Sets RTP payload ID.
   * This value is exchanged in SDP packets.
   * For most audio codecs this value is fixed and ignored.
   */
  public void setid(int id);
  public byte[] encode(short[] src16);
  public short[] decode(byte[] src8, int off);
  public int getSampleRate();
}
