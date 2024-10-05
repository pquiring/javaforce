/** Manages the local camera and sends the encoded image to all remote parties.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import javaforce.*;
import javaforce.awt.*;
import javaforce.jni.*;
import javaforce.media.*;
import javaforce.voip.*;

public class LocalCamera extends Thread implements MediaIO, PacketReceiver {
  private volatile boolean active = false;
  private volatile boolean main_done = false;
  private Camera camera;
  private static Codec codec;
  private static volatile boolean inuse[] = new boolean[6];
  private static PhoneLine lines[];
  private static LocalCamera thread;
  private static Object useLock = new Object();
  //    private RandomAccessFile raf;

  public static void setCodec(Codec codec) {
    LocalCamera.codec = codec;
  }

  public static Codec getCodec() {
    return codec;
  }

  public static void init(PhoneLine lines[]) {
    LocalCamera.lines = lines;
  }

  public static void enable(int line) {
    synchronized(useLock) {
      inuse[line] = true;
      if (thread == null) {
        thread = new LocalCamera();
        thread.start();
      }
    }
  }

  public static void disable(int line) {
    synchronized(useLock) {
      if (inuse[line] == false) return;
      inuse[line] = false;
      for(int a=0;a<6;a++) {
        if (inuse[a] == true) return;
      }
    }
    if (thread != null) {
      thread.cancel();
      thread = null;
    }
  }

  public static boolean isRunning() {
    return thread != null;
  }

  public void run() {
    active = true;
    main_done = false;
    try {
      int[] res = Settings.current.getVideoResolution();
      camera = new Camera();
      //      try { raf = new RandomAccessFile("media_local." + codec.name, "rw"); } catch (Exception e) {}
      if (!camera.init()) {
        JFAWT.showError("Error", "Failed to init camera");
        return;
      }
      String[] devices = camera.listDevices();
      if (devices == null || devices.length == 0) {
        JFAWT.showError("Error", "Failed to find a camera");
        return;
      }
      int idx = 0;
      for(int a=0;a<devices.length;a++) {
        if (devices[a].equals(Settings.current.videoDevice)) {
          idx = a;
          break;
        }
      }
      if (!camera.start(idx, PhonePanel.vx, PhonePanel.vy)) {
        JFAWT.showError("Error", "Failed to start camera");
        main_done = true;
        return;
      }
      int cx = camera.getWidth();
      int cy = camera.getHeight();
      JFLog.log("camera size=" + cx + "x" + cy);
      localImage = new JFImage();
      localImage.setImageSize(PhonePanel.vx, PhonePanel.vy);
      localCameraSender = new LocalCameraSender();
      localCameraSender.start();
      JFLog.log("LocalCamera starting");
      while (active) {
        while (active && list.size() > 0) {
          JF.sleep(10);
        }
        JF.sleep(1000 / Settings.current.videoFPS);
        int[] px = camera.getFrame();
        if (px == null) {
          continue;
        }
        JFImage tmp = new JFImage(cx, cy);
        tmp.putPixels(px, 0, 0, cx, cy, 0);
        localImage.getGraphics().drawImage(tmp.getImage(), 0, 0, PhonePanel.vx, PhonePanel.vy, null);
        synchronized(useLock) {
          for(int a=0;a<6;a++) {
            if (inuse[a]) {
              VideoWindow vw = lines[a].videoWindow;
              if (vw != null) vw.setLocalImage(localImage);
            }
          }
        }
        if (codec.name.equals("JPEG")) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          localImage.saveJPG(baos);
          rtpJpeg.encode(baos.toByteArray(), 0, baos.size(), PhonePanel.vx, PhonePanel.vy, this);
          synchronized (lock) {
            lock.notify();
          }
          continue;
        }
        if (codec.name.startsWith("H263")) {
          //H263,H263-1998,H263-2000
          px = localImage.getPixels();
          if (encoder == null) {
            encoder = new MediaEncoder();
            encoder.setFramesPerKeyFrame(5);
            if (!encoder.start(this, PhonePanel.vx, PhonePanel.vy, 24, -1, -1, "h263", true, false)) {
              JFLog.log("H263 encoder failed to start");
              encoder.stop();
              encoder = null;
              continue;
            }
          }
          encoder.addVideo(px);
          continue;
        }
        if (codec.name.equals("H264")) {
          px = localImage.getPixels();
          if (encoder == null) {
            encoder = new MediaEncoder();
            encoder.setFramesPerKeyFrame(5);
            if (!encoder.start(this, PhonePanel.vx, PhonePanel.vy, 24, -1, -1, "h264", true, false)) {
              JFLog.log("H264 encoder failed to start");
              encoder.stop();
              encoder = null;
              continue;
            }
          }
          encoder.addVideo(px);
          continue;
        }
        if (codec.name.equals("H265")) {
          px = localImage.getPixels();
          if (encoder == null) {
            encoder = new MediaEncoder();
            encoder.setFramesPerKeyFrame(5);
            if (!encoder.start(this, PhonePanel.vx, PhonePanel.vy, 24, -1, -1, "h265", true, false)) {
              JFLog.log("H265 encoder failed to start");
              encoder.stop();
              encoder = null;
              continue;
            }
          }
          encoder.addVideo(px);
          continue;
        }
        if (codec.name.equals("VP8")) {
          px = localImage.getPixels();
          if (encoder == null) {
            encoder = new MediaEncoder();
            encoder.setFramesPerKeyFrame(5);
            if (!encoder.start(this, PhonePanel.vx, PhonePanel.vy, 24, -1, -1, "vpx", true, false)) {
              JFLog.log("VP8 encoder failed to start");
              encoder.stop();
              encoder = null;
              continue;
            }
          }
          encoder.addVideo(px);
          continue;
        }
        if (codec.name.equals("VP9")) {
          px = localImage.getPixels();
          if (encoder == null) {
            encoder = new MediaEncoder();
            encoder.setFramesPerKeyFrame(5);
            if (!encoder.start(this, PhonePanel.vx, PhonePanel.vy, 24, -1, -1, "vpx", true, false)) {
              JFLog.log("VP9 encoder failed to start");
              encoder.stop();
              encoder = null;
              continue;
            }
          }
          encoder.addVideo(px);
          continue;
        }
        JFLog.log("err:local camera running without a valid codec");
      }
      camera.stop();
      camera.uninit();
      if (encoder != null) {
        encoder.stop();
        encoder = null;
      }
      while (!localCameraSender.sender_done) {
        JFLog.log("Waiting for LocalCameraSender to stop");
        JF.sleep(500);
        synchronized (lock) {
          lock.notify();
        }
      }
      //      try { raf.close(); } catch (Exception e) {}
    } catch (Exception e) {
      JFLog.log(e);
    }
    JFLog.log("LocalCamera done");
    main_done = true;
  }

  public void cancel() {
    active = false;
    while (!main_done) {
      JFLog.log("Waiting for LocalCamera to stop");
      JF.sleep(500);
    }
  }

  private class LocalCameraSender extends Thread {
    private volatile boolean sender_done = false;
    public void run() {
      try {
        while (active) {
          if (list.isEmpty()) {
            synchronized (lock) {
              try {
                lock.wait();
              } catch (Exception e) {
              }
            }
          }
          if (list.isEmpty()) {
            continue;
          }
          byte[] data = list.remove(0);
          if (data == null) {
            continue;
          }
          synchronized(useLock) {
            for(int a=0;a<6;a++) {
              if (!inuse[a]) continue;
              RTP rtp = lines[a].videoRTP;
              if (rtp == null) continue;
              if (!rtp.active) continue;
              RTPChannel channels[] = (RTPChannel[])rtp.channels.toArray(new RTPChannel[0]);
              for(int r=0;r<channels.length;r++) {
                RTPChannel channel = channels[r];
                if (channel.stream.type == SDP.Type.video && channel.stream.canSend()) {
                  channel.writeRTP(data, 0, data.length);
                }
              }
            }
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      JFLog.log("LocalCameraSender done");
      sender_done = true;
    }
  }

  private LocalCamera.LocalCameraSender localCameraSender;
  private JFImage localImage;
  private RTPJPEG rtpJpeg = new RTPJPEG();
  private RTPH263 rtpH263 = new RTPH263();
  private RTPH263_1998 rtpH263_1998 = new RTPH263_1998();
  private RTPH263_2000 rtpH263_2000 = new RTPH263_2000();
  private RTPH264 rtpH264 = new RTPH264();
  private RTPH265 rtpH265 = new RTPH265();
  private RTPVP8 rtpVP8 = new RTPVP8();
  private RTPVP9 rtpVP9 = new RTPVP9();
  private Object lock = new Object();
  private Vector<byte[]> list = new Vector<byte[]>();
  private MediaEncoder encoder;

  public int read(MediaCoder coder, byte[] bytes) {
    return 0;
  }

  public int write(MediaCoder coder, byte[] bytes) {
    int len = bytes.length;
    if (codec.name.equals("H263")) {
      //        printArray("encoded_h263", bytes, 0, bytes.length);
      rtpH263.setid(codec.id);
      rtpH263.encode(bytes, 0, bytes.length, PhonePanel.vx, PhonePanel.vy, this);
    } else if (codec.name.equals("VP8")) {
      //        printArray("encoded_vp8", bytes, 0, bytes.length);
      //        try { raf.write(bytes); } catch (Exception e) {}
      rtpVP8.setid(codec.id);
      rtpVP8.encode(bytes, 0, bytes.length, PhonePanel.vx, PhonePanel.vy, this);
    } else if (codec.name.equals("VP9")) {
      //        printArray("encoded_vp9", bytes, 0, bytes.length);
      //        try { raf.write(bytes); } catch (Exception e) {}
      rtpVP9.setid(codec.id);
      rtpVP9.encode(bytes, 0, bytes.length, PhonePanel.vx, PhonePanel.vy, this);
    } else if (codec.name.equals("H264")) {
      //        printArray("encoded_h264", bytes, 0, bytes.length);
      //        try { raf.write(bytes); } catch (Exception e) {}
      rtpH264.setid(codec.id);
      rtpH264.encode(bytes, 0, bytes.length, PhonePanel.vx, PhonePanel.vy, this);
    } else if (codec.name.equals("H265")) {
      //        printArray("encoded_h265", bytes, 0, bytes.length);
      //        try { raf.write(bytes); } catch (Exception e) {}
      rtpH265.setid(codec.id);
      rtpH265.encode(bytes, 0, bytes.length, PhonePanel.vx, PhonePanel.vy, this);
    } else if (codec.name.equals("H263-1998")) {
      //        printArray("encoded_1998", bytes, 0, bytes.length);
      rtpH263_1998.setid(codec.id);
      rtpH263_1998.encode(bytes, 0, bytes.length, PhonePanel.vx, PhonePanel.vy, this);
    } else if (codec.name.equals("H263-2000")) {
      //        printArray("encoded_2000", bytes, 0, bytes.length);
      rtpH263_2000.setid(codec.id);
      rtpH263_2000.encode(bytes, 0, bytes.length, PhonePanel.vx, PhonePanel.vy, this);
    }
    synchronized (lock) {
      lock.notify();
    }
    return len;
  }

  public long seek(MediaCoder coder, long l, int i) {
    return 0;
  }

  public void onPacket(Packet packet) {
    byte[] copy = new byte[packet.length];
    System.arraycopy(packet.data, packet.offset, copy, 0, packet.length);
    list.add(copy);
  }
}
