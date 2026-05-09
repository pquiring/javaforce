package javaforce.media;

/** Media audio decoder.
 *
 * @author pquiring
 */

import javaforce.voip.*;
import javaforce.api.*;
import javaforce.ffm.*;

public class MediaAudioDecoder extends MediaCoder {
  private FFMArray array = new FFMArray();
  /** Create a stand-alone audio decoder for raw audio. */
  public MediaAudioDecoder() {}
  /** Create an audio decoder for a stream in a container format. */
  public MediaAudioDecoder(MediaInput input) {
    this.ctx = input.ctx;
    shared = true;
  }
  /** Starts a stand-alone audio decoder. */
  public boolean start(int codec_id, int new_chs, int new_freq) {
    if (ctx != 0 || shared) return false;
    ctx = MediaAPI.getInstance(array).audioDecoderStart(codec_id, new_chs, new_freq);
    return ctx != 0;
  }
  /** Stops a stand-alone audio decoder. */
  public void stop() {
    if (ctx == 0 || shared) return;
    MediaAPI.getInstance(array).audioDecoderStop(ctx);
    ctx = 0;
  }
  /** Decodes codec packet into raw audio frame. */
  public short[] decode(byte[] data, int offset, int length) {
    if (ctx == 0) return null;
    return MediaAPI.getInstance(array).audioDecoderDecode(ctx, data, offset, length);
  }
  /** Decodes codec packet into raw audio frame. */
  public short[] decode(Packet packet) {
    return decode(packet.data, packet.offset, packet.length);
  }
  /** Returns number of audio channels. */
  public int getChannels() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance(array).audioDecoderGetChannels(ctx);
  }
  /** Returns audio sample rate. */
  public int getSampleRate() {
    if (ctx == 0) return -1;
    return MediaAPI.getInstance(array).audioDecoderGetSampleRate(ctx);
  }
  /** Changes output chs/freq only.  All other fields ignored. */
  public boolean change(CodecInfo info) {
    return MediaAPI.getInstance(array).audioDecoderChange(ctx, info.chs, info.freq);
  }
}
