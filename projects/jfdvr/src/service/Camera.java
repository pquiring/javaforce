package service;

/**
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.io.*;
import javaforce.voip.*;

public class Camera extends SerialObject implements Serializable, RTPInterface {
  public static final long serialVersionUID = 1;
  public String name;
  public String url;  //high res stream (viewing)
  public String url_low;  //low res stream (decoding, motion)
  public boolean enabled;
  public boolean record_motion;
  public int record_motion_threshold;
  public int record_motion_after;
  public int max_file_size;  //in MBs
  public int max_folder_size;  //in GBs

  //picture mode data
  public String controller;
  public String tag_trigger;
  public String tag_value;
  public boolean pos_edge;

  public static boolean debug = false;
  private int log;

  public Camera() {
    name = "";
    url = "";
    url_low = "";
    record_motion = true;
    record_motion_threshold = 20;
    record_motion_after = 5;
    max_file_size = 1024;
    max_folder_size = 100;
    controller = "";
    tag_trigger = "";
    tag_value = "";
    pos_edge = true;
    enabled = true;
  }

  public transient volatile float motion_value;
  public transient volatile byte[] preview;  //png image
  public transient volatile boolean viewing;
  public transient volatile boolean update_preview;
  public transient volatile Object viewersLock = new Object();
  public transient volatile ArrayList<RTSPSession> viewers = new ArrayList<>();

  public Codec codec;
  public float fps = -1;  //viewer fps (not decoder)

  public void setLog(int id) {
    log = id;
  }

  public void add_viewer(RTSPSession sess) {
    synchronized (viewersLock) {
      if (viewers.contains(sess)) {
        JFLog.log("DVR:Camera:RTSPSession:add_viewer:already exists:" + sess);
      } else {
        viewers.add(sess);
      }
      sess.res_user = this;
    }
  }

  public void remove_viewer(RTSPSession sess) {
    synchronized (viewersLock) {
      if (viewers.contains(sess)) {
        viewers.remove(sess);
      } else {
        JFLog.log("DVR:Camera:RTSPSession:remove_viewer:not found:" + sess);
      }
      sess.res_user = null;
    }
  }

  public String[] get_sdp(RTSPSession sess) {
    if (codec == null) {
      JFLog.log(log, "Error:Camera not ready");
      return null;
    }
    SDP sdp = new SDP();
    SDP.Stream stream = sdp.addStream(SDP.Type.video);
    stream.framerate = fps;
    stream.addCodec(codec);
    if (sess.rtp != null) {
      sess.rtp.uninit();
      sess.rtp = null;
    }
    sess.rtp = new RTP();
    sess.rtp.init(this);
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
      for(RTSPSession sess : viewers) {
        if (debug) JFLog.log(log, "Camera.sendPacket()" + sess.remotehost + ":" + sess.channel.stream.port);
        try {
          sess.channel.writeRTP(buf, offset, length);
        } catch (Exception e) {
          JFLog.log(log, e);
        }
      }
    }
  }

  public static final short id_name = id_len + 1;
  public static final short id_url = id_len + 2;
  public static final short id_controller = id_len + 3;
  public static final short id_tag_trigger = id_len + 4;
  public static final short id_tag_value = id_len + 5;
  public static final short id_url_low = id_len + 6;

  public static final short id_record_motion = id_1 + 1;
  public static final short id_pos_edge = id_1 + 2;
  public static final short id_enabled = id_1 + 3;

  public static final short id_record_motion_threshold = id_4 + 1;
  public static final short id_record_motion_after = id_4 + 2;
  public static final short id_max_file_size = id_4 + 3;
  public static final short id_max_folder_size = id_4 + 4;

  public void readObject() throws Exception {
    do {
      short id = readShort();
      switch (id) {
        case id_name: name = readString(); break;
        case id_url: url = readString(); break;
        case id_url_low: url_low = readString(); break;
        case id_record_motion: record_motion = readBoolean(); break;
        case id_record_motion_threshold: record_motion_threshold = readInt(); break;
        case id_record_motion_after: record_motion_after = readInt(); break;
        case id_max_file_size: max_file_size = readInt(); break;
        case id_max_folder_size: max_folder_size = readInt(); break;
        case id_controller: controller = readString(); break;
        case id_tag_trigger: tag_trigger = readString(); break;
        case id_tag_value: tag_value = readString(); break;
        case id_pos_edge: pos_edge = readBoolean(); break;
        case id_enabled: enabled = readBoolean(); break;
        case id_end: {
          if (name == null) name = "camera";
          return;
        }
        default: skipChunk(id); break;
      }
    } while (true);
  }

  public void writeObject() throws Exception {
    writeShort(id_name);
    writeString(name);
    writeShort(id_url);
    writeString(url);
    writeShort(id_url_low);
    writeString(url_low);
    writeShort(id_record_motion);
    writeBoolean(record_motion);
    writeShort(id_record_motion_threshold);
    writeInt(record_motion_threshold);
    writeShort(id_record_motion_after);
    writeInt(record_motion_after);
    writeShort(id_max_file_size);
    writeInt(max_file_size);
    writeShort(id_max_folder_size);
    writeInt(max_folder_size);
    if (controller != null) {
      writeShort(id_controller);
      writeString(controller);
      if (tag_trigger != null) {
        writeShort(id_tag_trigger);
        writeString(tag_trigger);
      }
      if (tag_value != null) {
        writeShort(id_tag_value);
        writeString(tag_value);
      }
      writeShort(id_pos_edge);
      writeBoolean(pos_edge);
    }
    writeShort(id_enabled);
    writeBoolean(enabled);
    writeShort(id_end);
  }

  //RTPInterface
  public void rtpPacket(RTPChannel rtpc, int i, byte[] bytes, int i1, int i2) {}

  public void rtpSamples(RTPChannel rtpc) {}

  public void rtpDigit(RTPChannel rtpc, char c) {}

  public void rtpInactive(RTPChannel rtpc) {}

  public String toString() {
    return "Camera:viewers=" + viewers.size() + ":" + name;
  }
}
