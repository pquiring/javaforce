package javaforce.media;

/** Media "raw" video decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaVideoDecoder extends MediaCoder {
  public native long nstart(int codec_id, int new_width, int new_height);
  public boolean start(int codec_id, int new_width, int new_height) {
    if (ctx != 0) return false;
    ctx = nstart(codec_id, new_width, new_height);
    return ctx != 0;
  }
  public native void nstop(long ctx);
  public boolean stop() {
    if (ctx == 0) return false;
    nstop(ctx);
    ctx = 0;
    return true;
  }
  public native int[] ndecode(long ctx, byte[] data, int offset, int length);
  public int[] decode(byte[] data, int offset, int length) {
    return ndecode(ctx, data, offset, length);
  }
  public int[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  public native int ngetWidth(long ctx);
  public int getWidth() {
    return ngetWidth(ctx);
  }
  public native int ngetHeight(long ctx);
  public int getHeight() {
    return ngetHeight(ctx);
  }
  public native float ngetFrameRate(long ctx);
  public float getFrameRate() {
    return ngetFrameRate(ctx);
  }
}
