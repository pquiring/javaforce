package javaforce.voip;

/*
 struct rtp_header {
 byte version;  //usually 0x80
 byte type;  //0x12 (18)
 short seqnum;
 int timestamp;
 int syncsrcid;
 };
 */

import javaforce.*;
import javaforce.codec.g729a.*;

/**
 * Encodes/decodes g729a packets.
 *
 * http://tools.ietf.org/html/rfc4749
 */
public class g729a implements RTPAudioCoder {

  private Encoder encoder = new Encoder();
  private Decoder decoder = new Decoder();
  private RTP rtp;

  public g729a(RTP rtp) {
    this.rtp = rtp;
  }

  public void setid(int id) {};  //ignored - fixed value

  private byte[] encoded = new byte[20 + 12];  //((160 / 80) * 10) + 12

  //samples must be 160 samples
  public byte[] encode(short[] samples) {
    RTPChannel rtpChannel = rtp.getDefaultChannel();
    RTPChannel.buildHeader(encoded, 18, rtpChannel.getseqnum(), rtpChannel.gettimestamp(160), rtpChannel.getssrc(), false);
    encoder.encode(encoded, 12, samples, 0, 160 / 80);  //output, outputOffset, input, inputOffset, # 80 samples packets (2)
    return encoded;
  }

  private int decode_timestamp;

  private short[] decoded = new short[160];

  //encoded must be 20+12 bytes at least
  public short[] decode(byte[] encoded, int off) {
    int decode_timestamp = BE.getuint32(encoded, off + 4);
    if (this.decode_timestamp == 0) {
      this.decode_timestamp = decode_timestamp;
    } else {
      if (RTP.debug) {
        JFLog.log("G729a:timestamp = " + decode_timestamp + ":" + ((this.decode_timestamp + 160 == decode_timestamp) ? "ok" : "lost packet"));
      }
      this.decode_timestamp = decode_timestamp;
    }
    decoder.decode(decoded, 0, encoded, off + 12, 20 / 10);
    return decoded;
  }

  public int getSampleRate() {return 8000;}
}
