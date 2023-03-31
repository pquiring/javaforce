package javaforce.media;

/** Media encoder.
 *
 * @author pquiring
 */

public class MediaEncoder extends MediaCoder {
  //these must be set BEFORE you call start()
  public boolean fps_1000_1001 = false;
  public int framesPerKeyFrame = 12;
  public int videoBitRate = 400000;
  public int audioBitRate = 128000;
  public int compressionLevel = -1;
  public int profileLevel = 1;  //1=baseline 2=main 3=high
  public native boolean start(MediaIO io, int width, int height, int fps, int chs, int freq, String codec, boolean doVideo, boolean doAudio);
  /** Sets frame rate = fps * 1000 / 1001 (default = false) */
  public void set1000over1001(boolean state) {
    fps_1000_1001 = state;
  }
  /** Sets frames per key frame (gop) (default = 12) */
  public void setFramesPerKeyFrame(int count) {
    framesPerKeyFrame = count;
  }
  /** Sets video bit rate (default = 400000 bits/sec) */
  public void setVideoBitRate(int rate) {
    videoBitRate = rate;
  }
  /** Sets audio bit rate (default = 128000 bits/sec) */
  public void setAudioBitRate(int rate) {
    audioBitRate = rate;
  }
  /** Sets compression level (meaning varies per codec) (default = -1) */
  public void setCompressionLevel(int level) {
    compressionLevel = level;
  }
  /** Sets profile level (1=baseline 2=main 3=pro)*/
  public void setProfileLevel(int level) {
    profileLevel = level;
  }
  public native boolean addAudio(short sams[], int offset, int length);
  public boolean addAudio(short sams[]) {
    return addAudio(sams, 0, sams.length);
  }
  public native boolean addVideo(int px[]);
  public native int getAudioFramesize();
  /** Adds pre encoded audio. */
  public native boolean addAudioEncoded(byte packet[], int offset, int length);
  /** Adds pre encoded video. */
  public native boolean addVideoEncoded(byte[] packet, int offset, int length, boolean key_frame);
  public native void flush();
  public native void stop();
  /** Returns codecs mimetype. */
  public String getCodecMimeType(String codec, boolean doVideo, boolean doAudio) {
    //see https://developer.mozilla.org/en-US/docs/Web/Media/Formats/codecs_parameter
    StringBuilder mime = new StringBuilder();
    switch (codec) {
      case "dash":
      case "mp4":
        mime.append("video/mp4;codecs=\"");
        if (doAudio) {
          //MP4.OO[.A] audio
          //  OO = type (40=audio)
          //  A = subtype (2=AAC-LC, 34=MP3)
          mime.append("mp4a.40.2");
        }
        if (doVideo) {
          if (doAudio) mime.append(",");
          //avc1.PPCCLL (h264 video codec)
          //  PP = profile (42=baseline, 4D=main, 64=high)
          //  CC = constraints (40=constraits, 00=none)
          //  LL = level (28=???)
          switch (profileLevel) {
            case 1: mime.append("avc1.420028"); break;
            case 2: mime.append("avc1.4D0028"); break;
            case 3: mime.append("avc1.640028"); break;
          }
        }
        mime.append("\"");
        break;
      case "webm":
        mime.append("video/webm;codecs=\"");
        if (doAudio) {
          if (true) {
            mime.append("opus");
          } else {
            mime.append("vorbis");
          }
        }
        if (doVideo) {
          //vp##.PP.LL.DD
          //  ## = 08,09,10
          //  PP = 0-3
          //  LL = level (10=1.0)
          //  DD = bit depth (08,10,12)
          if (doAudio) mime.append(",");
          mime.append("vp09.00.10.08");
        }
        mime.append("\"");
        break;
    }
    return mime.toString();
  }
}
