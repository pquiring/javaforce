package javaforce.media;

/** Media API
 *
 * @author pquiring
 */

import javaforce.jni.*;

public interface MediaAPI {

  public static MediaAPI getInstance() {
    return MediaJNI.getInstance();
  }

  //MediaCoder
  public boolean init();
  public void setLogging(boolean state);

  //MediaFormat
  public int getVideoStream(long ctx);
  public int getAudioStream(long ctx);
  public int getVideoCodecID(long ctx);
  public int getAudioCodecID(long ctx);
  public int getVideoBitRate(long ctx);
  public int getAudioBitRate(long ctx);

  //MediaInput
  public long openFile(String file, String format);
  public long openIO(MediaIO io);
  public long getDuration(long ctx);
  public int getVideoWidth(long ctx);
  public int getVideoHeight(long ctx);
  public float getVideoFrameRate(long ctx);
  public int getVideoKeyFrameInterval(long ctx);
  public int getAudioChannels(long ctx);
  public int getAudioSampleRate(long ctx);
  public boolean inputClose(long ctx);
  public boolean openvideo(long ctx, int width, int height);
  public boolean openaudio(long ctx, int chs, int freq);
  public int inputRead(long ctx);
  public boolean getPacketKeyFrame(long ctx);
  public int getPacketData(long ctx, byte[] data, int offset, int length);
  public boolean inputSeek(long ctx, long seconds);

  //MediaOutput
  public long createFile(String file, String format);
  public long createIO(MediaIO io, String format);
  public int addVideoStream(long ctx, int codec_id, int bit_rate, int width, int height, float fps, int keyFrameInterval);
  public int addAudioStream(long ctx, int codec_id, int bit_rate, int chs, int freq);
  public boolean outputClose(long ctx);
  public boolean writeHeader(long ctx);
  public boolean writePacket(long ctx, int stream, byte[] data, int offset, int length, boolean keyFrame);

  //MediaAudioDecoder
  public long audioDecoderStart(int codec_id, int new_chs, int new_freq);
  public void audioDecoderStop(long ctx);
  public short[] audioDecoderDecode(long ctx, byte[] data, int offset, int length);
  public int audioDecoderGetChannels(long ctx);
  public int audioDecoderGetSampleRate(long ctx);
  public boolean audioDecoderChange(long ctx, int chs, int freq);

  //MediaAudioEncoder
  public long audioEncoderStart(int codec_id, int bit_rate, int chs, int freq);
  public void audioEncoderStop(long ctx);
  public byte[] audioEncoderEncode(long ctx, short[] samples, int offset, int length);
  public int audioEncoderGetAudioFramesize(long ctx);

  //MediaVideoDecoder
  public long videoDecoderStart(int codec_id, int new_width, int new_height);
  public void videoDecoderStop(long ctx);
  public int[] videoDecoderDecode(long ctx, byte[] data, int offset, int length);
  public int videoDecoderGetWidth(long ctx);
  public int videoDecoderGetHeight(long ctx);
  public float videoDecoderGetFrameRate(long ctx);
  public boolean videoDecoderChange(long ctx, int width, int height);

  //MediaVideoEncoder
  public long videoEncoderStart(int codec_id, int bit_rate, int width, int height, float fps, int keyFrameInterval);
  public void videoEncoderStop(long ctx);
  public byte[] videoEncoderEncode(long ctx, int[] px, int offset, int length);

  //VideoBuffer
  public float compareFrames(int[] frame1, int[] frame2, int width, int height);
}
