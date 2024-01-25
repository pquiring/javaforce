package service;

/**
 *
 * @author pquiring
 */

import java.util.Random;
import javaforce.media.*;
import java.io.*;
import javaforce.JFLog;

public class MediaTest implements MediaIO {
  public RandomAccessFile raf;

  public MediaTest() {
    try { raf = new RandomAccessFile("test.mp4", "rw"); } catch (Exception e) {}
  }

  public int read(MediaCoder coder, byte[] buffer) {
    JFLog.log("read:" + buffer.length);
    return -1;
  }

  public int write(MediaCoder coder, byte[] buffer) {
    long pos = 0;
    try { pos=raf.getFilePointer(); raf.write(buffer); } catch (Exception e) {}
    JFLog.log("write:" + buffer.length + "@" + pos);
    return buffer.length;
  }

  public long seek(MediaCoder coder, long pos, int how) {
    JFLog.log("seek:" + pos + ":" + how);
    long newpos = 0;
    try {
      switch (how) {
        case MediaCoder.SEEK_SET: newpos = pos; break;
        case MediaCoder.SEEK_CUR: long curpos = raf.getFilePointer(); newpos = pos + curpos; break;
        case MediaCoder.SEEK_END: long size = raf.length(); newpos = size + pos; break;
      }
      raf.seek(newpos);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

  public static void main(String args[]) {
    MediaEncoder encoder = new MediaEncoder();
    encoder.framesPerKeyFrame = 10;
    encoder.videoBitRate = 16 * 1024 * 1024;
    int width = 1366;
    int height = 768;
    int px = width * height;
    encoder.start(new MediaTest(), width, height, 10, 0, 0, "mp4", true, false);
    //add random images
    int test[] = new int[px];
    Random r = new Random();
    for(int a=0;a<24 * 10;a++) {
      for(int b=0;b<px;b++) {
        test[b] = r.nextInt();
      }
      encoder.addVideo(test);
    }
    encoder.stop();
  }
}
