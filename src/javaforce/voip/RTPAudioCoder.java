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
  /** Encodes audio samples into RTP packet. */
  public byte[] encode(short[] src16);
  /** Decodes RTP packet into audio samples. */
  public short[] decode(byte[] src8, int off, int length);
  /** Returns sample rate. */
  public int getSampleRate();
  /** Returns encoded packet size (excluding RTP header)
   * -1 = variable sized
   */
  public int getPacketSize();
}
