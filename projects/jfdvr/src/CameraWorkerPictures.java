/** CameraWorkerPictures
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.controls.*;

public class CameraWorkerPictures extends Thread implements CameraWorker {
  public Camera camera;
  private String url;  //camera.url without user:pass@
  private String user,pass;
  private String path;
  private long max_file_size;  //in bytes
  private long max_folder_size;  //in bytes

  private long folder_size;
  private int width = -1, height = -1;
  private long now;
  private int frameCount = 0;
  private boolean active = true;
  private static final int preview_x = 320;
  private static final int preview_y = 200;
  private static final int decoded_xy = 320 * 200;
  private JFImage captured_image;
  private JFImage preview_image;
  private int log;
  private String tag_value = "";
  private String filename;
  private String last_filename;

  private Controller controller;

  public Camera getCamera() {
    return camera;
  }

  private static class Recording {
    public File file;
    public long size;
    public long time;
  }

  private static int next_log = 1;
  private static synchronized int nextLog() {
    return next_log++;
  }

  private ArrayList<Recording> files = new ArrayList<Recording>();

  public CameraWorkerPictures(Camera camera) {
    log = nextLog();
    JFLog.append(log, Paths.logsPath + "/cam-" + camera.name + ".log", false);
    JFLog.setRetention(log, 5);
    JFLog.log(log, "Camera=" + camera.name);
    this.camera = camera;
    path = Paths.videoPath + "/" + camera.name;
    max_file_size = camera.max_file_size * 1024L * 1024L;
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
    preview_image = new JFImage(preview_x, preview_y);
    captured_image = new JFImage();
  }

  public void cancel() {
    active = false;
  }

  boolean last_value;

  public void run() {
    try {
      listFiles();
      if (!parseURL()) return;
      if (!connect()) return;
      last_value = camera.pos_edge;
      while (active) {
        //read tag_trigger until we need to take a picture
        byte[] data = controller.read(camera.tag_trigger);
        if (data == null) {
          JF.sleep(25);
          continue;
        }
        if (camera.pos_edge) {
          if (last_value == false) {
            if (data[0] == 0) {
              JF.sleep(25);
              continue;
            } else {
              last_value = true;
              //take_picture();
            }
          } else {
            last_value = data[0] != 0;
            JF.sleep(25);
            continue;
          }
        } else {
          if (last_value == true) {
            if (data[0] != 0) {
              JF.sleep(25);
              continue;
            } else {
              last_value = false;
              //take_picture();
            }
          } else {
            last_value = data[0] != 0;
            JF.sleep(25);
            continue;
          }
        }

        //take a snapshot
        take_picture();

        //clean up folder
        while (folder_size > max_folder_size) {
          Recording rec = files.get(0);
          files.remove(0);
          rec.file.delete();
          folder_size -= rec.size;
          JFLog.log(log, "delete recording:" + rec.file.getName());
        }
        //update preview
        if (camera.viewing && camera.update_preview) {
          if (captured_image != null) {
            if (!captured_image.loadJPG(last_filename)) {
              JFLog.log("failed to load last image");
              continue;
            }
            preview_image.putJFImageScale(captured_image, 0, 0, preview_x, preview_y);
            ByteArrayOutputStream preview = new ByteArrayOutputStream();
            preview_image.savePNG(preview);
            camera.preview = preview.toByteArray();
            camera.update_preview = false;
          }
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
    try {
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    JFLog.log(log, ":closing");
    JFLog.close(log);
  }
  public boolean parseURL() {
    //reset values
    width = -1;
    height = -1;
    String url = camera.url;
    String uri = null;
    String remotehost = null;
    int remoteport = 80;
    //http://[user:pass@]host[:port]/uri
    if (!url.startsWith("http://")) {
      return false;
    }
    url = url.substring(7);  //remove http://
    int idx = url.indexOf('/');
    if (idx != -1) {
      uri = url.substring(idx);
      url = url.substring(0, idx);
    } else {
      uri = "";
    }
    idx = url.indexOf("@");
    if (idx != -1) {
      String user_pass = url.substring(0, idx);
      url = url.substring(idx+1);
      idx = user_pass.indexOf(':');
      if (idx != -1) {
        user = user_pass.substring(0, idx);
        pass = user_pass.substring(idx+1);
      }
    }
    idx = url.indexOf(':');
    if (idx != -1) {
      remoteport = Integer.valueOf(url.substring(idx+1));
      url = url.substring(0, idx);
    }
    remotehost = url;
    this.url = "http://" + remotehost + ":" + remoteport + uri;
    return true;
  }

  //connect to controller
  public boolean connect() {
    controller = new Controller();
    return controller.connect(camera.controller);
  }

  public void disconnect() {
    if (controller != null) {
      controller.disconnect();
      controller = null;
    }
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

  private String getFilename() {
    Calendar now = Calendar.getInstance();
    return String.format("%s/%s%04d-%02d-%02d_%02d-%02d-%02d.jpg"
      , path
      , tag_value
      , now.get(Calendar.YEAR)
      , now.get(Calendar.MONTH) + 1
      , now.get(Calendar.DAY_OF_MONTH)
      , now.get(Calendar.HOUR_OF_DAY)
      , now.get(Calendar.MINUTE)
      , now.get(Calendar.SECOND)
    );
  }

  private void addRecording() {
    Recording rec = new Recording();
    rec.file = new File(filename);
    rec.size = rec.file.length();
    rec.time = rec.file.lastModified();
    files.add(rec);
    frameCount = 0;
    folder_size += rec.size;
  }

  public void reloadConfig() {
    JFLog.log(log, "Reloading config");
    max_file_size = camera.max_file_size * 1024L * 1024L;
    max_folder_size = camera.max_folder_size * 1024L * 1024L * 1024L;
  }

  private String get16(byte[] data, boolean isBE) {
    if (isBE)
      return Integer.toString(BE.getuint16(data, 0));
    else
      return Integer.toString(LE.getuint16(data, 0));
  }

  private String get32(byte[] data, boolean isBE) {
    if (isBE)
      return Integer.toString(BE.getuint32(data, 0));
    else
      return Integer.toString(LE.getuint32(data, 0));
  }

  private String get64(byte[] data, boolean isBE) {
    if (isBE)
      return Long.toString(BE.getuint64(data, 0));
    else
      return Long.toString(LE.getuint64(data, 0));
  }

  private void take_picture() {
    if (camera.tag_value.length() > 0) {
      byte[] data = controller.read(camera.tag_value);
      if (data == null) {
        tag_value = "error_";
      } else {
        switch (data.length) {
          case 1: tag_value = Integer.toString(data[0]) + "_"; break;
          case 2: tag_value = get16(data, controller.isBE()) + "_"; break;
          case 4: tag_value = get32(data, controller.isBE()) + "_"; break;
          case 8: tag_value = get64(data, controller.isBE()) + "_"; break;
          default: try {tag_value = new String(data, "UTF-8") + "_";} catch (Exception e) {JFLog.log(e); tag_value = "error_";}
        }
      }
    } else {
      tag_value = "";
    }
    filename = getFilename();
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      URL url = new URI(this.url).toURL();
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setAuthenticator(new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication (user, pass.toCharArray());
        }
      });
      conn.connect();
      InputStream is = conn.getInputStream();
      byte[] data = is.readAllBytes();
      fos.write(data);
      fos.close();
      last_filename = filename;
      addRecording();
    } catch(Exception e) {
      JFLog.log(e);
    }
  }
}
