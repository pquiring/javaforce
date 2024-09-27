package javaforce.media;

/** MediaInput
 *
 * Read multi-media files using ffmpeg.
 *
 * @author peter.quiring
 */

import javaforce.voip.*;

public class MediaInput extends MediaFormat {
  private static native long nopenFile(String file, String format);

  public boolean open(String file, String format) {
    if (ctx != 0) return false;
    ctx = nopenFile(file, format);
    return ctx != 0;
  }

  private static native long nopenIO(MediaIO io);

  public boolean open(MediaIO io) {
    if (ctx != 0) return false;
    ctx = nopenIO(io);
    return ctx != 0;
  }

  private static native long ngetDuration(long ctx);

  private static native int ngetVideoWidth(long ctx);
  private static native int ngetVideoHeight(long ctx);
  private static native float ngetVideoFrameRate(long ctx);
  private static native int ngetVideoKeyFrameInterval(long ctx);

  private static native int ngetAudioChannels(long ctx);
  private static native int ngetAudioSampleRate(long ctx);

  /** Returns media video/audio codec info. */
  public CodecInfo getCodecInfo() {
    CodecInfo info = new CodecInfo();
    info.duration = ngetDuration(ctx);
    if (getVideoStream() != -1) {
      //get video info
      info.width = ngetVideoWidth(ctx);
      info.height = ngetVideoHeight(ctx);
      info.fps = ngetVideoFrameRate(ctx);
      info.keyFrameInterval = ngetVideoKeyFrameInterval(ctx);
      info.video_bit_rate = getVideoBitRate();
      info.video_codec = getVideoCodecID();
      info.video_stream = getVideoStream();
    }
    if (getAudioStream() != -1) {
      //get audio info
      info.chs = ngetAudioChannels(ctx);
      info.freq = ngetAudioSampleRate(ctx);
      info.bits = 16;  //only 16bit supported for now
      info.audio_bit_rate = getAudioBitRate();
      info.audio_codec = getAudioCodecID();
      info.audio_stream = getAudioStream();
    }
    return info;
  }

  private static native boolean nclose(long ctx);

  /** Closes media file and frees resources. */
  public boolean close() {
    if (ctx == 0) return false;
    boolean res = nclose(ctx);
    ctx = 0;
    return res;
  }

  private static native boolean nopenvideo(long ctx, int width, int height);

  /** Create video decoder.
   *
   * @param width = desired width (-1 = no conversion)
   * @param height = desired height (-1 = no conversion)
   */
  public MediaVideoDecoder createVideoDecoder(int width, int height) {
    if (!nopenvideo(ctx, width, height)) return null;
    MediaVideoDecoder decoder = new MediaVideoDecoder();
    decoder.setStream(getVideoStream());
    decoder.start(getVideoCodecID(), width, height);
    return decoder;
  }

  /** Create video decoder. */
  public MediaVideoDecoder createVideoDecoder() {
    return createVideoDecoder(-1, -1);
  }

  private static native boolean nopenaudio(long ctx, int chs, int freq);

  /** Create audio decoder.
   *
   * @param chs = desired channels (-1 = no conversion)
   * @param freq = desired sample rate (-1 = no conversion)
   */
  public MediaAudioDecoder createAudioDecoder(int chs, int freq) {
    if (!nopenaudio(ctx, chs, freq)) return null;
    MediaAudioDecoder decoder = new MediaAudioDecoder();
    decoder.setStream(getAudioStream());
    decoder.start(getAudioCodecID(), chs, freq);
    return decoder;
  }

  /** Create audio decoder. */
  public MediaAudioDecoder createAudioDecoder() {
    return createAudioDecoder(-1, -1);
  }

  /** Reads next packet and returns size. */
  private static native int nread(long ctx);
  /** Returns next packet key frame flag. */
  private static native boolean ngetPacketKeyFrame(long ctx);
  /** Copies next packet into data and returns stream. */
  private static native int ngetPacketData(long ctx, byte[] data, int offset, int length);

  private Packet packet;

  /** Reads next packet.
   *
   * Packet.stream will indicate which stream it is.
   */
  public Packet readPacket() {
    if (ctx == 0) return null;
    if (packet == null) {
      packet = new Packet();
      packet.data = new byte[64 * 1024];
    }
    int length = nread(ctx);
    while (packet.data.length < length) {
      packet.data = new byte[packet.data.length << 1];
    }
    packet.length = length;
    packet.keyFrame = ngetPacketKeyFrame(ctx);  //only valid for video packets
    packet.stream = ngetPacketData(ctx, packet.data, 0, length);
    return packet;
  }

  private native boolean nseek(long ctx, long seconds);
  public boolean seek(long seconds) {
    return nseek(ctx, seconds);
  }
}
