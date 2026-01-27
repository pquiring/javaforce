package javaforce.jni;

/** speex JNI
 *
 * @author pquiring
 */

import javaforce.voip.codec.*;

public class SpeexJNI extends speex {
  public native long speexCreate(int sample_rate, int echo_buffers);
  public native void speexFree(long ctx);
  public native void speexDenoise(long ctx, short[] audio);
  public native void speexEcho(long ctx, short[] audio_mic, short[] audio_spk, short[] audio_out);
}
