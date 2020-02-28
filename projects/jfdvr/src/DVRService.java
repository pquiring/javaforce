/**
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;

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
    Paths.init(null);
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
    CameraWorker instance = new CameraWorker(camera);
    instance.start();
    list.add(instance);
  }

  public void stopCamera(Camera camera) {
    int cnt = list.size();
    for(int a=0;a<cnt;a++) {
      if (list.get(a).camera == camera) {
        try {list.get(a).cancel();} catch (Exception e) {JFLog.log(e);}
        list.remove(a);
        return;
      }
    }
  }

  private void setupFirewall() {
    try {
      File firewall_setup = new File(Paths.dataPath + "/firewall.setup");
      if (firewall_setup.exists()) return;
      firewall_setup.createNewFile();
      Runtime.getRuntime().exec("netsh advfirewall firewall add rule name=\"jfDVR_RTP_IN\" dir=in protocol=udp localport=5000-10000 action=allow");
      Runtime.getRuntime().exec("netsh advfirewall firewall add rule name=\"jfDVR_RTP_OUT\" dir=out protocol=udp localport=5000-10000 action=allow");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
