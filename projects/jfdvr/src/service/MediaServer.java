package service;

/** MediaServer
 *
 * Serves recorded files to a viewer.
 *
 * @author peter.quiring
 */

import java.io.*;

import javaforce.*;
import javaforce.media.*;
import javaforce.voip.*;

public class MediaServer {
  public Camera camera;

  private RTSPServer server;
  private RTSPSession sess;
  private long ts_start;
  private long ts_end;
  private long ts_delta;
  private boolean active;
  private boolean download;
  private Reader reader;

  public MediaServer(Camera camera, RTSPServer server, RTSPSession sess, String ts_start, String ts_end) {
    download = ts_end != null;
    this.camera = camera;
    active = true;
    this.ts_start = JF.atol(ts_start);
    this.ts_delta = System.currentTimeMillis() - this.ts_start;
    if (download) {
      this.ts_end = JF.atol(ts_end);
    }
    this.server = server;
    this.sess = sess;
  }

  public void start() {
    if (reader == null) {
      reader = new Reader();
      reader.start();
    }
  }

  public void stop() {
    if (reader != null) {
      reader.abort();
      reader = null;
    }
  }

  //reads media file and streams to player
  private class Reader extends Thread {
    private Media media;
    private long ts_current;
    public void run() {
      loadFile();
      while (active) {
        long now = System.currentTimeMillis();
        Media.Frame frame = media.readFrame();
        if (frame == null) {
          closeFile();
          loadFile();
          continue;
        }
        if (download) {
          if (frame.ts >= ts_end) {
            abort();
            server.set_parameter(sess, sess.uri, new String[] {"download: complete"});
            break;
          }
        } else {
          long delay = (now - ts_delta) - frame.ts;
          if (delay > 3) {
            JF.sleep((int)delay);
          }
        }
        sess.channel.writeRTP(frame.data, frame.offset, frame.length);
      }
      if (media != null) {
        closeFile();
      }
    }
    private void loadFile() {
      if (media != null) {
        closeFile();
      }
      //load file @ ts_current
      String filename;
      File file;
      boolean exists;
      long now = System.currentTimeMillis();
      do {
        ts_current = now - ts_delta;
        long secs = ts_current % (60 * 1000);
        long round = 0;
        if (secs < (30 * 1000)) {
          round = 15 * 1000;
        }
        filename = DVRService.getRecordingFilename(camera.name, ts_current + round);
        file = new File(filename);
        exists = file.exists();
        if (!exists) {
          ts_delta -= 60 * 1000;  //try next minute
          if (ts_delta <= 0) {
            //skipped ahead to now
            server.set_parameter(sess, sess.uri, new String[] {"ts: 0"});
            abort();
            return;
          }
        }
      } while (!exists);
      media = new Media();
      if (!media.open(filename)) {
        abort();
      }
      media.seekTime(ts_current);
    }
    private void abort() {
      active = false;
      closeFile();
    }
    private void closeFile() {
      if (media != null) {
        media.close();
        media = null;
      }
    }
  }
}
