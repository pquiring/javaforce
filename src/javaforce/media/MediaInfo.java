package javaforce.media;

import java.io.*;

import javaforce.*;
import javaforce.voip.*;

/** MediaInfo.
 *
 * @author pquiring
 */

public class MediaInfo implements MediaIO {
  public static void main(String[] args) {
    new MediaInfo().run(args);
  }

  private RandomAccessFile file;

  public void run(String[] args) {
    if (args.length == 0) {
      print("MediaFormat : Displays info about media file.");
      print("Usage : MediaFormat {media_file}");
      return;
    }
    try {
      file = new RandomAccessFile(args[0], "r");
    } catch (Exception e) {
      JFLog.log(e);
    }
    MediaInput input = new MediaInput();
    if (!input.open(this)) {
      print("open failed");
      return;
    }
    CodecInfo info = input.getCodecInfo();
    if (info.video_codec > 0) {
      //output video details
      print("video_stream=" + info.video_stream);
      print("video_size=" + info.width + "x" + info.height);
      print("video_bitRate=" + info.video_bit_rate);
      print("video_mime=" + input.getVideoMimeType());
    }
    if (info.audio_codec > 0) {
      //output audio details
      print("audio_stream=" + info.audio_stream);
      print("audio_layout=" + info.chs + "chs@" + info.freq + "Hz");
      print("audio_bitRate=" + info.audio_bit_rate);
      print("audio_mime=" + input.getAudioMimeType());
    }
  }

  public void print(String msg) {
    System.out.println(msg);
  }

  public int read(byte[] data) {
    try {
      return file.read(data);
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int write(byte[] data) {
    try {
      file.write(data);
      return data.length;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public long seek(long pos, int how) {
    try {
      switch (how) {
        case MediaCoder.SEEK_CUR: file.seek(file.getFilePointer() + pos); break;
        case MediaCoder.SEEK_SET: file.seek(pos); break;
        case MediaCoder.SEEK_END: file.seek(file.length() + pos); break;
      }
      return file.getFilePointer();
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }
}
