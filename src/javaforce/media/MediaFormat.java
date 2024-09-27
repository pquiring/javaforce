package javaforce.media;

/** Base class for MediaInput, MediaOutput
 *   and MediaEncoder, MediaDecoder
 *
 * @author pquiring
 */

public class MediaFormat extends MediaCoder {
  private static native int ngetVideoStream(long ctx);

  public int getVideoStream() {
    return ngetVideoStream(ctx);
  }

  private static native int ngetAudioStream(long ctx);

  public int getAudioStream() {
    return ngetAudioStream(ctx);
  }

  private static native int ngetVideoCodecID(long ctx);

  public int getVideoCodecID() {
    return ngetVideoCodecID(ctx);
  }

  private static native int ngetAudioCodecID(long ctx);

  public int getAudioCodecID() {
    return ngetAudioCodecID(ctx);
  }

  private static native int ngetVideoBitRate(long ctx);

  public int getVideoBitRate() {
    return ngetVideoBitRate(ctx);
  }

  private static native int ngetAudioBitRate(long ctx);

  public int getAudioBitRate() {
    return ngetAudioBitRate(ctx);
  }
}
