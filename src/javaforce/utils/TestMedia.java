package javaforce.utils;

/** TestMedia
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import javaforce.JF;

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
      case "encoder": encoder();
      default: usage();
    }
  }
  public static void decoder() {
    while (true) {
      MediaDecoder decoder = new MediaDecoder();
      //...
    }
  }
  public static void random(int px[]) {
    Random r = new Random();
    int len = px.length;
    for(int a=0;a<len;a++) {
      px[a] = r.nextInt() | 0xff000000;
    }
  }
  public static void encoder() {
    TestMedia media = new TestMedia();
    int px[] = new int[640*480];
    while (true) {
      media.size = 0;
      MediaEncoder encoder = new MediaEncoder();
      media.create();
      encoder.start(media, 640, 480, 24, 2, 44100, "mp4", true, false);
      for(int a=0;a<100;a++) {
        random(px);
        encoder.addVideo(px);
      }
      encoder.stop();
      media.close();
      System.out.println("size=" + media.size);
      System.gc();
    }
  }

  public RandomAccessFile raf;

  public void create() {
    try {
      raf = new RandomAccessFile("test.mp4", "rw");
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
      raf.seek(pos);
      return pos;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }
}
