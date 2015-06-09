package javaforce.media;

/** Media "raw" video decoder.
 *
 * @author pquiring
 */

public class MediaVideoDecoder extends MediaCoder {
  public native boolean start(int codec_id, int new_width, int new_height);
  public native void stop();
  public native int[] decode(byte data[]);
  public native int getWidth();
  public native int getHeight();
  public native float getFrameRate();
}
