package javaforce.voip;

import javaforce.*;
import javaforce.codec.gsm.*;

/**
 * Encodes/decodes GSM packets.
 */
public class gsm implements RTPAudioCoder {

  private GSMEncoder encoder = new GSMEncoder();
  private GSMDecoder decoder = new GSMDecoder();
  private RTP rtp;

  public gsm(RTP rtp) {
    this.rtp = rtp;
  }

  private byte[] edata = new byte[33];
  private byte[] encoded = new byte[33 + 12];

  //samples must be 160 samples
  //id ignored (fixed)
  public byte[] encode(short[] samples, int id) {
    RTPChannel rtpChannel = rtp.getDefaultChannel();
    RTPChannel.buildHeader(encoded, 3, rtpChannel.getseqnum(), rtpChannel.gettimestamp(160), rtpChannel.getssrc(), false);
    encoder.encode(samples, edata);
    System.arraycopy(edata, 0, encoded, 12, 33);
    return encoded;
  }

  private int decode_timestamp;

  private short[] decoded = new short[160];
  private byte[] ddata = new byte[33];

  //encoded must be 20+12 bytes at least
  public short[] decode(byte[] encoded, int off) {
    int decode_timestamp = BE.getuint32(encoded, off + 4);
    if (this.decode_timestamp == 0) {
      this.decode_timestamp = decode_timestamp;
    } else {
      if (RTP.debug) {
        JFLog.log("GSM:timestamp = " + decode_timestamp + ":" + ((this.decode_timestamp + 160 == decode_timestamp) ? "ok" : "lost packet"));
      }
      this.decode_timestamp = decode_timestamp;
    }
    System.arraycopy(encoded, 12, ddata, 0, 33);
    try {
      int[] tmp = decoder.decode(ddata);
      for(int a=0;a<160;a++) {
        decoded[a] = (short)tmp[a];
      }
    } catch (Exception e) {
      return null;
    }
    return decoded;
  }

  public int getSampleRate() {return 8000;}
}
