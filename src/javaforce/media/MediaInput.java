package javaforce.media;

/** MediaInput
 *
 * Read container media files.
 *
 * Container files such as mpeg, avi, mov, ogg.
 *
 * @author peter.quiring
 */

import javaforce.voip.*;
import javaforce.api.*;

public class MediaInput extends MediaFormat {
  public boolean open(String file, String format) {
    if (ctx != 0) return false;
    ctx = MediaAPI.getInstance().inputOpenFile(file, format);
    return ctx != 0;
  }

  public boolean open(MediaIO io) {
    if (ctx != 0) return false;
    ctx = MediaAPI.getInstance().inputOpenIO(io);
    return ctx != 0;
  }

  public long getDuration(long ctx) {
    return MediaAPI.getInstance().getDuration(ctx);
  }

  public int getVideoWidth(long ctx) {
    return MediaAPI.getInstance().getVideoWidth(ctx);
  }
  public int getVideoHeight(long ctx) {
    return MediaAPI.getInstance().getVideoHeight(ctx);
  }
  public float getVideoFrameRate(long ctx) {
    return MediaAPI.getInstance().getVideoFrameRate(ctx);
  }
  public int getVideoKeyFrameInterval(long ctx) {
    return MediaAPI.getInstance().getVideoKeyFrameInterval(ctx);
  }

  public int getAudioChannels(long ctx) {
    return MediaAPI.getInstance().getAudioChannels(ctx);
  }
  public int getAudioSampleRate(long ctx) {
    return MediaAPI.getInstance().getAudioSampleRate(ctx);
  }

  /** Returns media video/audio codec info. */
  public CodecInfo getCodecInfo() {
    CodecInfo info = new CodecInfo();
    info.duration = getDuration(ctx);
    if (getVideoStream() != -1) {
      //get video info
      info.width = getVideoWidth(ctx);
      info.height = getVideoHeight(ctx);
      info.fps = getVideoFrameRate(ctx);
      info.keyFrameInterval = getVideoKeyFrameInterval(ctx);
      info.video_bit_rate = getVideoBitRate();
      info.video_codec = getVideoCodecID();
      info.video_stream = getVideoStream();
    }
    if (getAudioStream() != -1) {
      //get audio info
      info.chs = getAudioChannels(ctx);
      info.freq = getAudioSampleRate(ctx);
      info.bits = 16;  //only 16bit supported for now
      info.audio_bit_rate = getAudioBitRate();
      info.audio_codec = getAudioCodecID();
      info.audio_stream = getAudioStream();
    }
    return info;
  }

  /** Closes media file and frees resources. */
  public boolean close() {
    if (ctx == 0) return false;
    boolean res = MediaAPI.getInstance().inputClose(ctx);
    ctx = 0;
    return res;
  }

  /** Create video decoder.
   *
   * @param width = desired width (-1 = no conversion)
   * @param height = desired height (-1 = no conversion)
   */
  public MediaVideoDecoder createVideoDecoder(int width, int height) {
    if (!MediaAPI.getInstance().inputOpenVideo(ctx, width, height)) return null;
    MediaVideoDecoder decoder = new MediaVideoDecoder(this);
    decoder.setStream(getVideoStream());
    return decoder;
  }

  /** Create video decoder. */
  public MediaVideoDecoder createVideoDecoder() {
    return createVideoDecoder(-1, -1);
  }

  /** Create audio decoder.
   *
   * @param chs = desired channels (-1 = no conversion)
   * @param freq = desired sample rate (-1 = no conversion)
   */
  public MediaAudioDecoder createAudioDecoder(int chs, int freq) {
    if (!MediaAPI.getInstance().inputOpenAudio(ctx, chs, freq)) return null;
    MediaAudioDecoder decoder = new MediaAudioDecoder(this);
    decoder.setStream(getAudioStream());
    return decoder;
  }

  /** Create audio decoder. */
  public MediaAudioDecoder createAudioDecoder() {
    return createAudioDecoder(-1, -1);
  }

  /** Reads next packet and returns size. */
  public int read(long ctx) {
    return MediaAPI.getInstance().inputRead(ctx);
  }

  /** Returns next packet key frame flag. */
  public boolean getPacketKeyFrame(long ctx) {
    return MediaAPI.getInstance().getPacketKeyFrame(ctx);
  }

  /** Copies next packet into data and returns stream. */
  public int getPacketData(long ctx, byte[] data, int offset, int length) {
    return MediaAPI.getInstance().getPacketData(ctx, data, offset, length);
  }

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
    int length = read(ctx);
    while (packet.data.length < length) {
      packet.data = new byte[packet.data.length << 1];
    }
    packet.length = length;
    packet.keyFrame = getPacketKeyFrame(ctx);  //only valid for video packets
    packet.stream = getPacketData(ctx, packet.data, 0, length);
    return packet;
  }

  public boolean seek(long seconds) {
    return MediaAPI.getInstance().inputSeek(ctx, seconds);
  }
}
