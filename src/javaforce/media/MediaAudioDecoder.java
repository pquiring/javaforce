package javaforce.media;

/** Media audio decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;
import javaforce.api.*;

public class MediaAudioDecoder extends MediaCoder {
  /** Create an audio decoder for raw audio. */
  public MediaAudioDecoder() {}
  /** Create an audio decoder for a stream in a container format. */
  public MediaAudioDecoder(MediaInput input) {
    this.ctx = input.ctx;
    shared = true;
  }
  public boolean start(int codec_id, int new_chs, int new_freq) {
    if (ctx != 0 || shared) return false;
    ctx = MediaAPI.getInstance().audioDecoderStart(codec_id, new_chs, new_freq);
    return ctx != 0;
  }
  public void stop() {
    if (ctx == 0 || shared) return;
    MediaAPI.getInstance().audioDecoderStop(ctx);
    ctx = 0;
  }
  public short[] decode(byte[] data, int offset, int length) {
    if (ctx == 0) return null;
    return MediaAPI.getInstance().audioDecoderDecode(ctx, data, offset, length);
  }
  public short[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  public int getChannels() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().audioDecoderGetChannels(ctx);
  }
  public int getSampleRate() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance().audioDecoderGetSampleRate(ctx);
  }
  /** Changes output chs/freq only.  All other fields ignored. */
  public boolean change(CodecInfo info) {
    return MediaAPI.getInstance().audioDecoderChange(ctx, info.chs, info.freq);
  }
}
