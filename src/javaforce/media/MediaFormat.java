package javaforce.media;

/** Base class for MediaInput, MediaOutput
 *
 * TODO : move MediaCoder.AV_FORMAT_... here!
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
}
