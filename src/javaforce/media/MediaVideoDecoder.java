package javaforce.media;

/** Media video decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;
import javaforce.api.*;

public class MediaVideoDecoder extends MediaCoder {
  /** Create a stand-alone video decoder for raw video. */
  public MediaVideoDecoder() {}
  /** Create a video decoder for a stream in a container format. */
  public MediaVideoDecoder(MediaInput input) {
    this.ctx = input.ctx;
    shared = true;
  }
  /** Starts a stand-alone video decoder. */
  public boolean start(int codec_id, int new_width, int new_height) {
    if (ctx != 0 || shared) return false;
    ctx = MediaAPI.getInstance().videoDecoderStart(codec_id, new_width, new_height);
    return ctx != 0;
  }
  /** Stops a stand-alone video decoder. */
  public void stop() {
    if (ctx == 0 || shared) return;
    MediaAPI.getInstance().videoDecoderStop(ctx);
    ctx = 0;
  }
  /** Decodes codec packet into raw video frame. */
  public int[] decode(byte[] data, int offset, int length) {
    if (ctx == 0) return null;
    return MediaAPI.getInstance().videoDecoderDecode(ctx, data, offset, length);
  }
  /** Decodes codec packet into raw video frame. */
  public int[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  /** Returns video width. */
  public int getWidth() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().videoDecoderGetWidth(ctx);
  }
  /** Returns video height. */
  public int getHeight() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().videoDecoderGetHeight(ctx);
  }
  /** Returns video frame rate (per second). */
  public float getFrameRate() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().videoDecoderGetFrameRate(ctx);
  }
  /** Changes output width/height only.  All other fields ignored. */
  public boolean change(CodecInfo info) {
    return MediaAPI.getInstance().videoDecoderChange(ctx, info.width, info.height);
  }
}
