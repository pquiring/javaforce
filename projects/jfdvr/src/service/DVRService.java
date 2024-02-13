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
import javaforce.webui.*;

public class DVRService extends Thread implements RTSPServerInterface {
  public static DVRService dvrService;
  public static ConfigService configService;
  public static RTSPServer rtspServer;
  public static DebugState debugState;

  public final static boolean debug = true;
  public Timer timer;

  public static void serviceStart(String args[]) {
    if (dvrService != null) return;
    dvrService = new DVRService();
    dvrService.start();
  }

  public static void serviceStop() {
    if (dvrService == null) return;
    dvrService.cancel();
    dvrService = null;
  }

  public void run() {
    setName("DVRService");
    //setup native
    WinNative.load();
    //setup codecs
    MediaCoder.init();
    //init Paths
    Paths.init();
    //load current config
    Config.load();
    //create debug state
    if (debug) {
      //enable debug messages in sub-systems
      RTSP.debug = true;
      TransportTCPServer.debug = true;
      WebUIServer.debug = true;
      debugState = new DebugState(Paths.logsPath + "/debug.log", new Runnable() {public void run() {
        if (rtspServer == null) return;
        RTSPSession[] sesses = rtspServer.getSessions();
        debugState.write("*** Server Sessions ************");
        for(RTSPSession sess : sesses) {
          debugState.write(sess.toString());
        }
        Object[] workers = list.toArray();
        debugState.write("*** Camera Sessions ************");
        for(Object worker : workers) {
          debugState.write(worker.toString());
        }
      }});
      debugState.start();
    }
    //start config service
    configService = new ConfigService();
    configService.start();
    //enable firewall exception
    setupFirewall();
    //start keep alive timer
    timer = new Timer();
    timer.schedule(new TimerTask() {public void run() {checkKeepAlive();}}, 60 * 1000, 60 * 1000);
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
      try {
        list.get(a).cancel();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (rtspServer != null) {
      try {
        rtspServer.uninit();
      } catch (Exception e) {
        e.printStackTrace();
      }
      rtspServer = null;
    }
    if (configService != null) {
      try {
        configService.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
      configService = null;
    }
    if (debugState != null) {
      debugState.cancel();
      debugState = null;
    }
  }

  public void startCamera(Camera camera) {
    boolean same = camera.url.equals(camera.url_low);
    CameraWorker viewer = null;
    if (camera.url.length() > 0) {
      viewer = startCamera(camera, camera.url, true, same || camera.url_low.length() == 0, null);
    }
    if (camera.url_low.length() > 0 && !same) {
      startCamera(camera, camera.url_low, camera.url.length() == 0, true, viewer);
    }
  }

  public CameraWorker startCamera(Camera camera, String url, boolean isViewer, boolean isDecoding, CameraWorker viewer) {
    System.out.println("Start Camera:" + camera.name);
    CameraWorker instance = null;
    switch (url.substring(0,4)) {
      case "rtsp": instance = new CameraWorkerVideo(camera, url, isViewer, isDecoding, viewer); break;
      case "http": instance = new CameraWorkerPictures(camera, url, isViewer, isDecoding, viewer); break;
    }
    if (instance == null) {
      JFLog.log("Error:invalid camera:" + camera.name);
      return null;
    }
    instance.start();
    list.add(instance);
    return instance;
  }

  public void stopCamera(Camera camera) {
    int count = list.size();
    for(int idx=0;idx<count;) {
      if (list.get(idx).getCamera() == camera) {
        try {list.get(idx).cancel();} catch (Exception e) {JFLog.log(e);}
        list.remove(idx);
        count--;
      } else {
        idx++;
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
    RTP.setPortRange(30000, 40000);
    if (!JF.isWindows()) return;
    //setup windows firewall
    try {
      File firewall_setup = new File(Paths.dataPath + "/firewall.setup");
      if (firewall_setup.exists()) return;
      firewall_setup.createNewFile();
      Runtime.getRuntime().exec(new String[] {"netsh", "advfirewall", "firewall", "add", "rule", "name=\"jfDVR_RTP_IN\"", "dir=in", "protocol=udp", "localport=30000-50000", "action=allow"});
      Runtime.getRuntime().exec(new String[] {"netsh", "advfirewall", "firewall", "add", "rule", "name=\"jfDVR_RTP_OUT\"", "dir=out", "protocol=udp", "localport=30000-50000", "action=allow"});
      Runtime.getRuntime().exec(new String[] {"netsh", "advfirewall", "firewall", "add", "rule", "name=\"jfDVR_RTSP_IN\"", "dir=in", "protocol=tcp", "localport=554", "action=allow"});
      Runtime.getRuntime().exec(new String[] {"netsh", "advfirewall", "firewall", "add", "rule", "name=\"jfDVR_RTSP_OUT\"", "dir=out", "protocol=tcp", "localport=554", "action=allow"});
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
    sess.ts = System.currentTimeMillis();
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

  public void onConnect(RTSPServer rtsp, RTSPSession sess) {
    JFLog.log("onConnect:" + sess);
    sess.ts = System.currentTimeMillis();
  }

  public void onDisconnect(RTSPServer rtsp, RTSPSession sess) {
    JFLog.log("onDisconnect:" + sess);
    sess.ts = 0;
    checkKeepAlive();
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
    Group group = Config.current.getGroup(name);
    if (group == null) {
      JFLog.log("group not found:" + name);
      return null;
    }
    String camlist = group.getCameraList();
    return new String[] {camlist};
  }

  private void checkKeepAlive() {
    long cut = System.currentTimeMillis() - 60 * 1000;
    synchronized (Config.current.camerasLock) {
      for(Camera cam : Config.current.cameras) {
        synchronized (cam.viewersLock) {
          int count = cam.viewers.size();
          for(int idx=0;idx<count;idx++) {
            RTSPSession sess = cam.viewers.get(idx);
            if (sess.ts < cut) {
              JFLog.log("DVR:Session expired:" + sess);
              cam.remove_viewer(sess);
              count--;
            } else {
              idx++;
            }
          }
        }
      }
    }
  }
}
