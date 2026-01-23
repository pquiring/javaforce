package javaforce.media;

/** Base class for Media Coders.
 *
 * @author pquiring
 */

public class MediaCoder {
  protected long ctx = 0;
  protected boolean shared;
  private int stream;

  public int getStream() {
    return stream;
  }

  public void setStream(int stream) {
    this.stream = stream;
  }

  /** Returns state of coder.  If true you MUST call stop() or close() to free resources. */
  public boolean isRunning() {
    return ctx != 0;
  }

  public static void init() {
    MediaAPI.getInstance().init();
  }

  public static void setLogging(boolean state) {
    MediaAPI.getInstance().setLogging(state);
  }

  //seek types
  public static final int SEEK_SET = 0;
  public static final int SEEK_CUR = 1;
  public static final int SEEK_END = 2;

  //profile levels
  public static final int PROFILE_BASELINE = 1;  //default
  public static final int PROFILE_MAIN = 2;
  public static final int PROFILE_HIGH = 3;

  //formats (containers)
  //mpeg
  public static final String AV_FORMAT_ID_MP1 = "mp1";
  public static final String AV_FORMAT_ID_MP2 = "mp2";
  //note : mp3 was skipped because of possible confusion with mp1.3 (aka mp3)
  public static final String AV_FORMAT_ID_MP4 = "mp4";
  //MS
  public static final String AV_FORMAT_ID_AVI = "avi";
  //open
  public static final String AV_FORMAT_ID_MKV = "mkv";
  public static final String AV_FORMAT_ID_OGG = "ogg";
  public static final String AV_FORMAT_ID_WEBM = "webm";
  //audio only
  public static final String AV_FORMAT_ID_WAV = "wav";
  //apple
  public static final String AV_FORMAT_ID_MOV = "mov";

  /** No codec (for encoding only) */
  public static final int AV_CODEC_ID_NONE = 0;
  /** Select default codec for format (for encoding only) */
  public static final int AV_CODEC_ID_DEFAULT = -1;

  //video codecs (VideoDecoder)
  public static final int AV_CODEC_ID_MPEG1VIDEO = 1;
  public static final int AV_CODEC_ID_MPEG2VIDEO = 2;
  public static final int AV_CODEC_ID_H263 = 4;
  public static final int AV_CODEC_ID_MPEG4 = 12;
  public static final int AV_CODEC_ID_H264 = 27;
  public static final int AV_CODEC_ID_THEORA = 30;
  public static final int AV_CODEC_ID_VP8 = 139;
  public static final int AV_CODEC_ID_VP9 = 167;
  public static final int AV_CODEC_ID_H265 = 173;

  //audio codecs
  public static final int AV_CODEC_ID_PCM_S16LE = 0x10000;  //wav file
  public static final int AV_CODEC_ID_MP2 = 0x15000;
  public static final int AV_CODEC_ID_MP3 = 0x15001;
  public static final int AV_CODEC_ID_AAC = 0x15002;
  public static final int AV_CODEC_ID_AC3 = 0x15003;
  public static final int AV_CODEC_ID_VORBIS = 0x15005;
  public static final int AV_CODEC_ID_FLAC = 0x1500c;
  public static final int AV_CODEC_ID_GSM_MS = 0x1501e;
  public static final int AV_CODEC_ID_SPEEX = 0x15023;
  public static final int AV_CODEC_ID_OPUS = 0x1503c;

  //returned by MediaDecoder.read()
  public static final int END_FRAME = -1;
  public static final int NULL_FRAME = 0;  //could be metadata frame
  public static final int AUDIO_FRAME = 1;
  public static final int VIDEO_FRAME = 2;
}
