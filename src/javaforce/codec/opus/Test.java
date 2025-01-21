package javaforce.codec.opus;

/** Opus test
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.media.*;

public class Test {
  private static final int frame_size = 960;
  public static void main(String[] args) {
    MediaCoder.init();
    MediaCoder.setLogging(true);
    try {
      JFLog.log("frame_size=" + frame_size);
      short[] sams = new short[frame_size];
      byte[] encoded;
      AudioGenerate audio = new AudioGenerate();
      OpusEncoder encoder = new OpusEncoder();
      JFLog.log("encoder.open");
      encoder.open();
      OpusDecoder decoder = new OpusDecoder();
      JFLog.log("decoder.open");
      decoder.open();
      JFLog.log("start...");
      Wav wav_in = new Wav(1, 16, 48000);
      Wav wav_out = new Wav(1, 16, 48000);
      for(int a=0;a<16;a++) {
        audio.generate(sams, AudioGenerate.SINE, 48000, 1000, 1.0f);
        wav_in.add(sams);
        encoded = encoder.encode(sams);
        if (encoded == null) throw new Exception("encoder failed");
        JFLog.log("encoded=" + encoded.length);
        short[] decoded = decoder.decode(encoded, 0, encoded.length);
        if (decoded == null) throw new Exception("decoder failed");
        JFLog.log("decoded=" + decoded.length);
        wav_out.add(decoded);
      }
      encoder.close();
      decoder.close();
      wav_in.save("test-in.wav");
      wav_out.save("test-out.wav");
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}
