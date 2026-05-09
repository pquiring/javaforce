package javaforce.media;

/** Media audio encoder.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.voip.*;
import javaforce.api.*;
import javaforce.ffm.*;

public class MediaAudioEncoder extends MediaCoder {
  private FFMArray array = new FFMArray();
  /** Create a stand-alone audio encoder for raw audio. */
  public MediaAudioEncoder() {}
  /** Create an audio encoder for a stream in a container format. */
  public MediaAudioEncoder(MediaOutput output) {
    this.ctx = output.ctx;
    shared = true;
  }
  /** Starts a stand-alone audio encoder. */
  public boolean start(CodecInfo info) {
    if (ctx != 0 || shared) return false;
    ctx = MediaAPI.getInstance(array).audioEncoderStart(info.audio_codec, info.audio_bit_rate, info.chs, info.freq);
    return ctx != 0;
  }
  /** Stops a stand-alone audio encoder. */
  public void stop() {
    if (ctx == 0 || shared) return;
    MediaAPI.getInstance(array).audioEncoderStop(ctx);
    ctx = 0;
  }
  private Packet packet;
  /** Encodes raw audio data and returns packet in codec format. */
  public Packet encode(short[] samples, int offset, int length) {
    if (ctx == 0) {
      JFLog.log("MediaAudioEncoder no ctx");
      return null;
    }
    if (packet == null) {
      packet = new Packet();
      packet.stream = getStream();
    }
    packet.data = MediaAPI.getInstance(array).audioEncoderEncode(ctx, samples, offset, length);
    if (packet.data == null) {
      JFLog.log("MediaAudioEncoder.nencode:data == null");
      return null;
    }
    packet.length = packet.data.length;
    return packet;
  }
  /** Returns optimal audio frame size in samples. */
  public int getAudioFramesize() {
    return MediaAPI.getInstance(array).audioEncoderGetAudioFramesize(ctx);
  }
}
