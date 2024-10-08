package javaforce.media;

/** Media audio decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaAudioDecoder extends MediaCoder {
  public MediaAudioDecoder() {}
  public MediaAudioDecoder(MediaInput input) {
    this.ctx = input.ctx;
    shared = true;
  }
  public native long nstart(int codec_id, int new_chs, int new_freq);
  public boolean start(int codec_id, int new_chs, int new_freq) {
    if (ctx != 0 || shared) return false;
    ctx = nstart(codec_id, new_chs, new_freq);
    return ctx != 0;
  }
  public native void nstop(long ctx);
  public void stop() {
    if (ctx == 0 || shared) return;
    nstop(ctx);
    ctx = 0;
  }
  public native short[] ndecode(long ctx, byte[] data, int offset, int length);
  public short[] decode(byte[] data, int offset, int length) {
    if (ctx == 0) return null;
    return ndecode(ctx, data, offset, length);
  }
  public short[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  public native int ngetChannels(long ctx);
  public int getChannels() {
    if (ctx == 0) return -1;
    return ngetChannels(ctx);
  }
  public native int ngetSampleRate(long ctx);
  public int getSampleRate() {
    if (ctx == 0) return -1;
    return ngetSampleRate(ctx);
  }
}
