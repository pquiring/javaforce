package javaforce.voip;

import java.util.*;

import javaforce.*;
import javaforce.codec.speex.*;

/**
 * Encodes/decodes speex packets.
 *
 * RFC 5574
 */

public class speex implements RTPAudioCoder {

  private static boolean debug = false;

  private SpeexEncoder encoder = new SpeexEncoder();
  private SpeexDecoder decoder = new SpeexDecoder();
  private RTP rtp;
  private int rtp_id;
  private int rate;

  public speex(RTP rtp, int rate) {
    this.rtp = rtp;
    this.rate = rate;
    encoder.init(0, 4, rate, 1);
    decoder.init(0, rate, 1, false);
  }

  public void setid(int id) {
    this.rtp_id = id;
  }

  private byte[] encoded = new byte[20 + 12];  //((160 / 80) * 10) + 12

  //samples must be 160 samples
  public byte[] encode(short[] samples) {
    RTPChannel rtpChannel = rtp.getDefaultChannel();
    RTPChannel.buildHeader(encoded, rtp_id, rtpChannel.getseqnum(), rtpChannel.gettimestamp(160), rtpChannel.getssrc(), false);
    encoder.processData(samples, 0, samples.length);
    if (debug) {
      JFLog.log("speex:encoded.size=" + encoder.getProcessedDataByteSize());
    }
    encoder.getProcessedData(encoded, 12);
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
    try {
      decoder.processData(encoded, 12, 20);
      if (debug) {
        JFLog.log("speex.decoded.size=" + decoder.getProcessedDataByteSize() / 2);
      }
      decoder.getProcessedData(decoded, 0);
    } catch (Exception e) {
      JFLog.log("Error:speex:decode:" + e);
      JFLog.log(e);
    }
    return decoded;
  }

  public int getSampleRate() {return rate;}

  public static void main(String[] args) {
    try {
      debug = true;
      RTP rtp = new RTP();
      SDP sdp = new SDP();
      sdp.setIP("1.2.3.4");
      SDP.Stream stream = sdp.addStream(SDP.Type.audio);
      rtp.createChannel(stream);
      speex sx = new speex(rtp, 16000);
      int cnt = 160;
      short[] samples = new short[cnt];
      Random r = new Random();
      for(int a=0;a<160;a++) {
        samples[a] = (short)(r.nextInt(0xffff) - 0x7fff);
      }
      byte[] data = sx.encode(samples);
      short[] out;
      out = sx.decode(data, 0);
      JFLog.log("out.length=" + out.length);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
