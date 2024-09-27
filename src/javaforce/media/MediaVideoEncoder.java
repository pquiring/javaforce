package javaforce.media;

/** Media video encoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaVideoEncoder extends MediaCoder {
  public native long nstart(int codec_id, int bit_rate, int width, int height, float fps, int keyFrameInterval);
  public boolean start(CodecInfo info) {
    if (ctx != 0) return false;
    ctx = nstart(info.video_codec, info.video_bit_rate, info.width, info.height, info.fps, info.keyFrameInterval);
    return ctx != 0;
  }
  public native void nstop(long ctx);
  public void stop() {
    if (ctx == 0) return;
    nstop(ctx);
    ctx = 0;
  }
  public native byte[] nencode(long ctx, int[] px, int offset, int length);
  public byte[] encode(int[] px, int offset, int length) {
    if (ctx == 0) return null;
    return nencode(ctx, px, offset, length);
  }
}
