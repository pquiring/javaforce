package javaforce.voip;

/**
 * Interface to allow RTP to trigger callbacks.
 */
public interface RTPInterface {

  /**
   * Triggered when new voice RTP data has arrived and been decoded. It is safe
   * to call getSamples()
   */
  public void rtpSamples(RTP rtp);

  /**
   * Triggered when DTMF code has arrived.
   */
  public void rtpDigit(RTP rtp, char digit);

  /**
   * Triggered when RTP is using raw mode and a packet is received.
   */
  public void rtpPacket(RTP rtp, boolean rtcp, byte data[], int off, int len);

  /**
   * Triggered when a H.263 packet is received.
   */
  public void rtpH263(RTP rtp, byte data[], int off, int len);

  /**
   * Triggered when a H.264 packet is received.
   */
  public void rtpH264(RTP rtp, byte data[], int off, int len);

  /**
   * Triggered when a JPEG packet is received.
   */
  public void rtpJPEG(RTP rtp, byte data[], int off, int len);
}
