package server;

/**
 * Created : Mar 6, 2017
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.media.*;

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
  private MediaDecoder decoder;  //ffmpeg decoder
  private long frameCount;
  private long audioCount;
  private final Object countLock = new Object();
  private boolean playing, eof;
  private Window window;
  private Thread playThread;
  private ReadThread readThread;
  private static boolean inuse = false;

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

      try {
        is = socket.getInputStream();
        decoder = new MediaDecoder();
        if (decoder == null) throw new Exception("Unable to allocate decoder");
        JFLog.log("Starting decoder");
        if (!decoder.start(this, -1, -1, chs, 44100, false)) throw new Exception("Unable to start decoder");
        JFLog.log("Decoder Started!");

        long mediaLength = decoder.getDuration();
        JFLog.log("Duration=" + mediaLength);
        fps = decoder.getFrameRate();
        JFLog.log("FPS=" + fps);
        width = window.getWidth();
        height = window.getHeight();
        JFLog.log("size=" + width + "x" + height);
        decoder.resize(width, height);
        video_buffer = new VideoBuffer(width, height, buffer_seconds * (int)fps);
        playThread = new PlayAudioVideoThread();
        playThread.start();
        JFLog.log("Video Bit Rate=" + decoder.getVideoBitRate());
        JFLog.log("Audio Bit Rate=" + decoder.getAudioBitRate());
//        int avBitRate = decoder.getVideoBitRate() + decoder.getAudioBitRate();
//        if (avBitRate == 0) avBitRate = 64000;
        playing = true;
        while (playing) {
          if ((video_buffer != null && video_buffer.size() >= fps * (buffer_seconds-1)) || (audio_buffer.size() > (44100 * chs * (buffer_seconds-1)))) {
            int sleep;
            if (fps > 0) {
              sleep = 1000 / (int)fps;
//              JFLog.log("video sleeping:" + sleep + ":" + fps);
            } else {
              sleep = 1000 / ((44100 * chs) / (audio_bufsiz));
//              JFLog.log("audio sleeping:" + sleep);
            }
            JF.sleep(sleep);
            continue;
          }
          if (resizeVideo) {
            synchronized(sizeLock) {
              width = new_width;
              height = new_height;
              decoder.resize(width, height);
              resizeVideo = false;
            }
          }
          switch (decoder.read()) {
            case MediaCoder.AUDIO_FRAME:  //audio packet read
              short audio[] = decoder.getAudio();
              audio_buffer.add(audio, 0, audio.length);
              break;
            case MediaCoder.VIDEO_FRAME:  //video packet read
              int video[] = decoder.getVideo();
              JFImage img = video_buffer.getNewFrame();
              if (img != null) {
                if ((img.getWidth() != width) || (img.getHeight() != height)) {
                  img.setSize(width, height);
                }
                img.putPixels(video, 0, 0, width, height, 0);
                video_buffer.freeNewFrame();
              } else {
                JFLog.log("Warning : VideoBuffer overflow");
              }
              break;
            case MediaCoder.END_FRAME:
              eof = true;
              break;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      try {
        JFLog.log("wait for play thread");
        playThread.join();
        JFLog.log("play thread done");
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
      JFLog.log("read thread exit");
    }
    public int read(MediaCoder coder, byte data[]) {
      int read = 0;
      try {
        read = is.read(data, 0, data.length);
//        JFLog.log("read=" + read);
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
      JFLog.log("seek=" + pos + "," + how);
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
      double frameDelay = 1000.0f / fps;
      double samplesPerFrame = (44100.0 * ((double)chs)) / fps;
      JFLog.log("samplesPerFrame=" + samplesPerFrame);
      double samplesToWrite = 0;
      AudioOutput output = new AudioOutput();
      output.start(chs, 44100, 16, audio_bufsiz * 2 /*bytes*/, "<default>");
      short samples[] = new short[audio_bufsiz];
      double current = System.currentTimeMillis();
      int skip = 0;
      for(int a=0;a<2;a++) output.write(samples);  //prime audio output
      while (playing) {
        samplesToWrite += samplesPerFrame;
        if (eof) {
          if ((video_buffer.size() == 0) && (audio_buffer.size() < audio_bufsiz)) break;
        }
        while (audio_buffer.size() >= audio_bufsiz && samplesToWrite >= audio_bufsiz) {
          audio_buffer.get(samples, 0, audio_bufsiz);
          output.write(samples);
          samplesToWrite -= audio_bufsiz;
          synchronized(countLock) { audioCount += audio_bufsiz; };
        }
        if (video_buffer.size() > 0) {
          JFImage img = video_buffer.getNextFrame();
          synchronized(countLock) { frameCount++; }
          while (skip > 0 && video_buffer.size() > 1) {
            if (img == null) break;
            skip--;
            video_buffer.freeNextFrame();
            img = video_buffer.getNextFrame();
            synchronized(countLock) { frameCount++; }
          }
          skip = 0;
          if (img != null) {
            window.setImage(img);
            video_buffer.freeNextFrame();
          }
        } else {
          JFLog.log("Playback too slow - skipping a frame");
          skip++;
        }
        current += frameDelay;
        double now = System.currentTimeMillis();
        double delay = (current - now);
        if (delay >= 1.0) {
          JF.sleep((int)delay);
        }
      }
      output.stop();
      JFLog.log("play thread exit");
    }
  }
  public class PlayAudioOnlyThread extends Thread {
    public void run() {
      double frameDelay = 1000.0 / ((44100.0 * chs) / (audio_bufsiz));
      double samplesPerFrame = audio_bufsiz;
      double samplesToWrite = 0;
      AudioOutput output = new AudioOutput();
      output.start(chs, 44100, 16, audio_bufsiz * 2 /*bytes*/, "<default>");
      short samples[] = new short[audio_bufsiz];
      double current = System.currentTimeMillis();
      for(int a=0;a<2;a++) output.write(samples);  //prime output
      while (playing) {
        samplesToWrite += samplesPerFrame;
        if (eof) {
          if (audio_buffer.size() < audio_bufsiz) break;
        }
        while (audio_buffer.size() >= audio_bufsiz && samplesToWrite >= audio_bufsiz) {
          audio_buffer.get(samples, 0, audio_bufsiz);
          output.write(samples);
          samplesToWrite -= audio_bufsiz;
          synchronized(countLock) { audioCount += audio_bufsiz; };
        }
        current += frameDelay;
        double now = System.currentTimeMillis();
        double delay = (current - now);
        if (delay >= 1.0) {
          JF.sleep((int)delay);
        }
      }
      output.stop();
      JFLog.log("play thread exit");
    }
  }
}
