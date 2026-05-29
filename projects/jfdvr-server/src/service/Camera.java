package service;

/** Camera
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.io.*;
import javaforce.voip.*;

public class Camera implements Serializable, RTPInterface {
  public static final long serialVersionUID = 1;

  public String name;
  public String url;  //high res stream (viewing)
  public String url_low;  //low res stream (decoding, motion)
  public boolean enabled;
  public boolean record_motion;
  public int record_motion_threshold;
  public int max_folder_size;  //in GBs

  //picture mode data
  public String controller;
  public String tag_trigger;
  public String tag_value;
  public boolean pos_edge;

  public static boolean debug = false;
  private transient int log;
  public transient boolean keep;  //keep current recording : this value is shared between encoder and decoder threads

  public Camera() {
    name = "";
    url = "";
    url_low = "";
    record_motion = true;
    record_motion_threshold = 20;
    max_folder_size = 100;
    controller = "";
    tag_trigger = "";
    tag_value = "";
    pos_edge = true;
    enabled = true;
    init();
  }

  /** init transient fields. */
  public void init() {
    viewersLock = new Object();
    viewers_live = new ArrayList<>();
    viewers_file = new ArrayList<>();
  }

  public transient volatile float motion_value;
  public transient volatile byte[] preview;  //png image
  public transient volatile boolean viewing;
  public transient volatile boolean update_preview;
  public transient volatile Object viewersLock;
  public transient volatile ArrayList<RTSPSession> viewers_live;
  public transient volatile ArrayList<RTSPSession> viewers_file;

  public transient Codec codec;
  public transient float fps = -1;  //viewer fps (not decoder)

  public void setLog(int id) {
    log = id;
  }

  public void add_viewer(RTSPSession sess) {
    synchronized (viewersLock) {
      if (viewers_live.contains(sess)) {
        JFLog.log("DVR:Camera:RTSPSession:add_viewer:already exists:" + sess);
      } else {
        viewers_live.add(sess);
      }
      sess.res_user = this;
    }
  }

  public void add_viewer_file(RTSPServer server, RTSPSession sess, String ts_start, String ts_end) {
    synchronized (viewersLock) {
      if (viewers_file.contains(sess)) {
        JFLog.log("DVR:Camera:RTSPSession:add_viewer_recording:already exists:" + sess);
      } else {
        viewers_file.add(sess);
      }
      MediaServer media = new MediaServer(this, server, sess, ts_start, ts_end);
      media.start();
      sess.res_user = media;
    }
  }

  public void remove_viewer(RTSPSession sess) {
    synchronized (viewersLock) {
      if (viewers_live.contains(sess)) {
        viewers_live.remove(sess);
      } else if (viewers_file.contains(sess)) {
        viewers_file.remove(sess);
        MediaServer media = (MediaServer)sess.res_user;
        if (media != null) {
          media.stop();
        }
      } else {
        JFLog.log("DVR:Camera:RTSPSession:remove_viewer:not found:" + sess);
      }
      sess.res_user = null;
    }
  }

  public String[] get_sdp(RTSPSession sess) {
    SDP sdp = new SDP();
    SDP.Stream stream = sdp.addStream(SDP.Type.video);
    if (codec != null) {
      stream.framerate = fps;
      stream.addCodec(codec);
    } else {
      //create generic codec type (useful for playing recordings when camera is offline)
      stream.framerate = 10;
      stream.addCodec(new Codec("H264", 96));
    }
    stream.setIP(DVRService.rtspServer.resolve(sess.remotehost));
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
    if (debug) JFLog.log("Camera.get_sdp()remotehost=" + sess.remotehost);
    sess.channel.stream.setIP(sess.remotehost);
    return sdp.build(sess.localhost);
  }

  public void sendPacket(byte[] buf, int offset, int length) {
    synchronized (viewersLock) {
      for(RTSPSession sess : viewers_live) {
        if (debug) JFLog.log(log, "Camera.sendPacket()" + sess.remotehost + ":" + sess.channel.stream.port);
        try {
          sess.channel.writeRTP(buf, offset, length);
        } catch (Exception e) {
          JFLog.log(log, e);
        }
      }
    }
  }

  //RTPInterface
  public void rtpPacket(RTPChannel rtpc, int codecType, byte[] data, int offset, int length) {
  }

  public void rtpSamples(RTPChannel rtpc) {}

  public void rtpDigit(RTPChannel rtpc, char c) {}

  public void rtpInactive(RTPChannel rtpc) {}

  public String toString() {
    return "Camera:viewers=" + viewers_live.size() + ":" + name;
  }
}
