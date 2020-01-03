/**
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.media.*;

public class DVRService extends Thread {
  public static DVRService dvrService;
  public static ConfigService configService;
  public final static boolean debug = false;
  public static void serviceStart(String args[]) {
    main(args);
  }

  public static void serviceStop() {
    dvrService.cancel();
  }

  public static void main(String args[]) {
    if (dvrService != null) return;
    dvrService = new DVRService();
    dvrService.start();
  }

  public void run() {
    Paths.init();
    //load current config
    Config.load();
    //start config service
    JFLog.log("APPDATA=" + System.getenv("APPDATA"));
    configService = new ConfigService();
    configService.start();
    if (!MediaCoder.loaded) return;  //not ready
    if (!debug) {
      MediaCoder.ffmpeg_set_logging(false);
    }
    //start recording threads
    Config config = Config.current;
    for(int a=0;a<config.cameras.length;a++) {
      addCamera(config.cameras[a]);
    }
  }

  public ArrayList<CameraWorker> list = new ArrayList<CameraWorker>();

  public void cancel() {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      list.get(a).cancel(false);
    }
  }

  public void addCamera(Camera camera) {
    CameraWorker worker = new CameraWorker(camera);
    worker.start();
    list.add(worker);
  }

  public void removeCamera(Camera camera) {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      if (list.get(a).camera == camera) {
        list.remove(a);
        return;
      }
    }
  }

  public void stopCamera(Camera camera, boolean restart) {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      if (list.get(a).camera == camera) {
        try {list.get(a).cancel(restart);} catch (Exception e) {JFLog.log(e);}
      }
    }
  }

  public void startCamera(Camera camera) {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      try {list.get(a).restart();} catch (Exception e) {JFLog.log(e);}
    }
  }
}
