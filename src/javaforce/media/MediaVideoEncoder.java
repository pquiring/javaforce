package javaforce.media;

/** Media video encoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaVideoEncoder extends MediaCoder {
  /** Create a video encoder for raw video. */
  public MediaVideoEncoder() {}
  /** Create a video encoder for a stream in a container format. */
  public MediaVideoEncoder(MediaOutput output) {
    this.ctx = output.ctx;
    shared = true;
  }
  private native long nstart(int codec_id, int bit_rate, int width, int height, float fps, int keyFrameInterval);
  public boolean start(CodecInfo info) {
    if (ctx != 0 || shared) return false;
    ctx = nstart(info.video_codec, info.video_bit_rate, info.width, info.height, info.fps, info.keyFrameInterval);
    return ctx != 0;
  }
  private native void nstop(long ctx);
  public void stop() {
    if (ctx == 0 || shared) return;
    nstop(ctx);
    ctx = 0;
  }
  private Packet packet;
  private native byte[] nencode(long ctx, int[] px, int offset, int length);
  public Packet encode(int[] px, int offset, int length) {
    if (ctx == 0) return null;
    if (packet == null) {
      packet = new Packet();
      packet.stream = getStream();
    }
    packet.data = nencode(ctx, px, offset, length);
    if (packet.data == null) return null;
    packet.length = packet.data.length;
    return packet;
  }
}
