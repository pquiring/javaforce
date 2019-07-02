/**
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.voip.*;
import javaforce.media.*;

public class CameraWorker extends Thread implements RTSPClientInterface, RTPInterface, MediaIO {
  public Camera camera;
  private String url;  //camera.url without user:pass@
  private String path;
  private long max_file_size;  //in bytes
  private long max_folder_size;  //in bytes

  private RTSPClient client;
  private SDP sdp;
  private RTP rtp;
  private RTPChannel channel;
  private RTPH264 h264;
  private MediaVideoDecoder decoder;
  private MediaEncoder encoder;
  private RandomAccessFile raf;
  private RandomAccessFile encoder_raf;
  private long file_size;
  private long folder_size;
  private int lastFrame[];
  private int width = -1, height = -1;
  private float fps = -1;
  private Calendar now;
  private boolean recording = false;
  private boolean end_recording = false;
  private int frameCount = 0;
  private boolean active = true;
  private ArrayList<Frame> frames = new ArrayList<Frame>();
  private ArrayList<Packet> packets_decode = new ArrayList<Packet>();
  private ArrayList<Packet> packets_history = new ArrayList<Packet>();
  private Object framesLock = new Object();
  private static Object ffmpeg = new Object();

  private static int nextPort = 5000;
  private static synchronized int getLocalPort() {
    return nextPort++;
  }

  public int read(MediaCoder coder, byte[] buffer) {
//    JFLog.log("read:" + buffer.length);
    return -1;
  }

  public int write(MediaCoder coder, byte[] buffer) {
//    JFLog.log("write:" + buffer.length);
    writeFile(encoder_raf, buffer);
    file_size += buffer.length;
    folder_size += buffer.length;
    return buffer.length;
  }

  public long seek(MediaCoder coder, long pos, int how) {
//    JFLog.log("seek:" + pos + ":" + how);
    long newpos = 0;
    try {
      switch (how) {
        case MediaCoder.SEEK_SET: newpos = pos; break;
        case MediaCoder.SEEK_CUR: long curpos = encoder_raf.getFilePointer(); newpos = pos + curpos; break;
        case MediaCoder.SEEK_END: long size = encoder_raf.length(); newpos = size + pos; break;
      }
      encoder_raf.seek(newpos);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

  /** Frame to be recorded. */
  private static class Frame {
    public Frame(byte[] buffer, RandomAccessFile raf, boolean stop, boolean key_frame) {
      this.buffer = buffer;
      this.raf = raf;
      this.stop = stop;
      this.key_frame = key_frame;
      type = buffer[4] & 0x1f;
    }
    public byte[] buffer;
    public boolean stop;
    public RandomAccessFile raf;
    public int type;
    public boolean key_frame;
    public void closeFile() {
      try { raf.close(); } catch (Exception e) {}
    }
  }

  /** Packet received. */
  private static class Packet {
    public Packet(byte[] buffer) {
      this.buffer = buffer;
      type = buffer[4] & 0x1f;
    }
    public byte[] buffer;
    public int type;
  }

  private static class Recording {
    public File file;
    public long size;
    public long time;
  }

  private ArrayList<Recording> files = new ArrayList<Recording>();

  public CameraWorker(Camera camera) {
    this.camera = camera;
    path = Paths.videoPath + "/" + camera.name;
    max_file_size = camera.max_file_size * 1024L * 1024L;
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
    recording = !camera.record_motion;  //always recording
    JFLog.log("max file size=" + max_file_size);
  }
  public void run() {
    try {
      listFiles();
      connect();
      while (active) {
        JF.sleep(100);
        //clean up folder
        while (folder_size > max_folder_size) {
          Recording rec = files.get(0);
          files.remove(0);
          rec.file.delete();
          folder_size -= rec.size;
        }
        Frame frame = null;
        do {
          synchronized(framesLock) {
            if (frames.size() > 0) {
              frame = frames.remove(0);
            } else {
              frame = null;
            }
          }
          if (frame != null) {
            encoder_raf = frame.raf;
            if (encoder == null) {
              encoder = new MediaEncoder();
              encoder_raf = frame.raf;
              encoder.framesPerKeyFrame = (int)fps;
              encoder.videoBitRate = 4 * 1024 * 1024;  //4Mb/sec
              synchronized(ffmpeg) {
                encoder.start(this, width, height, 24, 0, 0, "mp4", true, false);
              }
            }
            synchronized(ffmpeg) {
              encoder.addVideoEncoded(frame.buffer, frame.key_frame);
            }
            if (frame.stop) {
              synchronized(ffmpeg) {
                encoder.stop();
              }
              encoder = null;
              frame.closeFile();
            }
          }
        } while (frame != null);
      }
      if (encoder != null) {
        synchronized(ffmpeg) {
          encoder.stop();
        }
        encoder = null;
      }
      if (raf != null) {
        closeFile(raf);
        raf = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public void cancel(boolean restart) {
    if (client != null) {
      client.teardown(url);
      client = null;
    }
    if (rtp != null) {
      rtp.stop();
      rtp = null;
    }
    if (channel != null) {
      channel = null;
    }
    if (!restart) {
      active = false;
    }
  }
  public void restart() {
    cancel(true);
    connect();
  }
  public boolean connect() {
    client = new RTSPClient();
    String url = camera.url;
    String uri = null;
    String remotehost = null;
    String user = null;
    String pass = null;
    int remoteport = 554;
    //rtsp://[user:pass@]host[:port]/uri
    if (!url.startsWith("rtsp://")) {
      return false;
    }
    url = url.substring(7);  //remove rtsp://
    int idx = url.indexOf('/');
    if (idx != -1) {
      uri = url.substring(idx);
      url = url.substring(0, idx);
    } else {
      uri = "";
    }
    idx = url.indexOf("@");
    if (idx != -1) {
      String user_pass = url.substring(0, idx);
      url = url.substring(idx+1);
      idx = user_pass.indexOf(':');
      if (idx != -1) {
        user = user_pass.substring(0, idx);
        pass = user_pass.substring(idx+1);
      }
    }
    idx = url.indexOf(':');
    if (idx != -1) {
      remoteport = Integer.valueOf(url.substring(idx+1));
      url = url.substring(0, idx);
    }
    remotehost = url;
    this.url = "rtsp://" + remotehost + ":" + remoteport + uri;
    int localport = getLocalPort();
    client.init(remotehost, remoteport, localport, this, TransportType.TCP);
    if (user != null && pass != null) client.setUserPass(user, pass);
    client.options(this.url);
    return true;
  }

  private void listFiles() {
    File folder = new File(path);
    folder.mkdirs();
    File list[] = folder.listFiles();
    if (list == null) return;
    for(int a=0;a<list.length;a++) {
      Recording rec = new Recording();
      rec.file = list[a];
      rec.size = list[a].length();
      rec.time = list[a].lastModified();
      files.add(rec);
      folder_size += rec.size;
    }
    //sort list : oldest -> newest
    int cnt = files.size();
    for(int a=0;a<cnt-1;a++) {
      Recording ar = files.get(a);
      for(int b=a+1;b<files.size();b++) {
        Recording br = files.get(b);
        long at = ar.time;
        long bt = br.time;
        if (bt < at) {
          files.set(a, br);
          files.set(b, ar);
          ar = br;
        }
      }
    }
  }

  private String getFilename() {
    return String.format("%s/%04d-%02d-%02d_%02d-%02d-%02d.mp4", path
      , now.get(Calendar.YEAR)
      , now.get(Calendar.MONTH) + 1
      , now.get(Calendar.DAY_OF_MONTH)
      , now.get(Calendar.HOUR)
      , now.get(Calendar.MINUTE)
      , now.get(Calendar.SECOND)
    );
  }

  private long last_change_time;
  private void detectMotion(int newFrame[]) {
    float changed = VideoBuffer.compareFrames(lastFrame, newFrame, width, height, 0xe0e0e0);
    if (changed > camera.record_motion_threshold) {
      last_change_time = now.getTimeInMillis();
      recording = true;
    } else {
      if (!recording) return;
      long diff = now.getTimeInMillis() - last_change_time;
      boolean off_delay = diff > camera.record_motion_after;
      if (off_delay) {
        end_recording = true;
      }
    }
  }

  private void createFile() {
    try {
      frameCount = 0;
      file_size = 0;
      raf = new RandomAccessFile(getFilename(), "rw");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void closeFile(RandomAccessFile raf) {
    try {
      raf.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void writeFile(RandomAccessFile raf, byte data[]) {
    try {
      raf.write(data);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void cleanPackets(ArrayList<Packet> packets) {
    //only keep back to the last keyFrame (type 5)
    int last_key_frame = -1;
    int key_frames = 0;
    int cnt = packets.size();
    for(int a=0;a<cnt;a++) {
      switch (packets.get(a).type) {
        case 5: last_key_frame = a; key_frames++; break;
      }
    }
    if (key_frames <= 1) return;
    //delete everything before last_key_frame
    for(int a=0;a<last_key_frame;a++) {
      packets.remove(0);
    }
  }

  private boolean haveCompleteFrame(ArrayList<Packet> packets) {
    int cnt = packets.size();
    for(int a=0;a<cnt;a++) {
      switch (packets.get(a).type) {
        case 1: return true;
        case 5: return true;
      }
    }
    return false;
  }

  private boolean isNextKeyFrame(ArrayList<Packet> packets) {
    int cnt = packets.size();
    for(int a=0;a<cnt;a++) {
      switch (packets.get(a).type) {
        case 1: return false;
        case 5: return true;
      }
    }
    return false;
  }

  private byte[] getNextFrame(ArrayList<Packet> packets) {
    if (!haveCompleteFrame(packets)) return null;
    int total = 0;
    int cnt = packets.size();
    for(int a=0;a<cnt;a++) {
      Packet packet = packets.get(a);
      total += packet.buffer.length;
      if (packet.type == 1 || packet.type == 5) {
        cnt = a;
        break;
      }
    }
    byte full[] = new byte[total];
    int pos = 0;
    for(int a=0;a<=cnt;a++) {
      Packet packet = packets.remove(0);
      System.arraycopy(packet.buffer, 0, full, pos, packet.buffer.length);
      pos += packet.buffer.length;
    }
    return full;
  }

  //RTSPClient Interface

  public void onOptions(RTSPClient client) {
    client.describe(url);
  }

  public void onDescribe(RTSPClient client, SDP sdp) {
    this.sdp = sdp;
    SDP.Stream stream = sdp.getFirstVideoStream();
    if (stream == null) {
      JFLog.log("Error:CameraWorker:onDescribe():SDP does not contain video stream");
      return;
    }
    //IP/port in SDP is all zeros
    stream.setIP(client.getRemoteIP());
    stream.setPort(-1);
    decoder = new MediaVideoDecoder();
    boolean status;
    synchronized(ffmpeg) {
      status = decoder.start(MediaCoder.AV_CODEC_ID_H264, -1, -1);
    }
    if (!status) {
      JFLog.log("Error:MediaVideoDecoder.start() failed");
      return;
    }
    rtp = new RTP();
    rtp.init(this);
    rtp.start();
    channel = rtp.createChannel(stream);
    channel.start();
    h264 = new RTPH264();
    client.setup(url, rtp.getlocalrtpport(), 0);
  }

  public void onSetup(RTSPClient client) {
    client.play(url);
  }

  public void onPlay(RTSPClient client) {
    //connect to RTP stream and start decoding video
  }

  public void onTeardown(RTSPClient client) {
    //stop RTP stream
    rtp.stop();
    rtp = null;
    channel = null;
    synchronized(ffmpeg) {
      decoder.stop();
    }
    decoder = null;
  }

  //RTP Interface

  public void rtpSamples(RTPChannel rtp) {
  }

  public void rtpDigit(RTPChannel rtp, char key) {
  }

  public void rtpPacket(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtcpPacket(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpH263(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpH263_1998(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpH263_2000(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpH264(RTPChannel rtp, byte[] buf, int offset, int length) {
    //I frame : 9 ... 5 (key frame)
    //P frame : 9 ... 1 (diff frame)
    byte buffer[] = h264.decode(Arrays.copyOf(buf, length));
    if (buffer == null) return;
    int type = buffer[4] & 0x1f;
    switch (type) {
      case 7:  //SPS
      case 8:  //PPS
      case 1:  //P frame
      case 5:  //I frame
        break;
      default:
        return;  //all others ignore
    }
    packets_decode.add(new Packet(buffer));
    packets_history.add(new Packet(buffer));
    cleanPackets(packets_decode);
    cleanPackets(packets_history);
    int frame[];
    boolean key_frame = isNextKeyFrame(packets_decode);
    byte full[] = getNextFrame(packets_decode);
    if (full == null) return;
    synchronized(ffmpeg) {
      frame = decoder.decode(full);
      if (width == -1 && height == -1) {
        width = decoder.getWidth();
        height = decoder.getHeight();
        fps = decoder.getFrameRate();
        JFLog.log("detected width/height=" + width + "x" + height);
        JFLog.log("detected FPS=" + fps);
        if (width == 0 || height == 0) return;
        if (width == -1 || height == -1) return;
      }
    }
    now = Calendar.getInstance();
    if (camera.record_motion) {
      detectMotion(frame);
    } else {
      recording = true;  //always recording
      if (file_size > max_file_size) {
        end_recording = true;
      }
    }
    if (recording) {
      if (raf == null) {
        createFile();
        //add everything from packets history
        synchronized(framesLock) {
          do {
            key_frame = isNextKeyFrame(packets_history);
            full = getNextFrame(packets_history);
            if (full == null) break;
            frames.add(new Frame(full, raf, false, key_frame));
          } while (true);
        }
      }
      if (frame != null) {
        frameCount++;
      }
      boolean had_frame = false;
      synchronized(framesLock) {
        key_frame = isNextKeyFrame(packets_history);
        full = getNextFrame(packets_history);
        if (full != null) {
          frames.add(new Frame(full, raf, end_recording, key_frame));
          had_frame = true;
        }
      }
      if (end_recording && had_frame) {
        end_recording = false;
        recording = false;
        raf = null;
      }
    }
  }

  public void rtpVP8(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpJPEG(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpInactive(RTPChannel rtp) {
  }
}
