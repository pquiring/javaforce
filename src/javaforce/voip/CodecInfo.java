package javaforce.voip;

/** Codec Info
 *
 * @author pquiring
 */

public class CodecInfo {
  //common
  public long duration;

  //video
  public int width;
  public int height;
  public float fps;
  public int keyFrameInterval;
  public int video_codec;
  public int video_bit_rate;
  public int video_stream;

  //audio
  public int chs;
  public int freq;
  public int bits;
  public int audio_codec;
  public int audio_bit_rate;
  public int audio_stream;
}
