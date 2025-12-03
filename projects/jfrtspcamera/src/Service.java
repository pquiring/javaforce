/** Service
 *
 * @author pquiring
 *
 * Created : Nov 30, 2025
 */

import java.util.*;
import java.net.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;
import javaforce.voip.*;

public class Service extends Thread implements RTSPServerInterface {

  public static String version = "0.1";
  public static boolean debug = false;

  private ArrayList<String> cameraDevices = new ArrayList<>();
  private ArrayList<ArrayList<String>> videoModes = new ArrayList<>();
  private ArrayList<String> audioDevices = new ArrayList<>();
  private Camera camera;
  private AudioInput mic;
  private boolean active = false;
  private RTSPServer rtsp;
  private ArrayList<CameraWorker> workers = new ArrayList<>();
  private static Service service;
  private int log;

  public static void serviceStart(String[] args) {
    MediaCoder.init();
    if (service != null) return;
    for(String arg : args) {
      switch (arg) {
      }
    }
    service = new Service();
    service.start();
  }

  public static void serviceStop() {
    if (service == null) return;
    service.cancel();
    service = null;
  }

  public void run() {
    Paths.init();
    Config.load();
    listCameras();
    listAudioDevices();
    rtsp = new RTSPServer();
    rtsp.init(554, this, TransportType.TCP);
    active = true;
    startCamera(Config.current.idx);
    while (active) {
      JF.sleep(500);
    }
    rtsp.uninit();
  }

  public void cancel() {
    active = false;
  }

  public void listCameras() {
    camera = new Camera();
    camera.init();
    String list[] = camera.listDevices();
    camera.uninit();
    camera = null;
    cameraDevices.clear();
    if (list == null) {
      JFAWT.showError("Error", "No camera detected");
      return;
    }
    for(String device : list) {
      cameraDevices.add(device);
    }
    if (cameraDevices.size() > 0) {
      listModes(0);
    }
  }

  public void listModes(int idx) {
    camera = new Camera();
    camera.init();
    camera.listDevices();
    String list[] = camera.listModes(idx);
    camera.uninit();
    camera = null;
    if (list == null) {
      JFAWT.showError("Error", "No camera modes");
      return;
    }
    ArrayList<String> vidModes = new ArrayList<String>();
    videoModes.add(vidModes);
    for(String mode : list) {
      vidModes.add(mode);
    }
  }

  public void listAudioDevices() {
    mic = new AudioInput();
    String list[] = mic.listDevices();
    audioDevices.clear();
    for(String item : list) {
      audioDevices.add(item);
    }
  }

  private void startCamera(int idx) {
    CameraWorker worker = new CameraWorker(idx, Config.current.fps);
    worker.start();
    workers.add(worker);
  }

  private String[] camera_get_sdp(String name, RTSPSession sess) {
    for(CameraWorker worker : workers) {
      if (worker.name.equals(name)) {
        return worker.get_sdp(sess);
      }
    }
    JFLog.log("Error:camera_get_sdp():Camera not found:" + name);
    return null;
  }

  private void camera_add_viewer(String name, RTSPSession sess) {
    for(CameraWorker worker : workers) {
      if (worker.name.equals(name)) {
        worker.add_viewer(sess);
        return;
      }
    }
    JFLog.log("Error:camera_add_viewer():Camera not found:" + name);
  }

  private void camera_remove_viewer(String name, RTSPSession sess) {
    for(CameraWorker worker : workers) {
      if (worker.name.equals(name)) {
        worker.remove_viewer(sess);
        return;
      }
    }
    JFLog.log("Error:camera_remove_viewer():Camera not found:" + name);
  }

  public class CameraWorker extends Thread implements RTPInterface, PacketReceiver {
    private Codec codec;
    private CodecInfo info;
    private int idx;
    private int fps;
    private int delay;
    private Camera camera;
    private MediaVideoEncoder encoder;
    private RTPH264 h264;
    private RTPH265 h265;
    private RTPVideoCoder rtpCoder;
    private ArrayList<RTSPSession> viewers = new ArrayList<>();
    public String name;  //camera01, etc.

    public CameraWorker(int idx, int fps) {
      this.idx = idx;
      this.fps = fps;
      delay = 1000 / fps;
      name = String.format("camera%02d", (idx + 1));  //camera01, camera02, etc.
    }

    public String[] get_sdp(RTSPSession sess) {
      SDP sdp = new SDP();
      SDP.Stream stream = sdp.addStream(SDP.Type.video);
      if (codec != null) {
        stream.framerate = fps;
        stream.addCodec(codec);
      } else {
        //create generic codec type
        stream.framerate = fps;
        stream.addCodec(new Codec("H264", 96));
      }
      stream.setIP(service.rtsp.resolve(sess.remotehost));
      stream.setPort(-1);
      if (sess.rtp != null) {
        sess.rtp.uninit();
        sess.rtp = null;
      }
      sess.rtp = new RTP();
      sess.rtp.init(this, TransportType.UDP);
      sess.rtp.start();
      if (sess.channel != null) {
        sess.channel = null;
      }
      sess.channel = sess.rtp.createChannel(sdp.getFirstVideoStream());
      sess.channel.start();
      if (debug) JFLog.log("Camera.get_sdp():remotehost=" + sess.remotehost);
      sess.channel.stream.setIP(sess.remotehost);
      return sdp.build(sess.localhost);
    }

    public void add_viewer(RTSPSession sess) {
      try {
        viewers.add(sess);
      } catch (Exception e) {
        JFLog.log(log, e);
      }
    }

    public void remove_viewer(RTSPSession sess) {
      try {
        viewers.remove(sess);
      } catch (Exception e) {
        JFLog.log(log, e);
      }
    }

    public void run() {
      camera = new Camera();
      camera.init();
      camera.listDevices();
      camera.start(idx, -1, -1);  //use default width, height
      info = new CodecInfo();

      info.fps = Config.current.fps;
      info.video_bit_rate = Config.current.bit_rate;
      info.width = camera.getWidth();
      info.height = camera.getHeight();
      info.video_stream = 0;  //stream #0
      info.keyFrameInterval = (int)info.fps;
      switch (Config.current.codec) {
        default:
          JFLog.log("Error:Unknown codec:" + Config.current.codec + ":using H264");
          //no break
        case 1:
          info.video_codec = MediaCoder.AV_CODEC_ID_H264;
          break;
        case 2:
          info.video_codec = MediaCoder.AV_CODEC_ID_H265;
          break;
      }
      JFLog.log("Stream=" + info);

      encoder = new MediaVideoEncoder();
      if (!encoder.start(info)) {
        JFLog.log("Encoder.start() failed!");
      }

      switch (Config.current.codec) {
        default:  //no break
        case Config.H264:
          h264 = new RTPH264();
          rtpCoder = h264;
          break;
        case Config.H265:
          h265 = new RTPH265();
          rtpCoder = h265;
          break;
      }
      rtpCoder.setid(96);

      try {
        while (active) {
          JF.sleep(delay);
          int[] px = camera.getFrameFlip();
          if (px == null) {
            if (debug) JFLog.log("Error:no frame from camera");
            continue;
          }
          int x = camera.getWidth();
          int y = camera.getHeight();
          //encode frame -> MPEG packet
          Packet packet = encoder.encode(px, 0, px.length);
          if (packet == null) {
            if (debug) JFLog.log("Error:no packet from encoder");
            continue;
          }
          if (packet.data == null) {
            if (debug) JFLog.log("Error:empty packet from encoder");
            continue;
          }
          //packet = raw MPEG packet
          //encode MPEG packet -> RTP packet(s)
          rtpCoder.encode(packet.data, 0, packet.data.length, x, y, this);
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      workers.remove(this);
    }

    //PacketReceiver interface
    public void onPacket(Packet packet) {
      //packet = RTP packet (fragment of MPEG packet)
      //send to all viewers
      ArrayList<RTSPSession> inactive = new ArrayList<>();
      try {
        for(RTSPSession viewer : viewers) {
          if (!viewer.channel.isActive()) {
            inactive.add(viewer);
            continue;
          }
          viewer.channel.writeRTP(packet.data, 0, packet.length);
        }
        for(RTSPSession viewer : inactive) {
          remove_viewer(viewer);
        }
      } catch (Exception e) {
        JFLog.log(log, e);
      }
    }

    //RTPInterface interface
    public void rtpPacket(RTPChannel rtpc, int codecType, byte[] data, int offset, int length) {
    }

    public void rtpSamples(RTPChannel rtpc) {
    }

    public void rtpDigit(RTPChannel rtpc, char c) {
    }

    public void rtpInactive(RTPChannel rtpc) {
    }

  }

  //RTSPServerInterface
  public String getPassword(String user) {
    JFLog.log("getPassword:" + user);
    return "password";
  }

  public void onOptions(RTSPServer server, RTSPSession sess) {
    try {
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onDescribe(RTSPServer server, RTSPSession sess) {
    try {
      URL url = new URI(sess.uri).toURL();
      String path = url.getPath().substring(1);  //remove leading /
      if (debug) JFLog.log(log, "onDescribe:" + path);
      String[] args = path.split("/");
      String type = args[0];
      String name = args[1];
      switch (type) {
        case "camera": sess.sdp = camera_get_sdp(name, sess); break;
        default: throw new Exception("BAD-URL");
      }
      server.reply(sess, 200, "OK");
      sess.sdp = null;
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onSetup(RTSPServer server, RTSPSession sess) {
    try {
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onPlay(RTSPServer server, RTSPSession sess) {
    try {
      URL url = new URI(sess.uri).toURL();
      String path = url.getPath().substring(1);  //rename leading /
      if (debug) JFLog.log(log, "onPlay:" + path);
      String[] args = path.split("/");
      String type = args[0];
      String name = args[1];
      switch (type) {
        case "camera":
          camera_add_viewer(name, sess);
          break;
        default: throw new Exception("BAD-URL");
      }
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onTeardown(RTSPServer server, RTSPSession sess) {
    try {
      URL url = new URI(sess.uri).toURL();
      String path = url.getPath().substring(1);  // / type / name
      if (debug) JFLog.log(log, "onTeardown:" + path);
      String[] args = path.split("/");
      String type = args[0];
      String name = args[1];
      switch (type) {
        case "camera": camera_remove_viewer(name, sess); break;
        default: throw new Exception("BAD-URL");
      }
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onGetParameter(RTSPServer server, RTSPSession sess, String[] params) {
    sess.ts = System.currentTimeMillis();
    try {
      String action = HTTP.getParameter(params, "action");
      if (action == null) action = "keep-alive";
      if (debug) JFLog.log(log, "onGetParameter:uri=" + sess.uri + ":action=" + action);
      switch (action) {
        case "query":
          URL url = new URI(sess.uri).toURL();
          String path = url.getPath();  // / type / name
          path = path.substring(1);  //remove leading /
          String[] args = path.split("/");
          String type = args[0];
          String name = args[1];
          if (debug) JFLog.log(log, "DVRService:query:" + type + "/" + name + ":" + sess.remotehost + ":" + sess.remoteport);
          sess.params = null;
          switch (type) {
//            case "list": sess.params = get_list_all(name); break;
            case "camera": sess.params = new String[] {"type: camera"}; break;
            default: throw new Exception("BAD URL");
          }
          server.reply(sess, 200, "OK");
          sess.params = null;
          break;
        case "keep-alive":
          if (debug) JFLog.log(log, "DVRService:ack keep-alive:" + sess.remotehost + ":" + sess.remoteport);
          sess.params = new String[] {"type: keep-alive"};
          server.reply(sess, 200, "OK");
          sess.params = null;
          break;
        default:
          throw new Exception("BAD REQUEST");
      }
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onSetParameter(RTSPServer server, RTSPSession sess, String[] params) {
    sess.ts = System.currentTimeMillis();
    try {
      String action = HTTP.getParameter(params, "Seek");
      if (action == null) throw new Exception("BAD REQUEST");
      switch (action) {
        case "seek":
          //TODO
          String ts = HTTP.getParameter(params, "ts");
          break;
      }
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onConnect(RTSPServer server, RTSPSession sess) {
    JFLog.log(log, "onConnect:" + sess);
    sess.ts = System.currentTimeMillis();
  }

  public void onDisconnect(RTSPServer server, RTSPSession sess) {
    JFLog.log(log, "onDisconnect:" + sess);
    sess.ts = 0;
    try {
      if (sess.res_user != null) {
        if (sess.res_user instanceof CameraWorker) {
          CameraWorker worker = (CameraWorker)sess.res_user;
          worker.remove_viewer(sess);
        }
      }
      if (sess.rtp != null) {
        sess.rtp.uninit();
        sess.rtp = null;
      }
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
    }
  }
}
