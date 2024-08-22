package service;

/** DVR Service
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.webui.*;
import javaforce.voip.*;

public class DVRService extends Thread implements RTSPServerInterface {
  public static DVRService dvrService;
  public static ConfigService configService;
  public static WebServerRedir redirService;
  public static RTSPServer rtspServer;
  public static DebugState debugState;
  public static WorkerKeepAlive keepAlive;

  public static boolean active = true;

  public final static boolean debug = true;
  public final static boolean debug_sub_systems = false;

  /** The G1 GC is known to crash under heavy loads.
   * Invoke System.gc() every 30 seconds seems to avoid GC deadlocks.
   * Switching to the ZGC is another fix that seems to work.
   *
   * This flag when enabled will perform the gc() every 30 seconds.
   */
  public final static boolean fix_gc = false;

  private int log;

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
    //init Paths
    Paths.init();
    log = Config.nextLog();
    JFLog.append(JFLog.DEFAULT, Paths.logsPath + "/system.log", false);  //default log (0)
    JFLog.append(log, Paths.logsPath + "/service.log", false);
    JFLog.setRetention(log, 5);
    JFLog.log(log, "jfDVR/" + ConfigService.version + " starting...");
    JFLog.log(log, "pid=" + ProcessHandle.current().pid());
    //load current config
    Config.load();
    //create debug state
    if (debug) {
      if (debug_sub_systems) {
        TransportTCPServer.debug = true;
        RTSP.debug = true;
        WebUIServer.debug = true;
        CameraWorkerVideo.debug = true;
      }
      debugState = new DebugState(Paths.logsPath + "/debug.log", new Runnable() {public void run() {
        if (rtspServer == null) {
          debugState.write("rtspServer==null");
          return;
        }
        debugState.write("now=" + System.currentTimeMillis());
        try {
          RTSPSession[] sesses = rtspServer.getSessions();
          debugState.write("*** Server Sessions ************");
          for(RTSPSession sess : sesses) {
            debugState.write(sess.toString());
          }
        } catch (Exception e) {
          debugState.write(e.toString());
        }
        try {
          debugState.write("*** Transport Sessions ************");
          String[] clients = rtspServer.getTransportClients();
          for(String client : clients) {
            debugState.write(client);
          }
        } catch (Exception e) {
          debugState.write(e.toString());
        }
        try {
          debugState.write("*** Cameras ************");
          Config config = Config.current;
          for(int a=0;a<config.cameras.length;a++) {
            Camera cam = config.cameras[a];
            if (cam.enabled) {
              debugState.write(cam.toString());
            }
          }
        } catch (Exception e) {
          debugState.write(e.toString());
        }
        try {
          debugState.write("*** Camera Sessions ************");
          Object[] workers = list.toArray();
          for(Object worker : workers) {
            debugState.write(worker.toString());
          }
        } catch (Exception e) {
          debugState.write(e.toString());
        }
      }});
      debugState.start();
    }
    //start config service
    configService = new ConfigService();
    configService.start();
    //start redir service
    redirService = new WebServerRedir();
    redirService.start(80, 443);
    //enable firewall exception
    setupFirewall();
    //start keep alive thread
    keepAlive = new WorkerKeepAlive();
    keepAlive.start();
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
    active = false;
    keepAlive = null;
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
    if (redirService != null) {
      try {
        redirService.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
      redirService = null;
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
    JFLog.log(log, "Start Camera:" + camera.name);
    CameraWorker instance = null;
    switch (url.substring(0,4)) {
      case "rtsp": instance = new CameraWorkerVideo(camera, url, isViewer, isDecoding, viewer); break;
      case "http": instance = new CameraWorkerPictures(camera, url, isViewer, isDecoding, viewer); break;
    }
    if (instance == null) {
      JFLog.log(log, "Error:invalid camera:" + camera.name);
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
        try {list.get(idx).cancel();} catch (Exception e) {JFLog.log(log, e);}
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
        try {list.get(a).reloadConfig();} catch (Exception e) {JFLog.log(log, e);}
        return;
      }
    }
    startCamera(camera);
  }

  private void setupFirewall() {
    //RTSP-TCP : 554
    //RTP-UDP : server=30000-40000 client=40000-50000
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
      JFLog.log(log, e);
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
      if (debug) JFLog.log(log, e);
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
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onTeardown(RTSPServer server, RTSPSession sess) {
    try {
      URL url = new URI(sess.uri).toURL();
      String path = url.getPath();  // / type / name
      String[] type_name = path.split("/");
      String type = type_name[1];
      String name = type_name[2];
      switch (type) {
        case "camera": camera_remove_viewer(name, sess); break;
        case "group": throw new Exception("CAN-NOT-PLAY-GROUP");
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
      if (action.equals("query")) {
        URL url = new URI(sess.uri).toURL();
        String path = url.getPath();  // / type / name
        String[] type_name = path.split("/");
        String type = type_name[1];
        String name = type_name[2];
        if (debug) JFLog.log(log, "DVRService:query:" + type + "/" + name + ":" + sess.remotehost + ":" + sess.remoteport);
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
        if (debug) JFLog.log(log, "DVRService:ack keep-alive:" + sess.remotehost + ":" + sess.remoteport);
        sess.params = new String[] {"type: keep-alive"};
        server.reply(sess, 200, "OK");
        sess.params = null;
      }
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onSetParameter(RTSPServer server, RTSPSession sess, String[] params) {
    sess.ts = System.currentTimeMillis();
    try {
      String seek = HTTP.getParameter(params, "Seek");
      if (seek != null) {
        //TODO
      }
      server.reply(sess, 200, "OK");
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      server.reply(sess, 501, "ERROR");
    }
  }

  public void onConnect(RTSPServer rtsp, RTSPSession sess) {
    JFLog.log(log, "onConnect:" + sess);
    sess.ts = System.currentTimeMillis();
  }

  public void onDisconnect(RTSPServer rtsp, RTSPSession sess) {
    JFLog.log(log, "onDisconnect:" + sess);
    sess.ts = 0;
    try {
      if (sess.res_user != null) {
        Camera cam = (Camera)sess.res_user;
        cam.remove_viewer(sess);
      }
      if (sess.rtp != null) {
        sess.rtp.uninit();
        sess.rtp = null;
      }
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
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

  private void camera_remove_viewer(String name, RTSPSession sess) {
    Camera camera = Config.current.getCamera(name);
    if (camera == null) return;
    camera.remove_viewer(sess);
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
        if (cnt > 0) grplist.append(",");
        grplist.append(group.name);
        cnt++;
      }
    }
    return new String[] {camlist.toString(), grplist.toString()};
  }

  private String[] get_list_group_cameras(String name) {
    Group group = Config.current.getGroup(name);
    if (group == null) {
      JFLog.log(log, "group not found:" + name);
      return null;
    }
    String camlist = group.getCameraList();
    return new String[] {camlist};
  }

  public class WorkerKeepAlive extends Thread {
    private int cnt;
    public void run() {
      setName("WorkerKeepAlive");
      while (active) {
        JF.sleep(1000);
        cnt++;
        if (cnt > 30) {
          //perform gc every 30 seconds to avoid system crashes
          cnt = 0;
          if (fix_gc) {
            System.gc();
          }
        }
        long cut = System.currentTimeMillis() - 60 * 1000;
        try {
          synchronized (Config.current.camerasLock) {
            for(Camera cam : Config.current.cameras) {
              synchronized (cam.viewersLock) {
                int count = cam.viewers.size();
                for(int idx=0;idx<count;idx++) {
                  RTSPSession sess = cam.viewers.get(idx);
                  if (sess.ts < cut) {
                    JFLog.log(log, "DVR:Session expired:" + sess);
                    cam.remove_viewer(sess);
                    count--;
                  } else {
                    idx++;
                  }
                }
              }
            }
          }
        } catch (Exception e) {
          if (debug) JFLog.log(log, e);
        }
      }
    }
  }
}
