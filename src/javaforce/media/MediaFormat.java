package javaforce.media;

/** Base class for MediaInput, MediaOutput
 *   and MediaEncoder, MediaDecoder
 *
 * @author pquiring
 */

import javaforce.api.*;

public class MediaFormat extends MediaCoder {
  /** Gets video stream index. */
  public int getVideoStream() {
    return MediaAPI.getInstance().getVideoStream(ctx);
  }

  /** Gets audio stream index. */
  public int getAudioStream() {
    return MediaAPI.getInstance().getAudioStream(ctx);
  }

  /** Gets video stream codec ID. */
  public int getVideoCodecID() {
    return MediaAPI.getInstance().getVideoCodecID(ctx);
  }

  /** Gets audio stream codec ID. */
  public int getAudioCodecID() {
    return MediaAPI.getInstance().getAudioCodecID(ctx);
  }

  /** Gets video stream bit rate. */
  public int getVideoBitRate() {
    return MediaAPI.getInstance().getVideoBitRate(ctx);
  }

  /** Gets audio stream bit rate. */
  public int getAudioBitRate() {
    return MediaAPI.getInstance().getAudioBitRate(ctx);
  }
}
