/** CameraWorker
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

  private final static boolean debug_buffers = false;
  private final static boolean debug_motion = false;
  private final static boolean debug_motion_image = false;
  private final static boolean debug_short_clips = false;

  private RTSPClient client;
  private SDP sdp;
  private RTP rtp;
  private RTPChannel channel;
  private RTPH264 h264;
  private MediaVideoDecoder decoder;
  private MediaEncoder encoder;
  private RandomAccessFile raf;
  private long file_size;
  private long file_pos;
  private long folder_size;
  private int width = -1, height = -1;
  private float fps = -1;
  private long now;
  private boolean recording = false;
  private boolean end_recording = false;
  private int frameCount = 0;
  private boolean active = true;
  private Frames frames;
  private Packets packets_decode;
  private Packets packets_encode;
  private long lastKeepAlive;
  private long lastPacket;
  private int last_frame[];
  private int decoded_frame[];
  private static final int decoded_x = 320;
  private static final int decoded_y = 200;
  private static final int decoded_xy = 320 * 200;
  private JFImage preview_image;
  private boolean wait_next_key_frame;
  private int log;

  public int read(MediaCoder coder, byte[] buffer) {
//    JFLog.log(log, "read:" + buffer.length);
    return -1;
  }

  public int write(MediaCoder coder, byte[] buffer) {
//    JFLog.log(log, "write:" + buffer.length);
    writeFile(buffer);
    if (file_pos == file_size) {
      file_size += buffer.length;
      folder_size += buffer.length;
    }
    file_pos += buffer.length;
    return buffer.length;
  }

  public long seek(MediaCoder coder, long pos, int how) {
//    JFLog.log(log, "seek:" + pos + ":" + how);
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
    file_pos = newpos;
    return newpos;
  }

  private static int maxFrames = 64;

  /** Frame to be recorded. */
  private static class Frames {
    public Frames(int log) {
      this.log = log;
      packets = new Packets(log);
    }
    public void stop() {
      this.stop[head] = true;
      int new_head = head + 1;
      if (new_head == maxFrames) new_head = 0;
      head = new_head;
    }
    public void add(Packet packet, boolean key_frame) {
      this.packets.add(packet);
      this.stop[head] = false;
      this.key_frame[head] = key_frame;
      type[head] = packets.data[packet.offset + 4] & 0x1f;
      int new_head = head + 1;
      if (new_head == maxFrames) new_head = 0;
      head = new_head;
    }
    public void removeFrame() {
      if (tail == head) {
        JFLog.log(log, "Error:Frames Buffer underflow");
        return;
      }
      if (!stop[tail]) {
        packets.removePacket();
      }
      int new_tail = tail + 1;
      if (new_tail == maxFrames) new_tail = 0;
      tail = new_tail;
    }
    public boolean empty() {
      return tail == head;
    }
    public int head = 0;
    public int tail = 0;
    public Packets packets;
    public int log;

    public boolean[] stop = new boolean[maxFrames];
    public int[] type = new int[maxFrames];
    public boolean[] key_frame = new boolean[maxFrames];
  }

  private static int maxPacketsSize = 16 * 1024 * 1024;
  private static int maxPackets = 256;

  /** Packets received. */
  private static class Packets {
    public Packets(int log) {
      this.log = log;
      data = new byte[maxPacketsSize];
      frag_data = new byte[maxPacketsSize];
    }
    public byte[] data;
    private byte[] frag_data;
    private Packet nextFrame = new Packet();
    public int[] offset = new int[maxPackets];
    public int[] length = new int[maxPackets];
    public int[] type = new int[maxPackets];
    public int nextOffset;
    public int head, tail;
    public int log;
    private void reset() {
      //TODO : need to lock this from consumer
      head = 0;
      tail = 0;
      nextOffset = 0;
    }
    private boolean calcOffset(int _length) {
      if (nextOffset + _length >= maxPacketsSize) {
        nextOffset = 0;
      }
      int next_head = head + 1;
      if (next_head == maxPackets) {
        next_head = 0;
      }
      if (next_head == tail) {
        JFLog.log(log, "Error : Buffer Overflow (# of packets exceeded)");
        reset();
        return false;
      }
      int _tail = tail;
      if (head == _tail) return true;  //empty
      int h1 = nextOffset;
      int h2 = nextOffset + _length - 1;
      int t1 = offset[_tail];
      int t2 = t1 + length[_tail] - 1;
      if ((h1 >= t1 && h1 <= t2) || (h2 >= t1 && h2 <= t2)) {
        JFLog.log(log, "Error : Buffer Overflow (# of bytes in buffer exceeded)");
        reset();
        return false;
      }
      return true;
    }
    public void add(Packet packet) {
      if (!calcOffset(packet.length)) return;
      try {
        System.arraycopy(packet.data, packet.offset, data, nextOffset, packet.length);
      } catch (Exception e) {
        JFLog.log(log, e);
        System.out.println("packet:" + packet.length);
        return;
      }
      offset[head] = nextOffset;
      length[head] = packet.length;
      type[head] = packet.data[4] & 0x1f;
      nextOffset += packet.length;
      int new_head = head + 1;
      if (new_head == maxPackets) new_head = 0;
      head = new_head;
    }
    public void removePacket() {
      if (tail == head) {
        JFLog.log(log, "Error:Packets Buffer underflow");
        return;
      }
      int new_tail = tail + 1;
      if (new_tail == maxPackets) new_tail = 0;
      tail = new_tail;
    }
    public void cleanPackets(boolean mark) {
      //only keep back to the last keyFrame (type 5)
      int key_frames = 0;
      for(int pos=tail;pos!=head;) {
        switch (type[pos]) {
          case 5: key_frames++; break;
        }
        pos++;
        if (pos == maxPackets) pos = 0;
      }
      if (key_frames <= 1) return;
      if (mark) {
        boolean i_frame = false;
        for(;tail!=head;) {
          switch (type[tail]) {
            case 1: i_frame = true; break;
            default: if (i_frame) return;
          }
          int new_tail = tail + 1;
          if (new_tail == maxPackets) new_tail = 0;
          tail = new_tail;
        }
      }
    }
    public boolean haveCompleteFrame() {
      next_frame_fragmented = false;
      for(int pos=tail;pos!=head;) {
        switch (type[pos]) {
          case 1: return true;
          case 5: return true;
        }
        pos++;
        if (pos == maxPackets) {
          pos = 0;
          next_frame_fragmented = true;
        }
      }
      return false;
    }
    public boolean isNextFrame_KeyFrame() {
      for(int pos=tail;pos!=head;) {
        switch (type[pos]) {
          case 1: return false;
          case 5: return true;
        }
        pos++;
        if (pos == maxPackets) pos = 0;
      }
      return false;
    }
    public Packet getNextFrame() {
      next_frame_packets = 0;
      if (!haveCompleteFrame()) {
        JFLog.log(log, "Error : getNextFrame() called but don't have one ???");
        return null;
      }
      if (next_frame_fragmented) {
        JFLog.log(log, "Warning:frame is fragmented");
        nextFrame.data = frag_data;
        nextFrame.offset = 0;
      } else {
        nextFrame.data = data;
        nextFrame.offset = offset[tail];
      }
      nextFrame.length = 0;
      int frag_pos = 0;
      for(int pos=tail;pos!=head;) {
        if (next_frame_fragmented) {
          System.arraycopy(data, offset[pos], frag_data, frag_pos, length[pos]);
          frag_pos += length[pos];
        }
        nextFrame.length += length[pos];
        next_frame_packets++;
        int this_type = type[pos];
        if (this_type == 1 || this_type == 5) {
          break;
        }
        pos++;
        if (pos == maxPackets) pos = 0;
      }
      return nextFrame;
    }
    public void removeNextFrame() {
      while (next_frame_packets > 0) {
        int new_tail = tail + 1;
        if (new_tail == maxPackets) new_tail = 0;
        tail = new_tail;
        next_frame_packets--;
      }
    }
    public boolean next_frame_fragmented;
    public int next_frame_packets;
    public String toString() {
      return "Packets:tail=" + tail + ":head=" + head;
    }
  }

  private static class Recording {
    public File file;
    public long size;
    public long time;
  }

  private static int next_log = 1;
  private static synchronized int nextLog() {
    return next_log++;
  }

  private ArrayList<Recording> files = new ArrayList<Recording>();

  public CameraWorker(Camera camera) {
    log = nextLog();
    JFLog.append(log, Paths.logsPath + "/cam-" + camera.name + ".log", false);
    JFLog.setRetention(log, 5);
    JFLog.log(log, "Camera=" + camera.name);
    this.camera = camera;
    path = Paths.videoPath + "/" + camera.name;
    max_file_size = camera.max_file_size * 1024L * 1024L;
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
    recording = !camera.record_motion;  //always recording
    frames = new Frames(log);
    packets_decode = new Packets(log);
    packets_encode = new Packets(log);
    preview_image = new JFImage(decoded_x, decoded_y);
  }

  public void cancel() {
    if (client != null) {
      client.teardown(url);
      client.uninit();
      client = null;
    }
    active = false;
  }

  public void run() {
    boolean idle = false;
    try {
      listFiles();
      if (!connect()) return;
      while (active) {
        if (idle) {
          JF.sleep(50);
        } else {
          idle = true;
        }
        long now = System.currentTimeMillis();
        if (now - lastPacket > 10*1000) {
          JFLog.log(log, camera.name + " : Reconnecting");
          disconnect();
          connect();
          idle = false;
        } else if (now - lastKeepAlive > 55*1000) {
          JFLog.log(log, camera.name + " : keep alive");
          client.keepalive(url);
          lastKeepAlive = now;
        }
        //clean up folder
        while (folder_size > max_folder_size) {
          Recording rec = files.get(0);
          files.remove(0);
          rec.file.delete();
          folder_size -= rec.size;
          JFLog.log(log, "delete recording:" + rec.file.getName());
        }
        //update preview
        if (camera.viewing && camera.update_preview) {
          int px[] = decoded_frame;
          if (px != null) {
            preview_image.putPixels(px, 0, 0, decoded_x, decoded_y, 0);
            ByteArrayOutputStream preview = new ByteArrayOutputStream();
            preview_image.savePNG(preview);
            camera.preview = preview.toByteArray();
            camera.update_preview = false;
          }
        }
        do {
          if (file_size > max_file_size) {
            JFLog.log(log, camera.name + " : max file size");
            if (encoder != null) {
              encoder.stop();
              encoder = null;
            }
            if (raf != null) {
              closeFile();
              raf = null;
            }
            idle = false;
            break;
          }
          if (frames.empty()) break;
          idle = false;
          int frame_tail = frames.tail;
          if (frames.stop[frame_tail]) {
            if (encoder != null) {
              encoder.stop();
              encoder = null;
            }
            if (raf != null) {
              closeFile();
              raf = null;
            }
            frames.removeFrame();
            break;
          }
          if (raf == null) {
            createFile();
          }
          if (encoder == null) {
            encoder = new MediaEncoder();
            encoder.framesPerKeyFrame = (int)fps;
            encoder.videoBitRate = 4 * 1024 * 1024;  //4Mb/sec
            if (!encoder.start(this, width, height, (int)fps, 0, 0, "mp4", true, false)) {
              JFLog.log(log, "Error:Encoder.start() failed! : fps=" + fps);
              encoder = null;
              break;
            }
          }
          int packet_tail = frames.packets.tail;
          encoder.addVideoEncoded(frames.packets.data, frames.packets.offset[packet_tail], frames.packets.length[packet_tail], frames.key_frame[frame_tail]);
          frames.removeFrame();
        } while (true);
      }
      disconnect();
      if (encoder != null) {
        encoder.stop();
        encoder = null;
      }
      if (raf != null) {
        closeFile();
        raf = null;
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    JFLog.log(log, ":closing");
    JFLog.close(log);
  }
  public boolean connect() {
    //reset values
    width = -1;
    height = -1;
    fps = -1;
    client = new RTSPClient();
    client.setLog(log);
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
    JFLog.log(log, camera.name + " : Connecting");
    if (!client.init(remotehost, remoteport, getLocalPort(), this, TransportType.TCP)) {
      System.out.println("RTSP init failed");
      return false;
    }
    if (user != null && pass != null) client.setUserPass(user, pass);
    client.options(this.url);
    long now = System.currentTimeMillis();
    lastKeepAlive = now;
    lastPacket = now;
    return true;
  }

  public void disconnect() {
    client.uninit();
    if (decoder != null) {
      decoder.stop();
      decoder = null;
    }
  }

  private static int nextPort = 5000;
  private static synchronized int getLocalPort() {
    if (nextPort > 10000) nextPort = 5000;
    int port = nextPort;
    nextPort += 2;
    return port;
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
    Calendar now = Calendar.getInstance();
    return String.format("%s/%04d-%02d-%02d_%02d-%02d-%02d.mp4", path
      , now.get(Calendar.YEAR)
      , now.get(Calendar.MONTH) + 1
      , now.get(Calendar.DAY_OF_MONTH)
      , now.get(Calendar.HOUR_OF_DAY)
      , now.get(Calendar.MINUTE)
      , now.get(Calendar.SECOND)
    );
  }

  private long last_change_time;
  private int imgcnt;
  private void detectMotion(int newFrame[], boolean key_frame) {
    if (newFrame == null) {
      return;
    }
    float changed = VideoBuffer.compareFrames(last_frame, newFrame, decoded_x, decoded_y);
    camera.motion_value = changed;
    if (debug_motion && key_frame) {
      System.out.println(camera.name + ":changed=" + changed);
      if (debug_motion_image) {
        JFImage img = new JFImage(decoded_x, decoded_y);
        int size = decoded_x * decoded_y;
        int px,r,g,b;
        for(int i=0;i<size;i++) {
          //_mm_adds_epu8()
          px = newFrame[i];
          r = (px & 0xff0000) >> 16;
          g = (px & 0xff00) >> 8;
          b = (px & 0xff);
          r += 8;
          if (r > 255) r = 255;
          g += 8;
          if (g > 255) g = 255;
          b += 8;
          if (b > 255) b = 255;
          px = 0xff000000 + (r << 16) + (g << 8) + b;
          newFrame[i] = px & 0xfff0f0f0;
        }
        img.putPixels(newFrame, 0, 0, decoded_x, decoded_y, 0);
        String tmpfile = "temp-" + (imgcnt++) + ".png";
        JFLog.log(log, "Debug:Saving motion image to:" + tmpfile);
        img.savePNG(tmpfile);
      }
    }
    System.arraycopy(newFrame, 0, last_frame, 0, decoded_xy);
    if (changed > camera.record_motion_threshold) {
      last_change_time = now;
      recording = true;
    } else {
      if (!recording) return;
      long diff = (now - last_change_time) / 1000L;
      boolean off_delay = diff > camera.record_motion_after;
      if (off_delay) {
        end_recording = true;
      }
    }
    if (debug_short_clips && recording && frameCount > 200) {
      System.out.println("Debug:end recording @ 200 frames");
      end_recording = true;
      frameCount = 0;
    }
  }

  private String filename;

  private void createFile() {
    try {
      frameCount = 0;
      file_size = 0;
      file_pos = 0;
      filename = getFilename();
      raf = new RandomAccessFile(filename, "rw");
      JFLog.log(log, camera.name + " : createFile:" + filename);
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void closeFile() {
    if (raf == null) return;
    try {
      raf.close();
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    raf = null;
    Recording rec = new Recording();
    rec.file = new File(filename);
    rec.size = rec.file.length();
    if (rec.size != file_size) {
      JFLog.log(log, "Error:file size mismatch");
    }
    rec.time = rec.file.lastModified();
    files.add(rec);
    frameCount = 0;
    file_size = 0;
  }

  private void writeFile(byte data[]) {
    try {
      raf.write(data);
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public void reloadConfig() {
    JFLog.log(log, "Reloading config");
    max_file_size = camera.max_file_size * 1024L * 1024L;
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
    if (!camera.record_motion) {
      recording = true;
    }
  }

  //RTSPClient Interface

  public void onOptions(RTSPClient client) {
    client.describe(url);
  }

  public void onDescribe(RTSPClient client, SDP sdp) {
    this.sdp = sdp;
    SDP.Stream stream = sdp.getFirstVideoStream();
    if (stream == null) {
      JFLog.log(log, "Error:CameraWorker:onDescribe():SDP does not contain video stream");
      return;
    }
    //IP/port in SDP is all zeros
    stream.setIP(client.getRemoteIP());
    stream.setPort(-1);
    if (decoder != null) {
      decoder.stop();
      decoder = null;
    }
    decoder = new MediaVideoDecoder();
    boolean status;
    status = decoder.start(MediaCoder.AV_CODEC_ID_H264, decoded_x, decoded_y);
    if (!status) {
      JFLog.log(log, "Error:MediaVideoDecoder.start() failed");
      return;
    }
    rtp = new RTP();
    rtp.setLog(log);
    rtp.init(this);
    rtp.start();
    channel = rtp.createChannel(stream);
    channel.start();
    h264 = new RTPH264();
    h264.setLog(log);
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
    if (rtp != null) {
      rtp.stop();
      rtp = null;
    }
    channel = null;
    if (decoder != null) {
      decoder.stop();
      decoder = null;
    }
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
    try {
      //I frame : 9 ... 5 (key frame)
      //P frame : 9 ... 1 (diff frame)
      lastPacket = System.currentTimeMillis();
      Packet packet = h264.decode(buf, 0, length);
      if (packet == null) {
        return;
      }
      int type = packet.data[4] & 0x1f;
      switch (type) {
        case 7:  //SPS
        case 8:  //PPS
        case 1:  //P frame
        case 5:  //I frame
          break;
        default:
          return;  //all others ignore
      }
      packets_decode.add(packet);
      packets_encode.add(packet);
      packets_decode.cleanPackets(true);
      packets_encode.cleanPackets(true);
      if (!packets_decode.haveCompleteFrame()) return;
      boolean key_frame = packets_decode.isNextFrame_KeyFrame();
      if (debug_buffers && key_frame) {
        JFLog.log(log, "packets_decode=" + packets_decode.toString());
        JFLog.log(log, "packets_encode=" + packets_decode.toString());
      }
      if (wait_next_key_frame) {
        if (!key_frame) {
          packets_decode.reset();
          packets_encode.reset();
          return;
        }
        wait_next_key_frame = false;
      }
      Packet nextPacket = packets_decode.getNextFrame();
      decoded_frame = decoder.decode(nextPacket.data, nextPacket.offset, nextPacket.length);
      if (decoded_frame == null) {
        JFLog.log(log, camera.name + ":Error:newFrame == null:packet.length=" + nextPacket.length);
        packets_decode.reset();
        packets_encode.reset();
        //decoding error : delete all frames till next key frame
        wait_next_key_frame = true;
        return;
      }
      packets_decode.removeNextFrame();
      if (width == -1 && height == -1) {
        width = decoder.getWidth();
        height = decoder.getHeight();
        fps = decoder.getFrameRate();
        JFLog.log(log, camera.name + " : detected width/height=" + width + "x" + height);
        JFLog.log(log, camera.name + " : detected FPS=" + fps);
        JFLog.log(log, camera.name + " : threshold=" + camera.record_motion_threshold + ":after=" + camera.record_motion_after);
        if (width == 0 || height == 0) {
          width = -1;
          height = -1;
          return;
        }
        if (width == -1 || height == -1) {
          width = -1;
          height = -1;
          return;
        }
        last_frame = new int[decoded_xy];
      }
      now = lastPacket;
      if (camera.record_motion) {
        detectMotion(decoded_frame, key_frame);
      } else {
        recording = true;  //always recording
      }
      if (recording) {
        while (packets_encode.haveCompleteFrame()) {
          key_frame = packets_encode.isNextFrame_KeyFrame();
          nextPacket = packets_encode.getNextFrame();
          frames.add(nextPacket, key_frame);
          frameCount++;
          packets_encode.removeNextFrame();
        }
        if (end_recording) {
          frames.stop();
          end_recording = false;
          recording = false;
        }
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public void rtpVP8(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpJPEG(RTPChannel rtp, byte[] buf, int offset, int length) {
  }

  public void rtpInactive(RTPChannel rtp) {
  }
}
