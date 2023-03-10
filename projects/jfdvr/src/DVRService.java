/**
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.media.*;
import javaforce.voip.RTP;

public class DVRService extends Thread {
  public static DVRService dvrService;
  public static ConfigService configService;
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
    serviceStart(args);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        serviceStop();
      }
    });
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
    //start recording processes
    Config config = Config.current;
    for(int a=0;a<config.cameras.length;a++) {
      startCamera(config.cameras[a]);
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
    System.out.println("Start Camera:" + camera.name);
    CameraWorker instance = null;
    switch (camera.url.substring(0,4)) {
      case "rtsp": instance = new CameraWorkerVideo(camera); break;
      case "http": instance = new CameraWorkerPictures(camera); break;
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
        return;
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
}
