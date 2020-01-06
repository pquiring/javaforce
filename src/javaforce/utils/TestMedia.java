package javaforce.utils;

/** TestMedia
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.media.*;

public class TestMedia implements MediaIO {
  public static void usage() {
    System.out.println("TestMedia encoder | decoder");
    System.exit(1);
  }
  public static void main(String args[]) {
    if (args.length == 0) {
      usage();
    }
    switch (args[0]) {
      case "decoder": decoder();
      case "encoder": encoder(true);
      default: usage();
    }
  }
  public static void decoder() {
    encoder(false);  //create test.mp4
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
            int px[] = decoder.getVideo();
            if (px == null) break;
            System.out.println("video=" + px.length);
            break;
          case MediaCoder.AUDIO_FRAME:
            short sams[] = decoder.getAudio();
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
  public static void random(int px[]) {
    Random r = new Random();
    int len = px.length;
    for(int a=0;a<len;a++) {
      px[a] = r.nextInt() | 0xff000000;
    }
  }
  public static void random(short sams[]) {
    Random r = new Random();
    int len = sams.length;
    for(int a=0;a<len;a++) {
      sams[a] = (short)(r.nextInt(65536) - 32768);
    }
  }
  public static void encoder(boolean loop) {
    TestMedia media = new TestMedia();
    int px[] = new int[640*480];
    short sams[] = new short[7350];
    int i = 0;
    do {
      media.size = 0;
      MediaEncoder encoder = new MediaEncoder();
      media.create(i++);
      if (i == 10) i = 0;
      encoder.start(media, 640, 480, 24, 2, 44100, "mp4", true, true);
      System.out.println("Audio Frame Size=" + encoder.getAudioFramesize());
      for(int a=0;a<24 * 4;a++) {  //4 seconds
        random(px);
        random(sams);
        encoder.addVideo(px);
        encoder.addAudio(sams);
      }
      encoder.stop();
      media.close();
      System.out.println("size=" + media.size);
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

  public long seek(MediaCoder coder, long pos, int how) {
    try {
      switch (how) {
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
