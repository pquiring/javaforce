package javaforce.jni;

/** speex JNI
 *
 * @author pquiring
 */

import javaforce.api.*;

public class SpeexJNI implements SpeexAPI {
  public native long speexCreate(int sample_rate, int echo_buffers);
  public native void speexFree(long ctx);
  public native void speexDenoise(long ctx, short[] audio, int length);
  public native void speexEcho(long ctx, short[] audio_mic, short[] audio_spk, short[] audio_out, int length);

  private static SpeexJNI instance;
  public static SpeexJNI getInstance() {
    if (instance == null) {
      instance = new SpeexJNI();
    }
    return instance;
  }
}
