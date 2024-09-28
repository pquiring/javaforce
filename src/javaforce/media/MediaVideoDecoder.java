package javaforce.media;

/** Media "raw" video decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaVideoDecoder extends MediaCoder {
  public MediaVideoDecoder() {}
  public MediaVideoDecoder(MediaInput input) {
    this.ctx = input.ctx;
    shared = true;
  }
  public native long nstart(int codec_id, int new_width, int new_height);
  public boolean start(int codec_id, int new_width, int new_height) {
    if (ctx != 0 || shared) return false;
    ctx = nstart(codec_id, new_width, new_height);
    return ctx != 0;
  }
  public native void nstop(long ctx);
  public void stop() {
    if (ctx == 0 || shared) return;
    nstop(ctx);
    ctx = 0;
  }
  public native int[] ndecode(long ctx, byte[] data, int offset, int length);
  public int[] decode(byte[] data, int offset, int length) {
    if (ctx == 0) return null;
    return ndecode(ctx, data, offset, length);
  }
  public int[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  public native int ngetWidth(long ctx);
  public int getWidth() {
    if (ctx == 0) return -1;
    return ngetWidth(ctx);
  }
  public native int ngetHeight(long ctx);
  public int getHeight() {
    if (ctx == 0) return -1;
    return ngetHeight(ctx);
  }
  public native float ngetFrameRate(long ctx);
  public float getFrameRate() {
    if (ctx == 0) return -1;
    return ngetFrameRate(ctx);
  }
}
