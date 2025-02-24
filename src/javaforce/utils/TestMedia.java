package javaforce.utils;

/** TestMedia
 *
 * To run :
 *   bin/jfexec -cp javaforce.jar javaforce.utils.TestMedia
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.media.*;
import javaforce.voip.*;

public class TestMedia implements MediaIO {
  public static void usage() {
    System.out.println("TestMedia encoder | decoder | output | input");
    System.exit(1);
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
    }
    MediaCoder.init();
    for(int a=0;a<args.length;a++) {
      int idx = args[a].indexOf('=');
      if (idx != -1) {
        String key = args[a].substring(0, idx);
        String value = args[a].substring(idx+1);
        switch (key) {
          case "seconds": encoder_seconds = Integer.valueOf(value); break;
          case "audiosrc": encoder_audio_src = value; break;
        }
        continue;
      }
      try {
        switch (args[a]) {
          case "decoder": decoder(); break;
          case "encoder": encoder(true); break;
          case "input": input(); break;
          case "output": output(true); break;
          default: usage();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public static void decoder() {
    if (!new File("test-0.mp4").exists()) {
      encoder(false);
    }
    while (true) {
      TestMedia media = new TestMedia();
      MediaDecoder decoder = new MediaDecoder();
      media.open("test-0.mp4");
      decoder.start(media, 640, 480, 2, 44100, true);
      boolean eof = false;
      int pkts = 0;
      do {
        int type = decoder.read();
        pkts++;
        switch (type) {
          case MediaCoder.VIDEO_FRAME:
            int[] px = decoder.getVideo();
            if (px == null) break;
            System.out.println("video=" + px.length);
            break;
          case MediaCoder.AUDIO_FRAME:
            short[] sams = decoder.getAudio();
            if (sams == null) break;
            System.out.println("audio=" + sams.length);
            break;
          case MediaCoder.END_FRAME:
            eof = true;
            break;
        }
      } while (!eof);
      System.out.println("packets=" + pkts);
      decoder.stop();
      media.close();
    }
  }

  public static void input() {
    if (!new File("test-0.mp4").exists()) {
      output(false);
    }
    while (true) {
      TestMedia media = new TestMedia();
      MediaInput decoder = new MediaInput();
      media.open("test-0.mp4");
      decoder.open(media);
      int pkts = 0;
      MediaVideoDecoder videoDecoder = decoder.createVideoDecoder();
      MediaAudioDecoder audioDecoder = decoder.createAudioDecoder();
      Packet pkt;
      do {
        pkt = decoder.readPacket();
        if (pkt == null || pkt.length == 0) break;
        pkts++;
        if (pkt.stream == videoDecoder.getStream()) {
          int[] px = videoDecoder.decode(pkt);
          if (px != null) {
            JFLog.log("video=" + px.length);
          } else {
            JFLog.log("no video");
          }
        }
        else if (pkt.stream == audioDecoder.getStream()) {
          short[] sams = audioDecoder.decode(pkt);
          if (sams != null) {
            JFLog.log("audio=" + sams.length);
          } else {
            JFLog.log("no audio");
          }
        }
      } while (true);
      System.out.println("packets=" + pkts);
      decoder.close();
      media.close();
    }
  }

  public static void random(int[] px) {
    Random r = new Random();
    int len = px.length;
    for(int a=0;a<len;a++) {
      px[a] = r.nextInt() | 0xff000000;
    }
  }

  private static AudioGenerate audio_gen = new AudioGenerate();

  public static void random(short[] sams) {
    Random r = new Random();
    int len = sams.length;
    for(int a=0;a<len;a++) {
      sams[a] = (short)(r.nextInt(65536) - 32768);
    }
  }

  public static void sine(short[] sams) {
    audio_gen.generate(sams, AudioGenerate.SINE, 44100, 5000, 1.0f);
  }

  public static int encoder_seconds = 4;
  public static String encoder_audio_src = "random";
  public static boolean use_media_io = false;

  public static void encoder(boolean loop) {
    TestMedia media = new TestMedia();
    int[] px = new int[640*480];
    short[] sams = new short[7350];
    int i = 0;
    int video = MediaCoder.AV_CODEC_ID_H265;
    int audio = MediaCoder.AV_CODEC_ID_AAC;
    do {
      MediaEncoder encoder = new MediaEncoder();
      AudioInput input = new AudioInput();
      if (use_media_io) {
        media.size = 0;
        media.create(i++);
        encoder.start(media, 640, 480, 24, 2, 44100, "mp4", video, audio);
      } else {
        String file = "test-" + (i++) + ".mp4";
        encoder.startFile(file, 640, 480, 24, 2, 44100, "mp4", video, audio);
        System.out.println("file=" + file);
      }
      if (i == 10) i = 0;
      int frame_size = encoder.getAudioFramesize() * 2;  //*2=stereo
      JFLog.log("frame_size=" + frame_size);
      if (encoder_audio_src.equals("mic")) {
        input.start(2, 44100, 16, frame_size, "<default>");
      }
      System.out.println("Audio Frame Size=" + encoder.getAudioFramesize());
      for(int a=0;a<24 * encoder_seconds;a++) {
        JFLog.log("loop=" + a);
        random(px);
        if (encoder_audio_src.equals("mic")) {
          while (!input.read(sams)) {
            JF.sleep(50);
          }
        } else {
          sine(sams);
        }
        encoder.addVideo(px);
        encoder.addAudio(sams);
      }
      encoder.stop();
      if (use_media_io) {
        media.close();
        System.out.println("size=" + media.size);
      }
      if (encoder_audio_src.equals("mic")) {
        input.stop();
      }
      System.gc();
    } while (loop);
  }

  public static void output(boolean loop) {
    TestMedia media = new TestMedia();
    int[] px = new int[640*480];
    short[] sams = new short[7350];
    int i = 0;
    CodecInfo info = new CodecInfo();
    Packet packet;
    do {
      MediaOutput encoder = new MediaOutput();
      MediaVideoEncoder videoEncoder;
      MediaAudioEncoder audioEncoder;
      AudioInput input = new AudioInput();
      if (use_media_io) {
        media.size = 0;
        media.create(i++);
        encoder.create(media, "mp4");
      } else {
        String file = "test-" + (i++) + ".mp4";
        encoder.create(file, "mp4");
        System.out.println("file=" + file);
      }
      info.width = 640;
      info.height = 480;
      info.fps = 10f;
      info.video_bit_rate = (int)(1 * JF.MB);  //1Mb/s
      info.video_codec = MediaCoder.AV_CODEC_ID_H265;
      videoEncoder = encoder.createVideoEncoder(info);
      info.chs = 2;  //stereo
      info.freq = 44100;
      info.audio_bit_rate = (int)(128 * JF.KB);  //128kb/s
      info.audio_codec = MediaCoder.AV_CODEC_ID_AAC;
      audioEncoder = encoder.createAudioEncoder(info);
      if (i == 10) i = 0;
      int frame_size = audioEncoder.getAudioFramesize() * 2;  //*2=stereo
      JFLog.log("frame_size=" + frame_size);
      if (encoder_audio_src.equals("mic")) {
        input.start(2, 44100, 16, frame_size, "<default>");
      }
      System.out.println("Audio Frame Size=" + audioEncoder.getAudioFramesize());
      for(int a=0;a<24 * encoder_seconds;a++) {
        JFLog.log("loop=" + a);
        random(px);
        packet = videoEncoder.encode(px, 0, px.length);
        if (packet != null) {
          encoder.writePacket(packet);
        }
        if (encoder_audio_src.equals("mic")) {
          while (!input.read(sams)) {
            JF.sleep(50);
          }
        } else {
          sine(sams);
        }
        packet = audioEncoder.encode(sams, 0, sams.length);
        if (packet != null) {
          encoder.writePacket(packet);
        }
      }
      encoder.close();
      if (use_media_io) {
        media.close();
        System.out.println("size=" + media.size);
      }
      if (encoder_audio_src.equals("mic")) {
        input.stop();
      }
      System.gc();
    } while (loop);
  }

  public RandomAccessFile raf;

  public void create(int i) {
    try {
      raf = new RandomAccessFile("test-" + i + ".mp4", "rw");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void open(String filename) {
    try {
      raf = new RandomAccessFile(filename, "rw");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      raf.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    raf = null;
  }

  public int read(MediaCoder coder, byte[] data) {
    try {
      return raf.read(data);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  public long size;

  public int write(MediaCoder coder, byte[] data) {
    try {
      size += data.length;
      raf.write(data);
      return data.length;
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  public long seek(MediaCoder coder, long pos, int type) {
    try {
      switch (type) {
        case MediaCoder.SEEK_SET:
          break;
        case MediaCoder.SEEK_CUR:
          pos += raf.getFilePointer();
          break;
        case MediaCoder.SEEK_END:
          pos += raf.length();
          break;
      }
      raf.seek(pos);
      return pos;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }
}
