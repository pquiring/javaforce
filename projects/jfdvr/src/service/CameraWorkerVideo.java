package service;

/** CameraWorkerVideo
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.voip.*;
import javaforce.media.*;

public class CameraWorkerVideo extends Thread implements RTSPClientInterface, RTPInterface, MediaIO, CameraWorker, PacketReceiver {
  public Camera camera;
  private String url;
  private String path;
  private long max_file_size;  //in bytes
  private long max_folder_size;  //in bytes

  public static boolean debug = false;
  private static boolean debug_encoder = false;
  private static boolean debug_decoder = false;
  private static boolean debug_buffers = false;
  private static boolean debug_motion = false;
  private static boolean debug_motion_image = false;

  private RTSPClient client;
  private RTP rtp;
  private RTPChannel channel;
  private RTPH264 h264;
  private RTPH265 h265;
  private MediaVideoDecoder decoder;
  private MediaEncoder encoder;
  private RandomAccessFile raf;
  private long file_size;
  private long file_pos;
  private long folder_size;
  private int width = -1;
  private int height = -1;
  private float fps = -1;
  private long now;
  private boolean recording = false;
  private int frameCount = 0;
  private boolean active = true;
  private PacketBuffer packets_decode;
  private PacketBuffer packets_encode;
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
  private boolean isEncoder;  //viewing, recording
  private boolean isDecoder;  //decoding, preview, motion detection
  private CameraWorker viewer;  //used by decoding to signal recording state to viewer

  //MediaCoder Interface

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

  private static class Recording {
    public File file;
    public long size;
    public long time;
  }

  private ArrayList<Recording> files = new ArrayList<Recording>();

  public CameraWorkerVideo(Camera camera, String url, boolean inEncoder, boolean isDecoder, CameraWorker viewer) {
    log = Config.nextLog();
    JFLog.append(log, Paths.logsPath + "/cam-" + camera.name + ".log", true);
    JFLog.setRetention(log, 5);
    JFLog.log(log, "CameraWorkerVideo:" + url + ":viewer=" + inEncoder + ":decoding=" + isDecoder);
    this.url = url;
    this.isEncoder = inEncoder;
    this.isDecoder = isDecoder;
    this.viewer = viewer;
    if (inEncoder && isDecoder) {
      this.viewer = this;
    }
    if (viewer == null) {
      JFLog.log(log, "Error:viewer = null");
    }
    JFLog.log(log, "Camera=" + camera.name);
    this.camera = camera;
    path = Paths.videoPath + "/" + camera.name;
    max_file_size = camera.max_file_size * 1024L * 1024L;
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
    recording = inEncoder && !camera.record_motion;  //always recording
    if (isDecoder) {
      preview_image = new JFImage(decoded_x, decoded_y);
    }
  }

  public void run() {
    setName("CameraWorkerVideo");
    JFLog.log(log, "CameraWorkerVideo:start");
    try {
      if (isEncoder) {
        listFiles();
      }
      connect();
      while (active) {
        JF.sleep(250);
        long now = System.currentTimeMillis();
        if (now - lastPacket > 10*1000) {
          JFLog.log(log, camera.name + " : Reconnecting");
          disconnect();
          if (!connect()) {
            JF.sleep(1000);
            continue;
          }
        } else if (now - lastKeepAlive > 45*1000) {
          JFLog.log(log, camera.name + " : keep alive");
          client.keepalive(url);
          lastKeepAlive = now;
        }
        if (isEncoder) {
          doEncoder();
        }
        if (isDecoder) {
          doDecoder();
        }
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    try {
      disconnect();
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    try {
      if (raf != null) {
        closeFile();
        raf = null;
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    JFLog.log(log ,"CameraWorkerVideo:stop");
    JFLog.close(log);
  }

  private void doEncoder() {
    //this should be a thread
    try {
      //clean up folder
      while (folder_size > max_folder_size) {
        Recording rec = files.get(0);
        files.remove(0);
        rec.file.delete();
        folder_size -= rec.size;
        JFLog.log(log, "delete recording:" + rec.file.getName());
      }
      if (packets_encode == null) return;
      if (width == -1 || height == -1) {
        //decode one frame to get width/height
        if (h264 != null) {
          if (!packets_encode.haveCompleteFrame()) return;
          Packet fullPacket = packets_encode.getNextFrame();
          CodecInfo info = RTPH264.getCodecInfo(fullPacket);
          if (info == null || info.width == 0 || info.height == 0) {
            JFLog.log(log, "Error:Unable to determine stream info");
            return;
          }
          JFLog.log(log, "Viewer:dim=" + info.width + "x" + info.height + ":" + info.fps);
          this.width = info.width;
          this.height = info.height;
          if (fps == -1) {
            fps = info.fps;
            camera.fps = fps;
          }
        }
        if (h265 != null) {
          if (!packets_encode.haveCompleteFrame()) return;
          Packet fullPacket = packets_encode.getNextFrame();
          CodecInfo info = RTPH265.getCodecInfo(fullPacket);
          if (info == null || info.width == 0 || info.height == 0) {
            JFLog.log(log, "Error:Unable to determine stream info");
            return;
          }
          JFLog.log(log, "Viewer:dim=" + info.width + "x" + info.height + ":" + info.fps);
          width = info.width;
          height = info.height;
          if (fps == -1) {
            fps = info.fps;
            camera.fps = fps;
          }
        }
      }
      if (!packets_encode.haveCompleteFrame()) {
        if (debug_encoder) JFLog.log(log, "no complete frame");
        return;
      }
      boolean key_frame = packets_encode.isNextFrame_KeyFrame();
      if (debug_encoder && key_frame) {
        JFLog.log(log, "encoder:key_frame=" + packets_encode.toString());
      }
      Packet fullPacket;
      while (packets_encode.haveCompleteFrame()) {
        key_frame = packets_encode.isNextFrame_KeyFrame();
        fullPacket = packets_encode.getNextFrame();
        if (debug_encoder) JFLog.log(log, "encoder:add full packet");
        if (recording) {
          recordFrame(fullPacket, key_frame);
          frameCount++;
        }
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void recordFrame(Packet packet, boolean key_frame) {
    try {
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
      }
      if (raf == null) {
        createFile();
      }
      if (encoder == null) {
        if (width == -1 || height == -1 || fps == -1) {
          JFLog.log(log, "Unable to start encoder:res=" + width + "x" + height + ",fps=" + fps);
          return;
        }
        encoder = new MediaEncoder();
        encoder.framesPerKeyFrame = (int)fps;
        encoder.videoBitRate = 4 * 1024 * 1024;  //4Mb/sec
        if (!encoder.start(this, width, height, (int)fps, 0, 0, "mp4", true, false)) {
          JFLog.log(log, "Error:Encoder.start() failed! : res=" + width + "x" + height + ",fps=" + fps);
          encoder = null;
          return;
        }
      }
      encoder.addVideoEncoded(packet.data, 0, packet.length, key_frame);
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void doDecoder() {
    //this should be a thread
    try {
      if (packets_decode == null) return;
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
      while (packets_decode.haveCompleteFrame()) {
        boolean key_frame = packets_decode.isNextFrame_KeyFrame();
        if (debug_buffers && key_frame) {
          JFLog.log(log, "packets_decode=" + packets_decode.toString());
        }
        if (wait_next_key_frame) {
          if (!key_frame) {
            //this is i-frame - discard and wait for key-frame
            packets_decode.reset();
            return;
          }
          wait_next_key_frame = false;
        }
        Packet fullPacket = packets_decode.getNextFrame();
        decoded_frame = decoder.decode(fullPacket.data, fullPacket.offset, fullPacket.length);
        if (decoded_frame == null) {
          JFLog.log(log, camera.name + ":Error:newFrame == null:packet.length=" + fullPacket.length);
          packets_decode.reset();
          //decoding error : delete all frames till next key frame
          wait_next_key_frame = true;
          return;
        }
        if (width == -1 && height == -1) {
          width = decoder.getWidth();
          height = decoder.getHeight();
          if (width == -1 || height == -1 || width == 0 || height == 0) {
            width = -1;
            height = -1;
            return;
          }
          if (fps == -1) {
            //should come from SDP
            fps = decoder.getFrameRate();
          }
          JFLog.log(log, camera.name + " : detected : sizet=" + width + "x" + height + ":fps=" + fps);
          last_frame = new int[decoded_xy];
        }
        if (camera.record_motion) {
          detectMotion(decoded_frame, key_frame);
        }
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public boolean connect() {
    //reset values
    width = -1;
    height = -1;
    fps = -1;
    client = new RTSPClient();
    client.setLog(log);
    String user = null;
    String pass = null;
    //rtsp://[user:pass@]host[:port]/uri
    if (!url.startsWith("rtsp://")) {
      JFLog.log(log, "Error:Invalid URL:" + url);
      return false;
    }
    String user_pass = RTSPURL.getUserInfo(url);
    if (user_pass != null) {
      int idx = user_pass.indexOf(':');
      user = user_pass.substring(0, idx);
      pass = user_pass.substring(idx+1);
    }
    String remotehost = RTSPURL.getHost(url);
    int remoteport = RTSPURL.getPort(url);
    if (remoteport == -1) remoteport = 554;
    if (debug) JFLog.log(log, camera.name + " : Connecting : viewer=" + isEncoder + ",decoding=" + isDecoder + ",remoteport=" + remoteport);
    if (!client.init(remotehost, remoteport, Config.getLocalPort(), this, TransportType.TCP)) {
      JFLog.log(log, "RTSP init failed");
      client = null;
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
    if (client != null) {
      try {
        client.teardown(url);
      } catch (Exception e) {}
      try {
        client.uninit();
      } catch (Exception e) {}
      client = null;
    }
    if (rtp != null) {
      rtp.uninit();
      rtp = null;
    }
    if (channel != null) {
      //no uninit
      channel = null;
    }
    if (decoder != null) {
      decoder.stop();
      decoder = null;
    }
    if (encoder != null) {
      encoder.stop();
      encoder = null;
    }
    packets_decode = null;
    packets_encode = null;
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
    if (newFrame == null || last_frame == null) {
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
    if (viewer == null) {
      JFLog.log(log, "Error:viewer==null");
      return;
    }
    if (changed > camera.record_motion_threshold) {
      last_change_time = now;
      viewer.setRecording(true);
    } else {
      if (!viewer.isRecording()) return;
      long diff = (now - last_change_time) / 1000L;
      boolean off_delay = diff > camera.record_motion_after;
      if (off_delay) {
        viewer.setRecording(false);
      }
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

  //CameraWorker interface

  public void cancel() {
    active = false;
  }

  public void reloadConfig() {
    JFLog.log(log, "Reloading config");
    max_file_size = camera.max_file_size * 1024L * 1024L;
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
    if (isEncoder && !camera.record_motion) {
      recording = true;
    }
  }

  public Camera getCamera() {
    return camera;
  }

  public void setRecording(boolean state) {
    if (state) {
      recording = true;
    } else {
      recording = false;
    }
  }

  public boolean isRecording() {
    return recording;
  }

  //RTSPClient Interface

  public void onOptions(RTSPClient client) {
    client.describe(url);
  }

  public void onDescribe(RTSPClient client, SDP sdp) {
    SDP.Stream stream = sdp.getFirstVideoStream();
    if (stream == null) {
      JFLog.log(log, "Error:CameraWorker:onDescribe():SDP does not contain video stream");
      return;
    }
    if (stream.framerate > 0) {
      fps = stream.framerate;
      if (isEncoder) {
        camera.fps = fps;
      }
    }
    //IP/port in SDP is all zeros
    stream.setIP(client.getRemoteIP());
    stream.setPort(-1);
    int av_codec_id = -1;
    if (stream.hasCodec(RTP.CODEC_H264)) {
      h264 = new RTPH264();
      h264.setLog(log);
      if (isDecoder) {
        packets_decode = new PacketBuffer(CodecType.H264);
        packets_decode.setLog(log);
      }
      if (isEncoder) {
        packets_encode = new PacketBuffer(CodecType.H264);
        packets_encode.setLog(log);
      }
      if (isEncoder) {
        camera.codec = stream.getCodec(RTP.CODEC_H264);
      }
      av_codec_id = MediaCoder.AV_CODEC_ID_H264;
    }
    if (stream.hasCodec(RTP.CODEC_H265)) {
      h265 = new RTPH265();
      h265.setLog(log);
      if (isDecoder) {
        packets_decode = new PacketBuffer(CodecType.H265);
        packets_decode.setLog(log);
      }
      if (isEncoder) {
        packets_encode = new PacketBuffer(CodecType.H265);
        packets_encode.setLog(log);
      }
      if (isEncoder) {
        camera.codec = stream.getCodec(RTP.CODEC_H265);
      }
      av_codec_id = MediaCoder.AV_CODEC_ID_H265;
    }
    if (isDecoder) {
      if (decoder != null) {
        decoder.stop();
        decoder = null;
      }
      decoder = new MediaVideoDecoder();
      if (!decoder.start(av_codec_id, decoded_x, decoded_y)) {
        JFLog.log(log, "Error:MediaVideoDecoder.start() failed");
        return;
      }
    }
    rtp = new RTP();
    rtp.setLog(log);
    if (!rtp.init(this)) {
      JFLog.log(log, "RTP init failed");
      rtp = null;
      return;
    }
    rtp.start();
    channel = rtp.createChannel(stream);
    channel.start();
    client.setup(url, rtp.getlocalrtpport(), stream.control);
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

  public void onGetParameter(RTSPClient client, String[] params) {
  }

  //RTP Interface

  public void rtpSamples(RTPChannel rtp) {
  }

  public void rtpDigit(RTPChannel rtp, char key) {
  }

  public void rtpPacket(RTPChannel rtp, int codec, byte[] buf, int offset, int length) {
    switch (codec) {
      case CodecType.H264: rtpCodec(rtp, buf, offset, length); break;
      case CodecType.H265: rtpCodec(rtp, buf, offset, length); break;
    }
  }

  public void rtpCodec(RTPChannel rtp, byte[] buf, int offset, int length) {
    try {
      if (isEncoder) {
        camera.sendPacket(buf, offset, length, log);
      }
      now = lastPacket = System.currentTimeMillis();
      if (h264 != null) {
        h264.decode(buf, 0, length, this);
      }
      if (h265 != null) {
        h265.decode(buf, 0, length, this);
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public void onPacket(Packet packet) {
    try {
      if (h264 != null) {
        byte type = h264.get_nal_type(packet.data, 4);
        if (!h264.canDecodePacket(type)) return;
      }
      if (h265 != null) {
        byte type = h265.get_nal_type(packet.data, 4);
        if (!h265.canDecodePacket(type)) return;
      }
      if (isEncoder) {
        onPacketEncoder(packet);
      }
      if (isDecoder) {
        onPacketDecoder(packet);
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void onPacketEncoder(Packet packet) {
    if (debug_encoder) JFLog.log(log, "onPacketViewer");
    try {
      packets_encode.add(packet);
      packets_encode.cleanPackets(true);
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void onPacketDecoder(Packet packet) {
    if (debug_decoder) JFLog.log(log, "onPacketDecoder");
    try {
      packets_decode.add(packet);
      packets_decode.cleanPackets(true);
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public void rtpInactive(RTPChannel rtp) {
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("CameraWorker:{");
    sb.append(client);
    sb.append(",");
    sb.append(rtp);
    sb.append("}");
    return sb.toString();
  }
}
