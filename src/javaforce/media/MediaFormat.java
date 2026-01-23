package javaforce.media;

/** Base class for MediaInput, MediaOutput
 *   and MediaEncoder, MediaDecoder
 *
 * @author pquiring
 */

public class MediaFormat extends MediaCoder {
  public int getVideoStream() {
    return MediaAPI.getInstance().getVideoStream(ctx);
  }

  public int getAudioStream() {
    return MediaAPI.getInstance().getAudioStream(ctx);
  }

  public int getVideoCodecID() {
    return MediaAPI.getInstance().getVideoCodecID(ctx);
  }

  public int getAudioCodecID() {
    return MediaAPI.getInstance().getAudioCodecID(ctx);
  }

  public int getVideoBitRate() {
    return MediaAPI.getInstance().getVideoBitRate(ctx);
  }

  public int getAudioBitRate() {
    return MediaAPI.getInstance().getAudioBitRate(ctx);
  }
}
