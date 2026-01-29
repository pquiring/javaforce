package javaforce.jni;

/** Media native API (JNI implementation)
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.media.*;

public class MediaJNI implements MediaAPI {
  private static MediaAPI instance;
  public static MediaAPI getInstance() {
    if (instance == null) {
      instance = new MediaJNI();
    }
    return instance;
  }

  //MediaCoder
  public native boolean mediaLoadLibs(String codec, String device, String filter, String format, String util, String scale, String postproc, String resample);
  public native void mediaSetLogging(boolean state);

  //MediaFormat
  public native int getVideoStream(long ctx);
  public native int getAudioStream(long ctx);
  public native int getVideoCodecID(long ctx);
  public native int getAudioCodecID(long ctx);
  public native int getVideoBitRate(long ctx);
  public native int getAudioBitRate(long ctx);

  //MediaInput
  public native long inputOpenFile(String file, String format);
  public native long inputOpenIO(MediaIO io);
  public native long getDuration(long ctx);
  public native int getVideoWidth(long ctx);
  public native int getVideoHeight(long ctx);
  public native float getVideoFrameRate(long ctx);
  public native int getVideoKeyFrameInterval(long ctx);
  public native int getAudioChannels(long ctx);
  public native int getAudioSampleRate(long ctx);
  public native boolean inputClose(long ctx);
  public native boolean inputOpenVideo(long ctx, int width, int height);
  public native boolean inputOpenAudio(long ctx, int chs, int freq);
  public native int inputRead(long ctx);
  public native boolean getPacketKeyFrame(long ctx);
  public native int getPacketData(long ctx, byte[] data, int offset, int length);
  public native boolean inputSeek(long ctx, long seconds);

  //MediaOutput
  public native long outputCreateFile(String file, String format);
  public native long outputCreateIO(MediaIO io, String format);
  public native int addVideoStream(long ctx, int codec_id, int bit_rate, int width, int height, float fps, int keyFrameInterval);
  public native int addAudioStream(long ctx, int codec_id, int bit_rate, int chs, int freq);
  public native boolean outputClose(long ctx);
  public native boolean writeHeader(long ctx);
  public native boolean writePacket(long ctx, int stream, byte[] data, int offset, int length, boolean keyFrame);

  //MediaAudioDecoder
  public native long audioDecoderStart(int codec_id, int new_chs, int new_freq);
  public native void audioDecoderStop(long ctx);
  public native short[] audioDecoderDecode(long ctx, byte[] data, int offset, int length);
  public native int audioDecoderGetChannels(long ctx);
  public native int audioDecoderGetSampleRate(long ctx);
  public native boolean audioDecoderChange(long ctx, int chs, int freq);

  //MediaAudioEncoder
  public native long audioEncoderStart(int codec_id, int bit_rate, int chs, int freq);
  public native void audioEncoderStop(long ctx);
  public native byte[] audioEncoderEncode(long ctx, short[] samples, int offset, int length);
  public native int audioEncoderGetAudioFramesize(long ctx);

  //MediaVideoDecoder
  public native long videoDecoderStart(int codec_id, int new_width, int new_height);
  public native void videoDecoderStop(long ctx);
  public native int[] videoDecoderDecode(long ctx, byte[] data, int offset, int length);
  public native int videoDecoderGetWidth(long ctx);
  public native int videoDecoderGetHeight(long ctx);
  public native float videoDecoderGetFrameRate(long ctx);
  public native boolean videoDecoderChange(long ctx, int width, int height);

  //MediaVideoEncoder
  public native long videoEncoderStart(int codec_id, int bit_rate, int width, int height, float fps, int keyFrameInterval);
  public native void videoEncoderStop(long ctx);
  public native byte[] videoEncoderEncode(long ctx, int[] px, int offset, int length);

  //VideoBuffer
  public native float compareFrames(int[] frame1, int[] frame2, int width, int height);
}
