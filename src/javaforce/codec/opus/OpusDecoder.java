package javaforce.codec.opus;

/** opus decoder
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.media.*;

public class OpusDecoder {

  private MediaAudioDecoder decoder;

  public void open() {
    decoder = new MediaAudioDecoder();
    decoder.start(MediaCoder.AV_CODEC_ID_OPUS, 1, 48000);
  }

  public void close() {
    if (decoder != null) {
      decoder.stop();
      decoder = null;
    }
  }

  public short[] decode(byte[] data, int off, int length) {
    while (true) {
      short[] sams = decoder.decode(data, off, length);
      if (sams == null || sams.length == 0) {
        JFLog.log("OpusDecoder:packet == null");
        break;
      }
      return sams;
    }
    return null;
  }
}
