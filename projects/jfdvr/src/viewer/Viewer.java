package viewer;

/**
 * Created : Mar 25, 2012
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.voip.*;
import javaforce.media.*;

public class Viewer {

  private final Object countLock = new Object();
  private VideoPanel videoPanel;
  private NetworkReader networkReader;
  private String[] cameras;
  private NetworkReader[] networkReaders;  //group cameras
  private int grid_x, grid_y, grid_xy;
  private boolean playing;
  private MediaEncoder encoder;
  private MediaDownload downloader;

  public static boolean debug = false;
  public static boolean debug_packets = false;

  private final static boolean debug_buffers = false;

  public URL url;

  /** Play a network source directly. */
  public void play(URL url) {
    if (playing) {
      stop(true);
    }
    playing = true;
    if (url == null) {
      JFAWT.showError("Error", "Invalid URL");
      return;
    }
    this.url = url;
    if (videoPanel == null) {
      videoPanel = new VideoPanel(this);
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          ViewerApp.setPanel(videoPanel);
        }
      });
      videoPanel.start();
    }
    videoPanel.setURL(url.toString());
    networkReader = new NetworkReader(url);
    networkReader.start();
  }

  public synchronized void stop(boolean wait) {
    if (!playing) {
      return;
    }
    playing = false;
    if (wait) {
      JFLog.log("stop:waiting for reader thread to stop");
      if (networkReader != null) {
        try {networkReader.join();} catch (Exception e) {JFLog.log(e);}
      }
      JFLog.log("stop:reader thread done");
    }
  }

  public void log(String msg) {
    JFLog.log("" + System.currentTimeMillis() + ":" + msg);
  }

  private final int buffer_seconds = 4;
  private final int pre_buffer_seconds = 2;
  private int new_width, new_height;
  private boolean resizeVideo;
  private Object sizeLock = new Object();

  public class NetworkReader extends Thread implements MediaIO, RTSPClientInterface, RTPInterface, PacketReceiver {
    private URL url;
    private String type, name;
    private RTSPClient rtsp;
    private RTP rtp;
    private RTPChannel channel;
    private RTPH264 h264;
    private RTPH265 h265;
    private PacketBuffer packets;
    private long lastPacket;
    private long lastKeepAlive;
    private int decoded_frame[];
    private int decoded_x, decoded_y, decoded_xy;
    private int log = 0;
    private boolean grid;
    private int gx, gy;
    private AudioBuffer audio_buffer;
    private VideoBuffer video_buffer;
    private MediaVideoDecoder video_decoder;
    private long frameCount;
    private long audioCount;
    private boolean preBuffering;
    private int width, height;
    private final int audio_bufsiz = 1024;
    private final int chs = 2;  //currently all formats are converted to stereo
    private float fps = -1;
    private Thread playThread;

    public NetworkReader(URL url) {
      //rtsp://host/type/name
      this.url = url;
      String path = url.getPath().substring(1);
      String[] p = path.split("/");
      type = p[0];
      name = p[1];
    }
    public void setGrid(int gx, int gy) {
      grid = true;
      this.gx = gx;
      this.gy = gy;
    }
    public void run() {
      frameCount = 0;
      audioCount = 0;
      audio_buffer = null;
      video_buffer = null;
      width = -1;
      height = -1;
      resizeVideo = false;
      preBuffering = true;

      drawCameraIcon();

      try {
        connect();

        String err = null;
        if (playThread == null) {
          playThread = new PlayVideoOnlyThread();
          playThread.start();
        }
        while (playing) {
          JF.sleep(1000);
          long now = System.currentTimeMillis();
          if (now - lastPacket > 20*1000) {
            JFLog.log(log, "NetworkReader : Reconnecting : " + url);
            disconnect();
            drawCameraIcon();
            if (!connect()) {
              JF.sleep(1000);
              continue;
            }
          } else if (now - lastKeepAlive > 45*1000) {
            rtsp.keepalive(url.toString());
            lastKeepAlive = now;
          }
          if (type.equals("group") && cameras != null) {
            //group no longer need to run
            break;
          }
        }
        JFLog.log("NetworkReader:closing");
        close(true, true);
        if (err != null) JFAWT.showError("Error", err);
      } catch (Exception e) {
        JFAWT.showError("Error", e.toString());
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
      if (videoPanel != null) {
        videoPanel.stop();
        videoPanel = null;
        ViewerApp.self.setPanel(null);
      }
      if (playing) Viewer.this.stop(false);
      video_decoder = null;
    }

    private void drawCameraIcon() {
      if (type.equals("group")) return;
      if (grid) {
        videoPanel.setImage(ViewerApp.cameraicon, gx, gy);
      } else {
        videoPanel.setImage(ViewerApp.cameraicon);
      }
    }

    public boolean connect() {
      rtsp = new RTSPClient();
      int port = url.getPort();
      if (port == -1) {
        port = 554;  //default RTSP port
      }
      rtsp.init(url.getHost(), port, Config.getLocalPort(), this, TransportType.TCP);
      String user_pass = url.getUserInfo();
      if (user_pass != null) {
        int idx = user_pass.indexOf(":");
        String user = user_pass.substring(0, idx);
        String pass = user_pass.substring(idx + 1);
        rtsp.setUserPass(user, pass);
      }
      if (type.equals("group")) {
        rtsp.get_parameter(url.toString(), new String[] {"action: query"});
      } else {
        start_camera();
      }

      long now = System.currentTimeMillis();
      lastKeepAlive = now;
      lastPacket = now;
      return true;
    }

    public void disconnect() {
      if (rtsp != null) {
        rtsp.uninit();
        rtsp = null;
      }
      if (video_decoder != null) {
        video_decoder.stop();
        video_decoder = null;
      }
    }

    private void close(boolean disconnect, boolean teardown) {
      JFLog.log("close:1");
      if (disconnect && rtsp != null) {
        if (teardown) {
          rtsp.teardown(url.toString());
        }
        rtsp.uninit();
        rtsp = null;
      }
      JFLog.log("close:2");
      if (rtp != null) {
        rtp.stop();
        rtp = null;
      }
      channel = null;
      JFLog.log("close:3");
      if (video_decoder != null) {
        video_decoder.stop();
        video_decoder = null;
      }
      JFLog.log("close:4");
    }

    //interface MediaIO
    public int read(MediaCoder coder, byte data[]) {
      int read = 0;
      try {
        //TODO : read network data
      } catch (Exception e) {
        JFLog.log(e);
        return read;
      }
      if (read == -1) read = 0;
      return read;
    }
    public int write(MediaCoder coder, byte data[]) {
      //jfmedia does not create media files
      return 0;
    }
    public long seek(MediaCoder coder, long pos, int how) {
      return -1;
    }

    //RTSPClient Interface
    public void onOptions(RTSPClient client) {
      JFLog.log("onOptions");
      client.describe(url.toString());
    }

    public void onDescribe(RTSPClient client, SDP sdp) {
      JFLog.log("onDescribe");
      JFLog.log("SDP=" + sdp);
      close(false, false);
      if (sdp == null) {
        JFLog.log("Play failed : onDescribe() SDP == null");
        return;
      }
      SDP.Stream stream = sdp.getFirstVideoStream();
      if (stream == null) {
        JFLog.log("Error:CameraWorker:onDescribe():SDP does not contain video stream");
        return;
      }
      //IP/port in SDP is all zeros
      stream.setIP(client.getRemoteIP());
      stream.setPort(-1);
      if (video_decoder != null) {
        video_decoder.stop();
        video_decoder = null;
      }
      video_decoder = new MediaVideoDecoder();
      boolean status;
      fps = stream.getFrameRate();
      if (fps <= 0) {
        JFLog.log("Warning : Invalid framerate:Using 10fps");
        fps = 10;
      } else {
        JFLog.log("FPS=" + fps);
      }
      decoded_x = ViewerApp.self.getWidth();
      decoded_y = ViewerApp.self.getHeight();
      decoded_xy = decoded_x * decoded_y;
      width = decoded_x;
      height = decoded_y;
      video_buffer = new VideoBuffer(width, height, buffer_seconds * (int)fps);
      int av_codec = -1;
      if (stream.hasCodec(RTP.CODEC_H264)) {
        av_codec = MediaCoder.AV_CODEC_ID_H264;
        h264 = new RTPH264();
        packets = new PacketBuffer(CodecType.H264);
      } else if (stream.hasCodec(RTP.CODEC_H265)) {
        av_codec = MediaCoder.AV_CODEC_ID_H265;
        h265 = new RTPH265();
        packets = new PacketBuffer(CodecType.H265);
      } else {
        JFLog.log("DVR Viewer:No supported codec detected");
        return;
      }
      status = video_decoder.start(av_codec, decoded_x, decoded_y);
      if (!status) {
        JFLog.log("Error:MediaVideoDecoder.start() failed");
        return;
      }
      rtp = new RTP();
      rtp.init(this, TransportType.UDP);
      rtp.start();
      channel = rtp.createChannel(stream);
      channel.start();
      client.setup(url.toString(), rtp.getlocalrtpport(), stream.control);
    }

    public void onSetup(RTSPClient client) {
      JFLog.log("onSetup");
      client.play(url.toString());
    }

    public void onPlay(RTSPClient client) {
      JFLog.log("onPlay");
      //connect to RTP stream and start decoding video
    }

    public void onTeardown(RTSPClient client) {
      JFLog.log("onTeardown");
      //stop RTP stream
      close(true, false);
    }

    private void start_camera() {
      rtsp.options(url.toString());
    }

    private void start_group(String cams) {
      JFLog.log("start_group:" + cams);
      cameras = cams.split(",");
      int count = cameras.length;
      grid = true;
      grid_x = 2;
      grid_y = 2;
      grid_xy = grid_x * grid_y;
      while (grid_xy < count) {
        if (grid_x == grid_y) {
          grid_x++;
        } else {
          grid_y++;
        }
        grid_xy = grid_x * grid_y;
      }
      videoPanel.setGrid(grid_x, grid_y);
      //start cameras
      networkReaders = new NetworkReader[count];
      int px = 0;
      int py = 0;
      for(int a=0;a<count;a++) {
        URL camurl = Config.newURL("/camera/" + cameras[a]);
        if (camurl == null) continue;
        NetworkReader nr = new NetworkReader(camurl);
        nr.setGrid(px, py);
        nr.start();
        networkReaders[a] = nr;
        px++;
        if (px == grid_x) {
          px = 0;
          py++;
        }
      }
    }

    public void onGetParameter(RTSPClient client, String[] params) {
      JFLog.log("onGetParameter:" + type);
      switch (type) {
        case "camera":
          //keep-alive
          break;
        case "group":
          if (!grid) {
            start_group(HTTP.getParameter(params, "cameras"));
          } else {
            //keep-alive
          }
          break;
      }
    }

    public void onSetParameter(RTSPClient client, String[] params) {
      String ts = HTTP.getParameter(params, "ts");
      if (ts != null) {
        //notice that is has changed
        videoPanel.setTimestamp(JF.atol(ts));
        return;
      }
      String download = HTTP.getParameter(params, "download");
      if (download == null) return;
      switch (download) {
        case "complete":
          stopDownload();
          break;
      }
    }

    //RTP Interface

    public void rtpSamples(RTPChannel rtp) {
    }

    public void rtpDigit(RTPChannel rtp, char key) {
    }

    public void rtpPacket(RTPChannel rtp, int codec, byte[] buf, int offset, int length) {
      switch (codec) {
        case CodecType.H264: rtpCodec(rtp, h264, buf, offset, length); break;
        case CodecType.H265: rtpCodec(rtp, h265, buf, offset, length); break;
      }
    }

    public void rtpCodec(RTPChannel rtp, RTPCodec codec, byte[] buf, int offset, int length) {
      if (debug_packets) JFLog.log("rtpCodec:packet");
      try {
        //I frame : 9 ... 5 (key frame)
        //P frame : 9 ... 1 (diff frame)
        lastPacket = System.currentTimeMillis();
        codec.decode(buf, 0, length, this);
      } catch (Exception e) {
        JFLog.log(log, e);
      }
    }

    public void onPacket(Packet rtp_packet) {
      if (debug_packets) JFLog.log("onPacket");
      try {
        packets.add(rtp_packet);
        if (!packets.haveCompleteFrame()) return;
        boolean key_frame = packets.isNextFrame_KeyFrame();
        if (debug_buffers && key_frame) {
          JFLog.log(log, "packets_decode=" + packets.toString());
        }
        Packet codec_packet = packets.getNextFrame();
        if (encoder != null) {
          encoder.addVideoEncoded(codec_packet.data, codec_packet.offset, codec_packet.length, key_frame);
          return;
        }
        decoded_frame = video_decoder.decode(codec_packet.data, codec_packet.offset, codec_packet.length);
        if (decoded_frame == null) {
          JFLog.log(log, "Error:newFrame == null:packet.length=" + codec_packet.length + ":" + name);
          JFLog.log(log, "NALs=" + packets.get_nal_list());
          packets.reset();
          return;
        } else {
          if (false) {
            int video_width = video_decoder.getWidth();
            int video_height = video_decoder.getHeight();
            float video_fps = video_decoder.getFrameRate();
            JFLog.log(log, "Note : detected width/height=" + video_width + "x" + video_height);
            JFLog.log(log, "Note : detected FPS=" + video_fps);
          }
          if (width > 0 && height > 0) {
            JFImage img = video_buffer.getNewFrame();
            if (img != null) {
              if ((img.getWidth() != width) || (img.getHeight() != height)) {
                img.setSize(width, height);
              }
              img.putPixels(decoded_frame, 0, 0, width, height, 0);
              video_buffer.freeNewFrame();
            } else {
              if (debug_buffers) JFLog.log("Warning : VideoBuffer overflow");
            }
          }
        }
      } catch (Exception e) {
        JFLog.log(log, e);
      }
    }

    public void rtpInactive(RTPChannel rtp) {
    }

    public class PlayAudioVideoThread extends Thread {
      public void run() {
        double frameDelay = -1;
        double samplesPerFrame = (44100.0 * ((double)chs)) / fps;
        JFLog.log("samplesPerFrame=" + samplesPerFrame);
        double samplesToWrite = 0;
        AudioOutput output = new AudioOutput();
        output.start(chs, 44100, 16, audio_bufsiz * 2 /*bytes*/, "<default>");
        short samples[] = new short[audio_bufsiz];
        double current = System.currentTimeMillis();
        int skip = 0;
        while (playing) {
          if (video_buffer == null) {
            JF.sleep(100);
            continue;
          }
          if (audio_buffer == null) {
            JF.sleep(100);
            continue;
          }
          if (frameDelay == -1) {
            if (fps == -1) {
              JF.sleep(100);
              continue;
            }
            frameDelay = 1000.0f / fps;
          }
          if (preBuffering) {
            //wait till buffers are 50% full before starting
            while (playing && preBuffering) {
              if (video_buffer.size() >= (fps * pre_buffer_seconds)) break;
              JF.sleep(25);
            }
            preBuffering = false;
            for(int a=0;a<2;a++) output.write(samples);  //prime audio output
          }
          samplesToWrite += samplesPerFrame;
          if (audio_buffer != null) {
            while (audio_buffer.size() >= audio_bufsiz && samplesToWrite >= audio_bufsiz) {
              audio_buffer.get(samples, 0, audio_bufsiz);
              output.write(samples);
              samplesToWrite -= audio_bufsiz;
              synchronized(countLock) { audioCount += audio_bufsiz; };
            }
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
              if (grid) {
                videoPanel.setImage(img, gx, gy);
              } else {
                videoPanel.setImage(img);
              }
              video_buffer.freeNextFrame();
            }
          } else {
            if (debug) JFLog.log("Playback too slow - skipping a frame");
            skip++;
          }
          current += frameDelay;
          double now = System.currentTimeMillis();
          double delay = (current - now);
          if (delay < 1) {
            delay = 1;
          }
          if (delay > 1000) {
            delay = 1000;
          }
          JF.sleep((int)delay);
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
        while (playing) {
          if (audio_buffer == null) {
            JF.sleep(100);
            continue;
          }
          if (preBuffering) {
            //wait till buffers are 50% full before starting
            while (playing && preBuffering) {
              if (audio_buffer.size() >= (44100 * chs * pre_buffer_seconds)) break;
              JF.sleep(25);
            }
            preBuffering = false;
            for(int a=0;a<2;a++) output.write(samples);  //prime output
          }
          samplesToWrite += samplesPerFrame;
          while (audio_buffer.size() >= audio_bufsiz && samplesToWrite >= audio_bufsiz) {
            audio_buffer.get(samples, 0, audio_bufsiz);
            output.write(samples);
            samplesToWrite -= audio_bufsiz;
            synchronized(countLock) { audioCount += audio_bufsiz; };
          }
          current += frameDelay;
          double now = System.currentTimeMillis();
          double delay = (current - now);
          if (delay < 1) {
            delay = 1;
          }
          if (delay > 1000) {
            delay = 1000;
          }
          JF.sleep((int)delay);
        }
        output.stop();
        JFLog.log("play thread exit");
      }
    }
    public class PlayVideoOnlyThread extends Thread {
      public void run() {
        double frameDelay = -1;
        double current = System.currentTimeMillis();
        int skip = 0;
        while (playing) {
          if (video_buffer == null) {
            JF.sleep(100);
            continue;
          }
          if (frameDelay == -1) {
            if (fps == -1) {
              JF.sleep(100);
              continue;
            }
            frameDelay = 1000.0f / fps;
          }
          if (preBuffering) {
            //wait till buffers are 50% full before starting
            while (playing && preBuffering) {
              if (video_buffer.size() >= (fps * pre_buffer_seconds)) break;
              JF.sleep(25);
            }
            preBuffering = false;
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
              if (grid) {
                videoPanel.setImage(img, gx, gy);
              } else {
                videoPanel.setImage(img);
              }
              video_buffer.freeNextFrame();
            }
          } else {
            if (debug) JFLog.log("Playback too slow - skipping a frame");
            skip++;
          }
          current += frameDelay;
          double now = System.currentTimeMillis();
          double delay = (current - now);
          if (delay < 1) {
            delay = 1;
          }
          if (delay > 1000) {
            delay = 1000;
          }
          JF.sleep((int)delay);
        }
        JFLog.log("play thread exit");
      }
    }
  }
  public void setVideoSize(int width,int height) {
    synchronized(sizeLock) {
      new_width = width;
      new_height = height;
      resizeVideo = true;
    }
  }
  public void refresh() {
    stop(true);
    play(Config.url);
  }
  public void startDownload(String filename) {
    downloader = new MediaDownload(this, filename);
    encoder = new MediaEncoder();
    encoder.start(downloader, -1, -1, -1, -1, -1, "mp4", true, false);
    downloader.setVisible(true);  //current thread is EDT
  }
  public void stopDownload() {
    if (encoder != null) {
      encoder.stop();
      encoder = null;
    }
    if (downloader != null) {
      downloader.complete();
      downloader = null;
    }
    play(JF.createURL(VideoPanel.cleanURL(url.toString())));
  }
}
