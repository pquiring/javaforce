package javaforce.media;

/** Media encoder.
 *
 * @author pquiring
 */

public class MediaEncoder extends MediaCoder {
  //these must be set BEFORE you call start()
  public boolean fps_1000_1001;
  public int framesPerKeyFrame = 12;
  public int videoBitRate = 400000;
  public int audioBitRate = 128000;
  public native boolean start(MediaIO io, int width, int height, int fps, int chs, int freq, String codec,
    boolean doVideo, boolean doAudio);
  /** Sets frame rate = fps * 1000 / 1001 (default = false) */
  public void set1000over1001(boolean state) {
    fps_1000_1001 = state;
  }
  /** Sets frames per key frame (gop) (default = 12) */
  public void setFramesPerKeyFrame(int count) {
    framesPerKeyFrame = count;
  }
  /** Sets video bit rate (default = 400000) */
  public void setVideoBitRate(int rate) {
    videoBitRate = rate;
  }
  /** Sets audio bit rate (default = 128000) */
  public void setAudioBitRate(int rate) {
    audioBitRate = rate;
  }
  public native boolean addAudio(short sams[], int offset, int length);
  public boolean addAudio(short sams[]) {
    return addAudio(sams, 0, sams.length);
  }
  public native boolean addVideo(int px[]);
  public native int getAudioFramesize();
  /** Adds pre encoded audio. */
  public native boolean addAudioEncoded(byte packet[], int offset, int length);
  /** Adds pre encoded video. */
  public native boolean addVideoEncoded(byte packet[], int offset, int length, boolean key_frame);
  public native void stop();
}
