package javaforce.media;

/** Media Decoder.
 *
 * @author pquiring
 */

public class MediaDecoder extends MediaCoder {
  /**
   * Starts decoder.
   *
   * @parma io = MediaIO interface
   * @param new_width = change video width during decoding (-1 = no change)
   * @param new_height = change video height during decoding (-1 = no change)
   * @param new_chs = change audio channels during decoding (-1 = no change)
   * @param new_freq = change audio frequency during decoding (-1 = no change)
   * @param seekable = do you need to seek position during playback?
   */
  public native boolean start(MediaIO io, int new_width, int new_height, int new_chs, int new_freq, boolean seekable);

  /**
   * Starts decoder.
   *
   * @parma file = file to write to.
   * @param new_width = change video width during decoding (-1 = no change)
   * @param new_height = change video height during decoding (-1 = no change)
   * @param new_chs = change audio channels during decoding (-1 = no change)
   * @param new_freq = change audio frequency during decoding (-1 = no change)
   */
  public native boolean startFile(String file, String input_format, int new_width, int new_height, int chs, int new_freq);
  public native void stop();
  public native int read();
  public native int[] getVideo();
  public native short[] getAudio();
  public native int getWidth();
  public native int getHeight();
  public native float getFrameRate();
  public native long getDuration();
  public native int getSampleRate();
  public native int getChannels();
  public native int getBitsPerSample();
  public native boolean seek(long seconds);
  public native int getVideoBitRate();
  public native int getAudioBitRate();
  public native boolean isKeyFrame();
  public native boolean resize(int newWidth, int newHeight);
}
