package javaforce.voip;

/** Codec Info
 *
 * @author pquiring
 */

public class CodecInfo {
  //common
  public int stream;
  public int bit_rate;
  public long duration;

  //video
  public int width;
  public int height;
  public float fps;
  public int keyFrameInterval;

  //audio
  public int chs;
  public int freq;
  public int bits;
}
