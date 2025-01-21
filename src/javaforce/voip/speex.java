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
  private static int quality = 5;  //0-10
  private static boolean enhanced = false;

  private SpeexEncoder encoder = new SpeexEncoder();
  private SpeexDecoder decoder = new SpeexDecoder();
  private RTP rtp;
  private int rtp_id;
  private int rate;
  private int nsamples;

  public speex(RTP rtp, int rate) {
    this.rtp = rtp;
    this.rate = rate;
    switch (rate) {
      case 8000:
        nsamples = 160;
        mode = 0;
        break;
      case 16000:
        nsamples = 160 * 2;
        mode = 1;
        break;
      case 32000:
        nsamples = 160 * 4;
        mode = 2;
        break;
    }
    encoder.init(mode, quality, rate, 1);
    decoder.init(mode, rate, 1, enhanced);
    encoded = new byte[12];
    decoded = new short[nsamples];
  }

  public void setid(int id) {
    this.rtp_id = id;
  }

  public int getPacketSize() {
    return -1;  //variable sized
  }

  /** Set encoder quality (0-10)
   * Default = 5.
   * Affects only new speex instances.
   */
  public speex setQuality(int value) {
    if (value < 0) value = 0;
    if (value > 10) value = 10;
    quality = value;
    return this;
  }

  /** Set decoder enhanced mode.
   * Default = false
   * Affects only new speex instances.
   */
  public speex setEnhancedMode(boolean mode) {
    enhanced = mode;
    return this;
  }

  private byte[] encoded;

  //samples must be multiple of 160 samples
  public byte[] encode(short[] samples) {
    encoder.processData(samples, 0, samples.length);
    int encoded_length = encoder.getProcessedDataByteSize();
    if (debug) {
      JFLog.log("speex:encoded.size=" + encoded_length);
    }
    if (encoded.length != encoded_length + 12) {
      encoded = new byte[encoded_length + 12];
    }
    encoder.getProcessedData(encoded, 12);
    RTPChannel rtpChannel = rtp.getDefaultChannel();
    RTPChannel.buildHeader(encoded, rtp_id, rtpChannel.getseqnum(), rtpChannel.gettimestamp(nsamples), rtpChannel.getssrc(), false);
    return encoded;
  }

  private int decode_timestamp;

  private short[] decoded;

  public short[] decode(byte[] encoded, int off, int length) {
    int decode_timestamp = BE.getuint32(encoded, off + 4);
    if (this.decode_timestamp == 0) {
      this.decode_timestamp = decode_timestamp;
    } else {
      if (RTP.debug) {
        JFLog.log("speex:timestamp = " + decode_timestamp + ":" + ((this.decode_timestamp + nsamples == decode_timestamp) ? "ok" : "lost packet"));
      }
      this.decode_timestamp = decode_timestamp;
    }
    try {
      decoder.processData(encoded, 12, length - 12);
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
      RTP rtp = new RTP();
      SDP sdp = new SDP();
      sdp.setIP("1.2.3.4");
      SDP.Stream stream = sdp.addStream(SDP.Type.audio);
      rtp.createChannel(stream);
      speex sx = new speex(rtp, rate);
      short[] samples = new short[sx.nsamples];
      Random r = new Random();
      for(int a=0;a<sx.nsamples;a++) {
        samples[a] = (short)(r.nextInt(0xffff) - 0x7fff);
      }
      byte[] data = sx.encode(samples);
      short[] out;
      out = sx.decode(data, 0, data.length);
      JFLog.log("out.length=" + out.length);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //these are some Speex optional digital signal processing (DSP) functions

  private native static long speexdspinit(int sample_rate, int echo_buffers);

  /** Allocate speex DSP context.
   * Audio buffers should be 160 samples.
   * NOTE : speex SDP functions do NOT require the use of speex codec.
   *
   *  @param sample_rate = sample rate
   *  @return ctx
   */
  public static long speex_dsp_init(int sample_rate) {
    return speexdspinit(sample_rate, -1);
  }

  /** Allocate speex DSP context.
   * Audio buffers should be 160 samples (50 per second, 20ms each).
   * NOTE : speex SDP functions do NOT require the use of speex codec.
   *
   *  @param sample_rate = sample rate
   *  @param echo_buffers = # of buffers to allocate for echo cancellation (-1=default of 10)
   *  @return ctx
   */
  public static long speex_dsp_init(int sample_rate, int echo_buffers) {
    return speexdspinit(sample_rate, echo_buffers);
  }

  public native static void speexdspuninit(long ctx);
  /** Free speex DSP context
   *
   * @param ctx = speex context
   */
  public static void speex_dsp_uninit(long ctx) {
    speexdspuninit(ctx);
  }

  public native static void speexdspdenoise(long ctx, short[] audio);
  /** Performs denoise function.
   *
   * @param ctx = speex context
   * @param audio = audio samples
   */
  public static void speex_dsp_denoise(long ctx, short[] audio) {
    speexdspdenoise(ctx, audio);
  }

  public native static void speexdspecho(long ctx, short[] audio_mic, short[] audio_spk, short[] audio_out);
  /** Performs echo cancellation.
   *
   * @param ctx = speex context
   * @param audio_out = outbound audio (not modified)
   * @param audio_mic = mic audio (modified)
   */
  public static void speex_dsp_echo(long ctx, short[] audio_mic, short[] audio_spk, short[] audio_out) {
    speexdspecho(ctx, audio_mic, audio_spk, audio_out);
  }

  private static void noise(short[] audio) {
    Random r = new Random();
    int len = audio.length;
    for(int i = 0;i<len;i++) {
      audio[i] = (short)(r.nextInt(0xffff) - 0x7fff);
    }
  }

  private static long avg(short[] audio) {
    long sum = 0;
    int len = audio.length;
    for(int i=0;i<len;i++) {
      sum += audio[i];
    }
    return sum / len;
  }

  private static void test_dsp() {
    short[] mic = new short[160];
    short[] spk = new short[160];
    short[] out = new short[160];

    noise(mic);
    noise(spk);

    long ctx = speex_dsp_init(8000, -1);

    JFLog.log("spk.avg=" + avg(spk));

    JFLog.log("mic.avg=" + avg(mic));
    speex_dsp_denoise(ctx, mic);
    JFLog.log("mic.avg=" + avg(mic));

    speex_dsp_echo(ctx, mic, spk, out);
    JFLog.log("out.avg=" + avg(out));

    speex_dsp_uninit(ctx);
  }

  public static void main(String[] args) {
    debug = true;
    test(8000);
    test(16000);
    test(32000);
    test_dsp();
  }

  public void close() {}
}
