package javaforce.media;

/** Media video decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaVideoDecoder extends MediaCoder {
  /** Create a video decoder for raw video. */
  public MediaVideoDecoder() {}
  /** Create a video decoder for a stream in a container format. */
  public MediaVideoDecoder(MediaInput input) {
    this.ctx = input.ctx;
    shared = true;
  }
  public boolean start(int codec_id, int new_width, int new_height) {
    if (ctx != 0 || shared) return false;
    ctx = MediaAPI.getInstance().videoDecoderStart(codec_id, new_width, new_height);
    return ctx != 0;
  }
  public void stop() {
    if (ctx == 0 || shared) return;
    MediaAPI.getInstance().videoDecoderStop(ctx);
    ctx = 0;
  }
  public int[] decode(byte[] data, int offset, int length) {
    if (ctx == 0) return null;
    return MediaAPI.getInstance().videoDecoderDecode(ctx, data, offset, length);
  }
  public int[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  public int getWidth() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().videoDecoderGetWidth(ctx);
  }
  public int getHeight() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().videoDecoderGetHeight(ctx);
  }
  public float getFrameRate() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().videoDecoderGetFrameRate(ctx);
  }
  /** Changes output width/height only.  All other fields ignored. */
  public boolean change(CodecInfo info) {
    return MediaAPI.getInstance().videoDecoderChange(ctx, info.width, info.height);
  }
}
