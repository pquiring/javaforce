package javaforce.media;

/** Media audio encoder.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.voip.*;

public class MediaAudioEncoder extends MediaCoder {
  /** Create an audio encoder for raw audio. */
  public MediaAudioEncoder() {}
  /** Create an audio encoder for a stream in a container format. */
  public MediaAudioEncoder(MediaOutput output) {
    this.ctx = output.ctx;
    shared = true;
  }
  public boolean start(CodecInfo info) {
    if (ctx != 0 || shared) return false;
    ctx = MediaAPI.getInstance().audioEncoderStart(info.audio_codec, info.audio_bit_rate, info.chs, info.freq);
    return ctx != 0;
  }
  public void stop() {
    if (ctx == 0 || shared) return;
    MediaAPI.getInstance().audioEncoderStop(ctx);
    ctx = 0;
  }
  private Packet packet;
  public Packet encode(short[] samples, int offset, int length) {
    if (ctx == 0) {
      JFLog.log("MediaAudioEncoder no ctx");
      return null;
    }
    if (packet == null) {
      packet = new Packet();
      packet.stream = getStream();
    }
    packet.data = MediaAPI.getInstance().audioEncoderEncode(ctx, samples, offset, length);
    if (packet.data == null) {
      JFLog.log("MediaAudioEncoder.nencode:data == null");
      return null;
    }
    packet.length = packet.data.length;
    return packet;
  }
  public int getAudioFramesize() {
    return MediaAPI.getInstance().audioEncoderGetAudioFramesize(ctx);
  }
}
