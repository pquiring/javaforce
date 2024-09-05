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

public class CameraWorkerVideo extends Thread implements RTSPClientInterface, RTPInterface, CameraWorker, PacketReceiver {
  public Camera camera;
  private String url;
  private String path;
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
  private Media encoder;
  private long folder_size;
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
  private int log;
  private boolean isEncoder;  //viewing, recording
  private boolean isDecoder;  //decoding, preview, motion detection
//  private CameraWorker encoderWorker;  //used by decoding to signal recording state to encoder
  private long minute;
  private boolean keep;

  private static class Recording {
    public File file;
    public long size;
    public long time;
  }

  private ArrayList<Recording> files = new ArrayList<Recording>();

  public CameraWorkerVideo(Camera cam, String cam_url, boolean is_Encoder, boolean is_Decoder) {
    this.url = cam_url;
    //NOTE:if camera only has one URL this class will be encoder and decoder
    this.isEncoder = is_Encoder;
    this.isDecoder = is_Decoder;
    this.camera = cam;
    if (isEncoder) {
      log = Config.nextLog();
      JFLog.append(log, Paths.logsPath + "/cam-" + camera.name + "-encode.log", true);
      JFLog.setRetention(log, 5);
      camera.setLog(log);
    } else {
      log = Config.nextLog();
      JFLog.append(log, Paths.logsPath + "/cam-" + camera.name + "-decode.log", true);
      JFLog.setRetention(log, 5);
    }
    JFLog.log(log, "CameraWorkerVideo:" + cam_url + ":encoder=" + is_Encoder + ":decoder=" + is_Decoder);
    JFLog.log(log, "Camera=" + camera.name);
    path = Paths.videoPath + "/" + cam.name;
    max_folder_size = cam.max_folder_size * 1024L * 1024L * 1024L;
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
        if (now - lastPacket > 5*1000) {
          JFLog.log(log, camera.name + " : Reconnecting");
          disconnect();
          if (!connect()) {
            JF.sleep(1000);
            continue;
          }
        } else if (now - lastKeepAlive > 25*1000) {
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
      int cnt = 0;
      while (packets_encode.haveCompleteFrame()) {
        boolean key_frame = packets_encode.isNextFrame_KeyFrame();
        Packet packet = packets_encode.getNextFrame();
        if (debug_encoder) JFLog.log(log, "encoder:add full packet");
        recordFrame(packet, key_frame);
        cnt++;
        if (cnt == 256) break;
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void recordFrame(Packet packet, boolean key_frame) {
    try {
      long now = System.currentTimeMillis();
      long current_minute = now / 60L;  //current minute
      if (current_minute != minute && key_frame) {
        minute = current_minute;
        if (!camera.record_motion) {
          keep = true;
        }
        JFLog.log(log, camera.name + " : max file size");
        if (encoder != null) {
          closeFile();
        }
        keep = !camera.record_motion;
      }
      if (encoder == null) {
        createFile();
      }
      encoder.writeFrame(0, packet.data, 0, packet.length, now, key_frame);
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
      int cnt = 0;
      while (packets_decode.haveCompleteFrame()) {
        boolean key_frame = packets_decode.isNextFrame_KeyFrame();
        Packet packet = packets_decode.getNextFrame();
        decoded_frame = decoder.decode(packet.data, packet.offset, packet.length);
        if (decoded_frame == null) {
          JFLog.log(log, camera.name + ":Error:newFrame == null:packet.length=" + packet.length);
          JFLog.log(log, "NALs=" + packets_decode.get_nal_list());
          packets_decode.reset();
          return;
        }
        if (last_frame == null) {
          last_frame = new int[decoded_xy];
        }
        if (camera.record_motion) {
          detectMotion(decoded_frame, key_frame);
        }
        cnt++;
        if (cnt == 256) break;
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public boolean connect() {
    //reset values
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
    if (debug) JFLog.log(log, camera.name + " : Connecting : encoder=" + isEncoder + ",decoder=" + isDecoder + ",remoteport=" + remoteport);
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
      closeFile();
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
    if (changed > camera.record_motion_threshold) {
      keep = true;
    }
  }

  private String filename;

  private void createFile() {
    try {
      long now = System.currentTimeMillis();
      long secs = now % (60 * 1000);
      long round = 0;
      if (secs < (30 * 1000)) {
        round = 15 * 1000;
      }
      filename = DVRService.getRecordingFilename(camera.name, now + round);
      JFLog.log(log, camera.name + " : createFile:" + filename);
      int codec = -1;
      if (h264 != null) codec = MediaCoder.AV_CODEC_ID_H264;
      else if (h265 != null) codec = MediaCoder.AV_CODEC_ID_H265;
      if (codec == -1) {
        JFLog.log("Error:createFile():Codec not known yet");
        return;
      }
      encoder = new Media();
      encoder.create(filename, new int[] {codec});
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void closeFile() {
    if (encoder == null) return;
    encoder.close();
    encoder = null;
    if (keep) {
      Recording rec = new Recording();
      rec.file = new File(filename);
      rec.size = rec.file.length();
      rec.time = rec.file.lastModified();
      files.add(rec);
    } else {
      new File(filename).delete();
    }
  }

  //CameraWorker interface

  public void cancel() {
    active = false;
  }

  public void reloadConfig() {
    JFLog.log(log, "Reloading config");
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
  }

  public Camera getCamera() {
    return camera;
  }

  public int getLog() {
    return log;
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
    if (stream.framerate > 0 && isEncoder) {
      camera.fps = stream.framerate;
    }
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
    if (!rtp.init(this, TransportType.UDP)) {
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

  public void onSetParameter(RTSPClient client, String[] params) {
  }

  //RTP Interface

  public void rtpSamples(RTPChannel rtp) {
  }

  public void rtpDigit(RTPChannel rtp, char key) {
  }

  /** Received RTP packet. */
  public void rtpPacket(RTPChannel rtp, int codec, byte[] buf, int offset, int length) {
    switch (codec) {
      case CodecType.H264: rtpCodec(rtp, buf, offset, length); break;
      case CodecType.H265: rtpCodec(rtp, buf, offset, length); break;
    }
  }

  /** Received RTP packet. */
  public void rtpCodec(RTPChannel rtp, byte[] buf, int offset, int length) {
    try {
      if (isEncoder) {
        camera.sendPacket(buf, offset, length);
      }
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

  /** Received H264/265 packet. */
  public void onPacket(Packet packet) {
    try {
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
    if (debug_encoder) JFLog.log(log, "onPacketEncoder");
    try {
      packets_encode.add(packet);
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  private void onPacketDecoder(Packet packet) {
    if (debug_decoder) JFLog.log(log, "onPacketDecoder");
    try {
      packets_decode.add(packet);
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
