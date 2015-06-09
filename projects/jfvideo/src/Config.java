/** Config data
 *
 * @author pquiring
 *
 * Created : Aug 11, 2014
 */

public class Config {
  public String path;
  public int videoRate;
  public int audioRate;
  public int audioChannels;
  public int width;
  public int height;
  public Track[] track;
  public boolean preview;
  public int videoBitRate;
  public int audioBitRate;
  public CameraKey[] cameraKey;
  public boolean v1001;

  public Config() {
    videoRate = 24;
    audioRate = 44100;
    audioChannels = 2;
    width = 640;
    height = 480;
    preview = true;
    videoBitRate = 400000;
    audioBitRate = 128000;
  }
}
