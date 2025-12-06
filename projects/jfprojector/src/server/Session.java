package server;

/**
 * Created : Mar 6, 2017
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;
import javaforce.voip.*;

public class Session {

  public void start(Socket socket) {
    this.socket = socket;
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        window = new Window();
        readThread = new ReadThread();
        readThread.start();
      }
    });
  }

  private Socket socket;
  private MediaInput decoder;
  private MediaAudioDecoder audio;
  private MediaVideoDecoder video;
  private long frameCount;
  private long audioCount;
  private final Object countLock = new Object();
  private boolean playing, eof;
  private Window window;
  private Thread playThread;
  private ReadThread readThread;
  private int frame;

  private static boolean inuse = false;
  private static Object lock = new Object();

  private AudioBuffer audio_buffer;
  private VideoBuffer video_buffer;
  final int audio_bufsiz = 1024;
  final int chs = 2;  //currently all formats are converted to stereo
  float fps;
  int width, height;
  int new_width, new_height;
  boolean resizeVideo;
  Object sizeLock = new Object();

  //buffer size in seconds
  //too small can cause problems
  //too large causes resizes to take a long time to take effect
  //the problem is that some video files are not interlaced very well
  final int buffer_seconds = 4;
  final int pre_buffer_seconds = 2;

  public class ReadThread extends Thread implements MediaIO {
    private InputStream is;
    int m_in;  //input data
    int m_out[] = new int[4];  //output data
    public void run() {
      if (inuse) {
        try {socket.close();} catch (Exception e) {}
        JFLog.log("Connection denied : Projector in use!");
        return;
      } else {
        inuse = true;
        JFLog.log("Connection from:" + socket.getRemoteSocketAddress());
      }
      frameCount = 0;
      audioCount = 0;
      audio_buffer = new AudioBuffer(44100, chs, buffer_seconds);
      video_buffer = null;
      width = -1;
      height = -1;
      resizeVideo = false;
      eof = false;

      width = window.getWidth();
      height = window.getHeight();

      try {
        is = socket.getInputStream();
        decoder = new MediaInput();
        if (decoder == null) throw new Exception("Unable to allocate decoder");
        if (!decoder.open(this)) throw new Exception("Unable to start decoder");
        audio = decoder.createAudioDecoder();
        video = decoder.createVideoDecoder(width, height);
        CodecInfo info = decoder.getCodecInfo();

        long mediaLength = info.duration;
        JFLog.log("Duration=" + mediaLength);
        fps = info.fps;
        JFLog.log("FPS=" + fps);
        JFLog.log("size=" + width + "x" + height);
        video_buffer = new VideoBuffer(width, height, buffer_seconds * (int)fps);
        playThread = new PlayAudioVideoThread();
        playThread.start();
        JFLog.log("Video Bit Rate=" + decoder.getVideoBitRate());
        JFLog.log("Audio Bit Rate=" + decoder.getAudioBitRate());
//        int avBitRate = decoder.getVideoBitRate() + decoder.getAudioBitRate();
//        if (avBitRate == 0) avBitRate = 64000;
        playing = true;
        while (playing) {
          if (resizeVideo) {
            synchronized(sizeLock) {
              width = new_width;
              height = new_height;
              info.width = width;
              info.height = height;
              video.change(info);
              resizeVideo = false;
            }
          }
          Packet packet = decoder.readPacket();
          if (packet == null) {
            eof = true;
            playing = false;
            break;
          }
          if (packet.stream == info.audio_stream) {
            short sams[] = audio.decode(packet);
            audio_buffer.add(sams, 0, sams.length);
            synchronized(lock) {
              lock.notify();
            }
          }
          else if (packet.stream == info.video_stream) {
            int px[] = video.decode(packet);
            JFImage img = video_buffer.getNewFrame();
            if (img != null) {
              if ((img.getWidth() != width) || (img.getHeight() != height)) {
                img.setSize(width, height);
              }
              img.putPixels(px, 0, 0, width, height, 0);
              video_buffer.freeNewFrame();
            } else {
              JFLog.log("Warning : VideoBuffer overflow");
            }
            synchronized(lock) {
              lock.notify();
            }
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      try {
        playThread.join();
      } catch (Exception e) {
        JFLog.log(e);
      }
      playThread = null;
      audio_buffer = null;
      video_buffer = null;
      if (window != null) {
        window.dispose();
        window = null;
      }
      decoder = null;
      inuse = false;
    }
    public int read(MediaCoder coder, byte data[]) {
      int read = 0;
      try {
        read = is.read(data, 0, data.length);
      } catch (Exception e) {
        JFLog.log(e);
        playing = false;
        return read;
      }
      if (read == -1) {
        playing = false;
        read = 0;
      }
      return read;
    }
    public int write(MediaCoder coder, byte data[]) {
      return 0;
    }
    public long seek(MediaCoder coder, long pos, int how) {
      switch (how) {
        case MediaCoder.SEEK_SET: return pos;
        case MediaCoder.SEEK_CUR: return pos;
        case MediaCoder.SEEK_END: return pos;
      }
      return 0;
    }
  }
  public class PlayAudioVideoThread extends Thread {
    public void run() {
      AudioOutput output = new AudioOutput();
      output.start(chs, 44100, 16, audio_bufsiz * 2 /*bytes*/, "<default>");
      short samples[] = new short[audio_bufsiz];
      for(int a=0;a<2;a++) output.write(samples);  //prime audio output
      while (playing) {
        synchronized(lock) {
          try {lock.wait();} catch (Exception e) {}
        }
        while (audio_buffer.size() >= audio_bufsiz) {
          audio_buffer.get(samples, 0, audio_bufsiz);
          output.write(samples);
          synchronized(countLock) { audioCount += audio_bufsiz; };
        }
        while (video_buffer.size() > 0) {
          JFImage img = video_buffer.getNextFrame();
          synchronized(countLock) { frameCount++; }
          if (img != null) {
            String txt = "Frame:" + frame++ + ":" + video_buffer.size();
            img.getGraphics().drawBytes(txt.getBytes(), 0, txt.length(), 0, 25);
            window.setImage(img);
            video_buffer.freeNextFrame();
          }
        }
      }
      output.stop();
    }
  }
  public class PlayAudioOnlyThread extends Thread {
    public void run() {
      AudioOutput output = new AudioOutput();
      output.start(chs, 44100, 16, audio_bufsiz * 2 /*bytes*/, "<default>");
      short samples[] = new short[audio_bufsiz];
      for(int a=0;a<2;a++) output.write(samples);  //prime output
      while (playing) {
        synchronized(lock) {
          try {lock.wait();} catch (Exception e) {}
        }
        while (audio_buffer.size() >= audio_bufsiz) {
          audio_buffer.get(samples, 0, audio_bufsiz);
          output.write(samples);
          synchronized(countLock) { audioCount += audio_bufsiz; };
        }
      }
      output.stop();
    }
  }
}
