package javaforce.media;

/** Media audio encoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;

public class MediaAudioEncoder extends MediaCoder {
  public MediaAudioEncoder() {}
  public MediaAudioEncoder(MediaOutput output) {
    this.ctx = output.ctx;
    shared = true;
  }
  public native long nstart(int codec_id, int bit_rate, int chs, int freq);
  public boolean start(CodecInfo info) {
    if (ctx != 0 || shared) return false;
    ctx = nstart(info.audio_codec, info.audio_bit_rate, info.chs, info.freq);
    return ctx != 0;
  }
  public native void nstop(long ctx);
  public void stop() {
    if (ctx == 0 || shared) return;
    nstop(ctx);
    ctx = 0;
  }
  private Packet packet;
  public native byte[] nencode(long ctx, short[] samples, int offset, int length);
  public Packet encode(short[] samples, int offset, int length) {
    if (ctx == 0) return null;
    if (packet == null) {
      packet = new Packet();
      packet.stream = getStream();
    }
    packet.data = nencode(ctx, samples, offset, length);
    if (packet.data == null) return null;
    packet.length = packet.data.length;
    return packet;
  }
  public native int ngetAudioFramesize(long ctx);
  public int getAudioFramesize() {
    return ngetAudioFramesize(ctx);
  }
}
