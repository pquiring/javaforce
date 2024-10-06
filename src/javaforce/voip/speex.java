package javaforce.voip;

import java.util.*;

import javaforce.*;
import javaforce.codec.speex.*;

/**
 * Encodes/decodes speex packets.
 *
 * RFC 5574
 *
 */

public class speex implements RTPAudioCoder {

  private static boolean debug = false;

  private int mode = 0;  //0=NB 1=WB 2=UWB
  private int quality = 5;  //0-10
  private boolean enhanced = false;

  private SpeexEncoder encoder = new SpeexEncoder();
  private SpeexDecoder decoder = new SpeexDecoder();
  private RTP rtp;
  private int rtp_id;
  private int rate;

  public speex(RTP rtp, int rate) {
    this.rtp = rtp;
    this.rate = rate;
    switch (rate) {
      case 8000:
        decoded = new short[160];
        mode = 0;
        break;
      case 16000:
        decoded = new short[160 * 2];
        mode = 1;
        break;
      case 32000:
        decoded = new short[160 * 4];
        mode = 2;
        break;
    }
    encoder.init(mode, quality, rate, 1);
    decoder.init(mode, rate, 1, enhanced);
    encoded = new byte[12];
  }

  public void setid(int id) {
    this.rtp_id = id;
  }

  public int getPacketSize() {
    return -1;  //variable sized
  }

  private byte[] encoded;

  //samples must be multiple of 160 samples
  public byte[] encode(short[] samples) {
    RTPChannel rtpChannel = rtp.getDefaultChannel();
    RTPChannel.buildHeader(encoded, rtp_id, rtpChannel.getseqnum(), rtpChannel.gettimestamp(160), rtpChannel.getssrc(), false);
    encoder.processData(samples, 0, samples.length);
    int encoded_length = encoder.getProcessedDataByteSize();
    if (debug) {
      JFLog.log("speex:encoded.size=" + encoded_length);
    }
    if (encoded.length != encoded_length + 12) {
      encoded = new byte[encoded_length + 12];
    }
    encoder.getProcessedData(encoded, 12);
    return encoded;
  }

  private int decode_timestamp;

  private short[] decoded;

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
      decoder.processData(encoded, 12, encoded.length - 12);
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

  private static void test(int rate) {
    try {
      int mult = rate / 8000;
      RTP rtp = new RTP();
      SDP sdp = new SDP();
      sdp.setIP("1.2.3.4");
      SDP.Stream stream = sdp.addStream(SDP.Type.audio);
      rtp.createChannel(stream);
      speex sx = new speex(rtp, rate);
      int cnt = 160 * mult;
      short[] samples = new short[cnt];
      Random r = new Random();
      for(int a=0;a<cnt;a++) {
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

  public static void main(String[] args) {
    debug = true;
    test(8000);
    test(16000);
    test(32000);
  }
}
