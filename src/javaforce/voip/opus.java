package javaforce.voip;

/** opus rtp coder
 *
 * RFC : 7587
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.codec.opus.*;

public class opus implements RTPAudioCoder {

  private OpusEncoder encoder;
  private OpusDecoder decoder;
  private RTP rtp;
  private int rtp_id;
  private int rate;
  private int nsamples;

  //RTPAudioCoder

  public opus(RTP rtp, int rate) {
    this.rtp = rtp;
    this.rate = rate;
    this.nsamples = 160;
  }

  public void setid(int id) {
    rtp_id = id;
  }

  private byte[] encoded;

  public byte[] encode(short[] samples) {
    if (encoder == null) {
      encoder = new OpusEncoder();
      encoder.open();
    }
    byte[] packet = encoder.encode(samples);
    int encoded_length = packet.length;
    if (encoded == null || encoded.length != encoded_length + 12) {
      encoded = new byte[encoded_length + 12];
    }
    System.arraycopy(packet, 0, encoded, 12, encoded_length);
    RTPChannel rtpChannel = rtp.getDefaultChannel();
    RTPChannel.buildHeader(encoded, rtp_id, rtpChannel.getseqnum(), rtpChannel.gettimestamp(nsamples), rtpChannel.getssrc(), false);
    return encoded;
  }

  private int decode_timestamp;

  public short[] decode(byte[] encoded, int off, int length) {
    if (decoder == null) {
      decoder = new OpusDecoder();
      decoder.open();
    }
    int decode_timestamp = BE.getuint32(encoded, off + 4);
    if (this.decode_timestamp == 0) {
      this.decode_timestamp = decode_timestamp;
    } else {
      if (RTP.debug) {
        JFLog.log("opus:timestamp = " + decode_timestamp + ":" + ((this.decode_timestamp + nsamples == decode_timestamp) ? "ok" : "lost packet"));
      }
      this.decode_timestamp = decode_timestamp;
    }
    return decoder.decode(encoded, off + 12, length - 12);
  }

  public int getSampleRate() {
    return rate;
  }

  public int getPacketSize() {
    return 160;
  }

  public void close() {
    if (encoder != null) {
      encoder.close();
      encoder = null;
    }
    if (decoder != null) {
      decoder.close();
      decoder = null;
    }
  }
}
