package javaforce.jni;

/** speex JNI
 *
 * @author pquiring
 */

import javaforce.voip.codec.*;

public class SpeexJNI extends speex {
  public native long init(int sample_rate, int echo_buffers);
  public native void uninit(long ctx);
  public native void denoise(long ctx, short[] audio);
  public native void echo(long ctx, short[] audio_mic, short[] audio_spk, short[] audio_out);
}
