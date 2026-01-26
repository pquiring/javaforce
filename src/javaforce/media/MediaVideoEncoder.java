package javaforce.media;

/** Media video encoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;
import javaforce.api.*;

public class MediaVideoEncoder extends MediaCoder {
  /** Create a video encoder for raw video. */
  public MediaVideoEncoder() {}
  /** Create a video encoder for a stream in a container format. */
  public MediaVideoEncoder(MediaOutput output) {
    this.ctx = output.ctx;
    shared = true;
  }
  public boolean start(CodecInfo info) {
    if (ctx != 0 || shared) return false;
    ctx = MediaAPI.getInstance().videoEncoderStart(info.video_codec, info.video_bit_rate, info.width, info.height, info.fps, info.keyFrameInterval);
    return ctx != 0;
  }
  public void stop() {
    if (ctx == 0 || shared) return;
    MediaAPI.getInstance().videoEncoderStop(ctx);
    ctx = 0;
  }
  private Packet packet;
  public Packet encode(int[] px, int offset, int length) {
    if (ctx == 0) return null;
    if (packet == null) {
      packet = new Packet();
      packet.stream = getStream();
    }
    packet.data = MediaAPI.getInstance().videoEncoderEncode(ctx, px, offset, length);
    if (packet.data == null) return null;
    packet.length = packet.data.length;
    return packet;
  }
}
