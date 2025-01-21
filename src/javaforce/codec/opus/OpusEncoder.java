package javaforce.codec.opus;

/** opus encoder
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.media.*;
import javaforce.voip.*;

public class OpusEncoder {

  private MediaAudioEncoder encoder;

  public void open() {
    encoder = new MediaAudioEncoder();
    CodecInfo info = new CodecInfo();
    info.freq = 48000;
    info.bits = 16;
    info.chs = 1;
    info.audio_codec = MediaCoder.AV_CODEC_ID_OPUS;
    info.audio_bit_rate = 128 * 1024;
    encoder.start(info);
  }

  public void close() {
    if (encoder != null) {
      encoder.stop();
      encoder = null;
    }
  }

  public byte[] encode(short[] data) {
    while (true) {
      Packet packet = encoder.encode(data, 0, data.length);
      if (packet == null || packet.length == 0) {
        JFLog.log("OpusEncoder:packet == null");
        break;
      }
      return packet.data;
    }
    return null;
  }
}
