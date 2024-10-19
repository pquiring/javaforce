package javaforce.media;

/** MediaOutput
 *
 * Create multi-media files using ffmpeg.
 *
 * Notes:
 *  - ffmpeg cli can be queried to determine default codecs for format.
 *    ffmpeg --help muxer={format}
 *  - Stream order is always video then audio.
 *
 * @author peter.quiring
 */

import javaforce.*;
import javaforce.voip.*;

public class MediaOutput extends MediaFormat {
  private static native long ncreateFile(String file, String format);

  /** Create output file.
   * @param file = filename
   * @param format = media container (see MediaCoder.AV_FORMAT_ID...)
   */
  public boolean create(String file, String format) {
    if (ctx != 0) return false;
    ctx = ncreateFile(file, format);
    if (ctx == 0) {
      JFLog.log("MediaOutput.ncreateFile() == 0");
    }
    return ctx != 0;
  }

  private static native long ncreateIO(MediaIO io, String format);

  /** Create output media via MediaIO.
   * @param io = Media IO interface
   * @param format = media container (see MediaCoder.AV_FORMAT_ID...)
   */
  public boolean create(MediaIO io, String format) {
    if (ctx != 0) return false;
    ctx = ncreateIO(io, format);
    if (ctx == 0) {
      JFLog.log("MediaOutput.ncreateIO() == 0");
    }
    return ctx != 0;
  }

  private static native int naddVideoStream(long ctx, int codec_id, int bit_rate, int width, int height, float fps, int keyFrameInterval);

  /** Adds a video stream to output.
   * All media streams must be added before calling write()
   *
   * @param info.width/height = video dimension
   *        info.fps = frames per second
   *        info.keyFrameInterval = key frame interval
   *        info.video_codec = video codec (0=default for format)
   *        info.video_bit_rate = video bit rate (default = 1Mb/s)
   * @return true if successful
   *         info.stream = stream index
   *         info.video_codec = selected codec
   */
  public boolean addVideoStream(CodecInfo info) {
    info.video_stream = naddVideoStream(ctx, info.video_codec, info.video_bit_rate, info.width, info.height, info.fps, info.keyFrameInterval);
    if (info.video_stream == -1) {
      JFLog.log("addVideoStream == -1");
      return false;
    }
    if (info.video_codec == 0) {
      info.video_codec = getVideoCodecID();
    }
    return true;
  }

  private static native int naddAudioStream(long ctx, int codec_id, int bit_rate, int chs, int freq);

  /** Adds an audio stream to output.
   * All media streams must be added before calling write()
   *
   * @param info.chs = audio channels
   *        info.freq = audio sample rate
   *        info.audio_codec = audio codec (0=default for format)
   *        info.audio_bit_rate = audio bit rate (default = 128Kb/s)
   * @return true is successful
   *         info.stream = stream index
   *         info.audio_codec = selected codec
   */
  public boolean addAudioStream(CodecInfo info) {
    info.audio_stream = naddAudioStream(ctx, info.audio_codec, info.audio_bit_rate, info.chs, info.freq);
    if (info.audio_stream == -1) {
      JFLog.log("addAudioStream == -1");
      return false;
    }
    if (info.audio_codec == 0) {
      info.audio_codec = getAudioCodecID();
    }
    return true;
  }

  /** Creates a video stream by invoking addVideoStream() and returns a video encoder for new stream.
   */
  public MediaVideoEncoder createVideoEncoder(CodecInfo info) {
    if (!addVideoStream(info)) return null;
    MediaVideoEncoder encoder = new MediaVideoEncoder(this);
    encoder.setStream(info.video_stream);
    return encoder;
  }

  /** Creates an audio stream by invoke addAudioStream() and returns an audio encoder for new stream.
   */
  public MediaAudioEncoder createAudioEncoder(CodecInfo info) {
    if (!addAudioStream(info)) return null;
    MediaAudioEncoder encoder = new MediaAudioEncoder(this);
    encoder.setStream(info.audio_stream);
    return encoder;
  }

  private static native boolean nclose(long ctx);

  /** Closes media file and frees resources. */
  public boolean close() {
    if (ctx == 0) return false;
    boolean res = nclose(ctx);
    ctx = 0;
    return res;
  }

  private boolean header;

  private static native boolean nwriteHeader(long ctx);
  private static native boolean nwritePacket(long ctx, int stream, byte[] data, int offset, int length, boolean keyFrame);

  public boolean writePacket(Packet packet) {
    if (!header) {
      //write header must be called after streams are added
      nwriteHeader(ctx);
      header = true;
    }
    return nwritePacket(ctx, packet.stream, packet.data, packet.offset, packet.length, packet.keyFrame);
  }
}
