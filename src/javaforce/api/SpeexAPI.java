package javaforce.api;

/** Speex native API
 *
 * @author pquiring
 */

public interface SpeexAPI {
  public long speexCreate(int sample_rate, int echo_buffers);
  public void speexFree(long ctx);
  public void speexDenoise(long ctx, short[] audio, int length);
  public void speexEcho(long ctx, short[] audio_mic, short[] audio_spk, short[] audio_out, int length);
}
