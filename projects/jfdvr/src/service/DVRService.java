package service;

/** DVR Service
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.media.*;
import javaforce.voip.*;

public class DVRService extends Thread implements RTSPServerInterface {
  public static DVRService dvrService;
  public static ConfigService configService;
  public static RTSPServer rtspServer;

  public final static boolean debug = false;
  public static void serviceStart(String args[]) {
    if (dvrService != null) return;
    dvrService = new DVRService();
    dvrService.start();
  }

  public static void serviceStop() {
    dvrService.cancel();
  }

  public static void main(String args[]) {
    serviceStart(args);  //for debugging
  }

  public void run() {
    //setup native
    WinNative.load();
    //setup codecs
    MediaCoder.init();
    //init Paths
    Paths.init();
    //load current config
    Config.load();
    //start config service
    JFLog.log("APPDATA=" + System.getenv("APPDATA"));
    configService = new ConfigService();
    configService.start();
    //enable firewall exception
    setupFirewall();
    //start RTSP server
    rtspServer = new RTSPServer();
    rtspServer.init(554, this, TransportType.TCP);
    //start recording processes
    Config config = Config.current;
    for(int a=0;a<config.cameras.length;a++) {
      if (config.cameras[a].enabled) {
        startCamera(config.cameras[a]);
      }
    }
  }

  public ArrayList<CameraWorker> list = new ArrayList<CameraWorker>();

  public void cancel() {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      list.get(a).cancel();
    }
  }

  public void startCamera(Camera camera) {
    boolean same = camera.url.equals(camera.url_low);
    if (camera.url.length() > 0) {
      startCamera(camera, camera.url, true, same || camera.url_low.length() == 0);
    }
    if (camera.url_low.length() > 0 && !same) {
      startCamera(camera, camera.url_low, camera.url.length() == 0, true);
    }
  }

  public void startCamera(Camera camera, String url, boolean viewer, boolean record) {
    System.out.println("Start Camera:" + camera.name);
    CameraWorker instance = null;
    switch (url.substring(0,4)) {
      case "rtsp": instance = new CameraWorkerVideo(camera, url, viewer, record); break;
      case "http": instance = new CameraWorkerPictures(camera, url, viewer, record); break;
    }
    if (instance == null) {
      JFLog.log("Error:invalid camera:" + camera.name);
      return;
    }
    instance.start();
    list.add(instance);
  }

  public void stopCamera(Camera camera) {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      if (list.get(a).getCamera() == camera) {
        try {list.get(a).cancel();} catch (Exception e) {JFLog.log(e);}
        list.remove(a);
      }
    }
  }

  public void reloadCamera(Camera camera) {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      if (list.get(a).getCamera() == camera) {
        try {list.get(a).reloadConfig();} catch (Exception e) {JFLog.log(e);}
        return;
      }
    }
    startCamera(camera);
  }

  private void setupFirewall() {
    RTP.setPortRange(5000, 10000);
    try {
      File firewall_setup = new File(Paths.dataPath + "/firewall.setup");
      if (firewall_setup.exists()) return;
      firewall_setup.createNewFile();
      Runtime.getRuntime().exec(new String[] {"netsh", "advfirewall", "firewall", "add", "rule", "name=\"jfDVR_RTP_IN\"", "dir=in", "protocol=udp", "localport=5000-10000", "action=allow"});
      Runtime.getRuntime().exec(new String[] {"netsh", "advfirewall", "firewall", "add", "rule", "name=\"jfDVR_RTP_OUT\"", "dir=out", "protocol=udp", "localport=5000-10000", "action=allow"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //RTSPServerInterface

  //rtsp://user:pass@host:port/camera/name
  //rtsp://user:pass@host:port/group/name

  public String getPassword(String user) {
    return "password";
  }

  public void onOptions(RTSPServer server, RTSPSession sess) {
    try {
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onDescribe(RTSPServer server, RTSPSession sess) {
    try {
      URL url = new URI(sess.uri).toURL();
      String path = url.getPath();  // / type / name
      String[] type_name = path.split("/");
      String type = type_name[1];
      String name = type_name[2];
      switch (type) {
        case "camera": sess.sdp = camera_get_sdp(name, sess); break;
        case "group": throw new Exception("CAN-NOT-PLAY-GROUP");
        default: throw new Exception("BAD-URL");
      }
      server.reply(sess, 200, "OK");
      sess.sdp = null;
    } catch (Exception e) {
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onSetup(RTSPServer server, RTSPSession sess) {
    try {
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onPlay(RTSPServer server, RTSPSession sess) {
    try {
      URL url = new URI(sess.uri).toURL();
      String path = url.getPath();  // / type / name
      String[] type_name = path.split("/");
      String type = type_name[1];
      String name = type_name[2];
      switch (type) {
        case "camera": camera_add_viewer(name, sess); break;
        case "group": throw new Exception("CAN-NOT-PLAY-GROUP");
        default: throw new Exception("BAD-URL");
      }
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onTeardown(RTSPServer server, RTSPSession sess) {
    try {
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onGetParameter(RTSPServer server, RTSPSession sess, String[] params) {
    try {
      String action = HTTP.getParameter(params, "action");
      JFLog.log("onGetParameter:uri=" + sess.uri + ":action=" + action);
      if (action.equals("query")) {
        URL url = new URI(sess.uri).toURL();
        String path = url.getPath();  // / type / name
        String[] type_name = path.split("/");
        String type = type_name[1];
        String name = type_name[2];
        JFLog.log("query:" + type + "/" + name);
        sess.params = null;
        switch (type) {
          case "list": sess.params = get_list_all(name); break;
          case "camera": sess.params = new String[] {"type: camera"}; break;
          case "group": sess.params = get_list_group_cameras(name); break;
          default: throw new Exception("BAD URL");
        }
        server.reply(sess, 200, "OK");
        sess.params = null;
      } else {
        sess.params = new String[] {"type: keep-alive"};
        server.reply(sess, 200, "OK");
        sess.params = null;
      }
    } catch (Exception e) {
      JFLog.log(e);
      server.reply(sess, 501, "ERROR");
    }
  }

  private String[] camera_get_sdp(String name, RTSPSession sess) {
    Camera camera = Config.current.getCamera(name);
    if (camera == null) return null;
    return camera.get_sdp(sess);
  }

  private void camera_add_viewer(String name, RTSPSession sess) {
    Camera camera = Config.current.getCamera(name);
    if (camera == null) return;
    camera.add_viewer(sess);
  }

  private String[] get_list_all(String type) {
    JFLog.log("query:list:all");
    StringBuilder camlist = new StringBuilder();
    StringBuilder grplist = new StringBuilder();
    camlist.append("cameras: ");
    grplist.append("groups: ");
    if (type.equals("camera") || type.equals("all")) {
      int cnt = 0;
      for(Camera camera : Config.current.cameras) {
        if (cnt > 0) camlist.append(",");
        camlist.append(camera.name);
        cnt++;
      }
    }
    if (type.equals("group") || type.equals("all")) {
      int cnt = 0;
      for(Group group : Config.current.groups) {
        if (cnt > 0) camlist.append(",");
        grplist.append(group.name);
        cnt++;
      }
    }
    return new String[] {camlist.toString(), grplist.toString()};
  }

  private String[] get_list_group_cameras(String name) {
    JFLog.log("query:group:" + name);
    Group group = Config.current.getGroup(name);
    if (group == null) {
      JFLog.log("group not found:" + name);
      return null;
    }
    String camlist = group.getCameraList();
    JFLog.log("group:camlist=" + camlist);
    return new String[] {camlist};
  }
}
