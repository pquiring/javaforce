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
  private long now;
  private boolean recording = false;
  private boolean end_recording = false;
  private int frameCount = 0;
  private boolean active = true;
  private Frames frames = new Frames();
  private Packets packets_decode = new Packets();
  private Packets packets_encode = new Packets();
  private long lastKeepAlive;
  private long lastPacket;

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

  private static int maxFrames = 64;

  /** Frame to be recorded. */
  private static class Frames {
    public void add(Packets packets, int offset, int length, RandomAccessFile raf, boolean stop, boolean key_frame) {
      this.packets.add(packets, offset, length);
      this.raf[head] = raf;
      this.stop[head] = stop;
      this.key_frame[head] = key_frame;
      type[head] = packets.data[offset + 4] & 0x1f;
      int new_head = head + 1;
      if (new_head == maxFrames) new_head = 0;
      head = new_head;
    }
    public void removeFrame() {
      tail++;
      if (tail == maxFrames) tail = 0;
      packets.removePacket();
    }
    public boolean empty() {
      return tail == head;
    }
    public int head = 0;
    public int tail = 0;
    public Packets packets = new Packets();

    public boolean[] stop = new boolean[maxFrames];
    public RandomAccessFile[] raf = new RandomAccessFile[maxFrames];
    public int[] type = new int[maxFrames];
    public boolean[] key_frame = new boolean[maxFrames];

    public void closeFile() {
      try { raf[tail].close(); } catch (Exception e) {}
    }
  }

  private static int maxPacketsSize = 10 * 1024 * 1024;
  private static int maxPackets = 64;

  /** Packets received. */
  private static class Packets {
    public Packets() {
      data = new byte[maxPacketsSize];
    }
    public byte[] data;
    public int[] offset = new int[maxPackets];
    public int[] length = new int[maxPackets];
    public int[] type = new int[maxPackets];
    public int nextOffset;
    public int head, tail;
    public Object lock = new Object();
    private boolean calcOffset(int _length) {
      if (nextOffset + _length >= maxPacketsSize) {
        nextOffset = 0;
      }
      int next_head = head + 1;
      if (next_head == maxPackets) {
        next_head = 0;
      }
      if (next_head == tail) {
        JFLog.log(1, "Error : Buffer Overflow (# of packets exceeded)");
        return false;
      }
      int _tail = tail;
      if (head == _tail) return true;  //empty
      int h1 = nextOffset;
      int h2 = nextOffset + _length - 1;
      int t1 = offset[_tail];
      int t2 = t1 + length[_tail] - 1;
      if ((h1 >= t1 && h1 <= t2) || (h2 >= t1 && h2 <= t2)) {
        JFLog.log(1, "Error : Buffer Overflow (# of bytes in buffer exceeded)");
        return false;
      }
      return true;
    }
    public void add(Packet packet) {
      synchronized(lock) {
        if (!calcOffset(packet.length)) return;
        System.arraycopy(packet.data, 0, data, nextOffset, packet.length);
        offset[head] = nextOffset;
        length[head] = packet.length;
        type[head] = packet.data[4] & 0x1f;
        nextOffset += packet.length;
        int new_head = head + 1;
        if (new_head == maxPackets) new_head = 0;
        head = new_head;
      }
    }
    public void add(Packets packets, int _offset, int _length) {
      synchronized(lock) {
        if (!calcOffset(_length)) return;
        System.arraycopy(packets.data, _offset, data, nextOffset, _length);
        offset[head] = nextOffset;
        length[head] = _length;
        type[head] = packets.data[_offset + 4] & 0x1f;
        nextOffset += _length;
        int new_head = head + 1;
        if (new_head == maxPackets) new_head = 0;
        head = new_head;
      }
    }
    public void removePacket() {
      synchronized(lock) {
        tail++;
        if (tail == maxPackets) {
          tail = 0;
        }
      }
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
          tail++;
          if (tail == maxPackets) tail = 0;
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
    public void getNextFrame() {
      next_frame_packets = 0;
      next_frame_bytes = 0;
      if (!haveCompleteFrame()) {
        JFLog.log("error : getNextFrame() called but don't have one ???");
        return;
      }
      if (next_frame_fragmented) {
        //packets MUST be re-ordered (keep buffer size large to avoid this)
        JFLog.log("re-ordering packets");
        int new_tail = head;
        for(int pos=tail;pos!=head;) {
          add(this, offset[pos], length[pos]);
          pos++;
          if (pos == maxPackets) pos = 0;
        }
        tail = new_tail;
      }
      for(int pos=tail;pos!=head;) {
        next_frame_bytes += length[pos];
        next_frame_packets++;
        int this_type = type[pos];
        if (this_type == 1 || this_type == 5) {
          break;
        }
        pos++;
        if (pos == maxPackets) pos = 0;
      }
    }
    public void removeNextFrame() {
      while (next_frame_packets > 0) {
        tail++;
        if (tail == maxPackets) {
          tail = 0;
        }
        next_frame_packets--;
      }
    }
    public boolean next_frame_fragmented;
    public int next_frame_packets;
    public int next_frame_bytes;
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
  }

  public void run() {
    try {
      listFiles();
      JFLog.log(1, camera.name + " : Connecting");
      connect();
      while (active) {
        JF.sleep(100);
        long now = System.currentTimeMillis();
        if (now - lastPacket > 10*1000) {
          JFLog.log(1, camera.name + " : Reconnecting");
          disconnect();
          connect();
        } else if (now - lastKeepAlive > 55*1000) {
          JFLog.log(1, camera.name + " : keep alive");
          client.keepalive(url);
          lastKeepAlive = now;
        }
        //clean up folder
        while (folder_size > max_folder_size) {
          Recording rec = files.get(0);
          files.remove(0);
          rec.file.delete();
          folder_size -= rec.size;
        }
        do {
          if (frames.empty()) break;
          int tail = frames.tail;
          encoder_raf = frames.raf[tail];
          if (encoder == null) {
            encoder = new MediaEncoder();
            encoder.framesPerKeyFrame = (int)fps;
            encoder.videoBitRate = 4 * 1024 * 1024;  //4Mb/sec
            encoder.start(this, width, height, (int)fps, 0, 0, "mp4", true, false);
          }
          synchronized(frames.packets.lock) {
            JFLog.log("add:" + frames.packets.offset[frames.packets.tail]);
            encoder.addVideoEncoded(frames.packets.data, frames.packets.offset[frames.packets.tail], frames.packets.length[frames.packets.tail], frames.key_frame[tail]);
          }
          if (frames.stop[tail]) {
            encoder.stop();
            encoder = null;
            frames.closeFile();
          }
          frames.removeFrame();
        } while (true);
      }
      disconnect();
      if (encoder != null) {
        encoder.stop();
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
    //reset values
    width = -1;
    height = -1;
    fps = -1;
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
    long now = System.currentTimeMillis();
    lastKeepAlive = now;
    lastPacket = now;
    return true;
  }

  public void disconnect() {
    client.uninit();
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
      , now.get(Calendar.HOUR)
      , now.get(Calendar.MINUTE)
      , now.get(Calendar.SECOND)
    );
  }

  private long last_change_time;
  private void detectMotion(int newFrame[]) {
    float changed = VideoBuffer.compareFrames(lastFrame, newFrame, width, height, 0xe0e0e0);
    System.arraycopy(newFrame, 0, lastFrame, 0, width * height);
    if (changed > camera.record_motion_threshold) {
      last_change_time = now;
      recording = true;
    } else {
      if (!recording) return;
      long diff = (now - last_change_time) * 1000L;
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
    status = decoder.start(MediaCoder.AV_CODEC_ID_H264, -1, -1);
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
    decoder.stop();
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
    JFLog.log("packet = " + (packet.data[4] & 0x1f) + " : " + packet.length);
    packets_decode.add(packet);
    packets_encode.add(packet);
    packets_decode.cleanPackets(true);
    packets_encode.cleanPackets(true);
    int frame[];
    boolean key_frame = packets_decode.isNextFrame_KeyFrame();
    if (!packets_decode.haveCompleteFrame()) return;
    packets_decode.getNextFrame();
    frame = decoder.decode(packets_decode.data, packets_decode.offset[packets_decode.tail], packets_decode.next_frame_bytes);
    packets_decode.removeNextFrame();
    if (width == -1 && height == -1) {
      width = decoder.getWidth();
      height = decoder.getHeight();
      fps = decoder.getFrameRate();
      JFLog.log(1, camera.name + " : detected width/height=" + width + "x" + height);
      JFLog.log(1, camera.name + " : detected FPS=" + fps);
      if (width == 0 || height == 0) return;
      if (width == -1 || height == -1) return;
      lastFrame = new int[width * height];
    }
    now = lastPacket;
    if (camera.record_motion) {
      detectMotion(frame);
    } else {
      recording = true;  //always recording
    }
    if (recording) {
      if (frameCount > 100) {
        end_recording = true;  //test
      }
      if (file_size > max_file_size) {
        end_recording = true;
      }
      if (raf == null) {
        createFile();
        //add everything from packets history
        do {
          if (!packets_encode.haveCompleteFrame()) break;
          key_frame = packets_encode.isNextFrame_KeyFrame();
          packets_encode.getNextFrame();
          frames.add(packets_encode, packets_encode.offset[packets_encode.tail], packets_encode.next_frame_bytes, raf, false, key_frame);
          frameCount++;
          packets_encode.removeNextFrame();
        } while (true);
      }
      frameCount++;
      boolean had_frame = false;
      if (packets_encode.haveCompleteFrame()) {
        packets_encode.getNextFrame();
        key_frame = packets_encode.isNextFrame_KeyFrame();
        frames.add(packets_encode, packets_encode.offset[packets_encode.tail], packets_encode.next_frame_bytes, raf, end_recording, key_frame);
        had_frame = true;
        packets_encode.removeNextFrame();
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
