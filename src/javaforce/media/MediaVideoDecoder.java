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
  private native long nstart(int codec_id, int new_width, int new_height);
  public boolean start(int codec_id, int new_width, int new_height) {
    if (ctx != 0 || shared) return false;
    ctx = nstart(codec_id, new_width, new_height);
    return ctx != 0;
  }
  private native void nstop(long ctx);
  public void stop() {
    if (ctx == 0 || shared) return;
    nstop(ctx);
    ctx = 0;
  }
  private native int[] ndecode(long ctx, byte[] data, int offset, int length);
  public int[] decode(byte[] data, int offset, int length) {
    if (ctx == 0) return null;
    return ndecode(ctx, data, offset, length);
  }
  public int[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  private native int ngetWidth(long ctx);
  public int getWidth() {
    if (ctx == 0) return -1;
    return ngetWidth(ctx);
  }
  private native int ngetHeight(long ctx);
  public int getHeight() {
    if (ctx == 0) return -1;
    return ngetHeight(ctx);
  }
  private native float ngetFrameRate(long ctx);
  public float getFrameRate() {
    if (ctx == 0) return -1;
    return ngetFrameRate(ctx);
  }
  private native boolean nchange(long ctx, int width, int height);
  /** Changes output width/height only.  All other fields ignored. */
  public boolean change(CodecInfo info) {
    return nchange(ctx, info.width, info.height);
  }
}
