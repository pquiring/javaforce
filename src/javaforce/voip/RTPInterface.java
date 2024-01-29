package javaforce.voip;

/**
 * Interface to allow RTP to trigger callbacks.
 */

public interface RTPInterface {

  /**
   * Triggered when RTP packet is received.
   *
   * @param rtp = RTPChannel
   * @param codecType = CodecType value
   * @param data = packet data
   * @param off = offset into data where packet starts
   * @param len = length of packet
   */
  public void rtpPacket(RTPChannel rtp, int codecType, byte[] data, int off, int len);

  /**
   * Triggered when new voice RTP data has arrived and been decoded.
   * It is safe to call getSamples()
   */
  public void rtpSamples(RTPChannel rtp);

  /**
   * Triggered when DTMF code has arrived.
   */
  public void rtpDigit(RTPChannel rtp, char digit);

  /**
   * Triggered when an RTPChannel has been inactive for some time.
   */
  public void rtpInactive(RTPChannel rtp);
}
