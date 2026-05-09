package javaforce.media;

/** Base class for MediaInput, MediaOutput
 *   and MediaEncoder, MediaDecoder
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.ffm.*;

public class MediaFormat extends MediaCoder {
  private FFMArray array = new FFMArray();
  /** Gets video stream index. */
  public int getVideoStream() {
    return MediaAPI.getInstance(array).getVideoStream(ctx);
  }

  /** Gets audio stream index. */
  public int getAudioStream() {
    return MediaAPI.getInstance(array).getAudioStream(ctx);
  }

  /** Gets video stream codec ID. */
  public int getVideoCodecID() {
    return MediaAPI.getInstance(array).getVideoCodecID(ctx);
  }

  /** Gets audio stream codec ID. */
  public int getAudioCodecID() {
    return MediaAPI.getInstance(array).getAudioCodecID(ctx);
  }

  /** Gets video stream bit rate. */
  public int getVideoBitRate() {
    return MediaAPI.getInstance(array).getVideoBitRate(ctx);
  }

  /** Gets audio stream bit rate. */
  public int getAudioBitRate() {
    return MediaAPI.getInstance(array).getAudioBitRate(ctx);
  }
}
