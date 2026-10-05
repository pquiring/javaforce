package javaforce.tests.webui;

import java.io.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

/** Tests WebUI Video functions
 *
 * @author pquiring
 *
 * Created : Oct 3, 2026
 */

public class TestVideo extends Thread implements WebUIHandler {

  private String file = "test.mp4";

  //containers
  private String mkv = "video/x-matroska";  //not working
  private String mp4 = "video/mp4";
  private String webm = "video/webm";

  //video codecs
  private String h264 = "avc1.42401e";
  private String h265 = "hvc1.1.6.L186.B0";
  private String h265vr = "hev1.1.6.L186.B0";  //variable resolution
  private String vp8 = "vp8";
  private String vp9 = "vp9";
  private String av1 = "av01.0.04M.08";

  //audio codes
  private String opus = "opus";
  private String mp4a = "mp4a.40.2";  //AAC

  private String codec = mp4 + ";codecs=" + h264 + "," + opus;

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new TestVideo().start();
      }
    });
  }

  public void run() {
    WebUIServer webui = new WebUIServer();
    webui.start(this, 8080);
    while (active) {
      JF.sleep(100);
    }
  }

  private boolean active = true;
  private WebUIClient client;
  private Video video_playback;
  private Video video_capture;
  private RandomAccessFile raf;
  private int mode = 0;
  private Reader reader;

  private static final int MODE_NONE = 0x00;
  private static final int MODE_PLAYBACK = 0x01;
  private static final int MODE_CAPTURE = 0x02;

  //WebUIHandler

  public void clientConnected(WebUIClient client) {
    JFLog.log("clientConnected:" + client);
    this.client = client;
    client.setProperty("init-segment", "false");  //got init segment (ftyp)
    client.setProperty("start-segment", "false");  //got start of segment (styp) (else wait for next styp)
  }

  public void clientDisconnected(WebUIClient client) {
    JFLog.log("clientDisconnected:" + client);
    this.client = null;
    //System.exit(0);
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebRequest request, WebResponse res) {
    //TODO : return static images, etc needed by webpage
    return null;
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    Panel panel = new Panel();

    mode = MODE_NONE;

    Label msg = new Label("Select playback or record (not both).");
    panel.add(msg);

    GridLayout grid = new GridLayout(2, 1);
    panel.add(grid);

    Panel left = new Panel();
    grid.add(left, 0, 0);

    video_playback = new Video();
    video_playback.setWidth(640);
    video_playback.setHeight(480);
    left.add(new Label("Playback recording"));
    left.add(video_playback);

    video_playback.addActionListener(new Action() {
      public void action(Component cmp) {
        JFLog.log("video_playback");
        if (mode != MODE_NONE) return;
        mode = MODE_PLAYBACK;
        start = System.currentTimeMillis();
        try {
          raf = new RandomAccessFile(file, "r");
        } catch (Exception e) {
          JFLog.log(e);
        }
        video_playback.setLiveSource(codec);
        reader = new Reader();
        reader.start();
      }
    });

    Panel right = new Panel();
    grid.add(right, 1, 0);

    video_capture = new Video();
    video_capture.setWidth(640);
    video_capture.setHeight(480);
//    video_capture.setFrameRate(10);
    right.add(new Label("Capture camera"));
    right.add(video_capture);

    video_capture.addActionListener(new Action() {
      public void action(Component cmp) {
        JFLog.log("video_capture");
        if (mode != MODE_NONE) return;
        mode = MODE_CAPTURE;
        video_capture.setCapture(true, true);
        WebUIClient.debug = true;
        try {
          client.setOutputPacketLength(true);
          client.setOutputStream(new FileOutputStream(file));
        } catch (Exception e) {}
      }
    });

    return panel;
  }

  double start;

  public String getCurrentTime() {
    double now = System.currentTimeMillis();
    return String.format("%.3f", (now - start) / 1000.0);
  }

  private class Reader extends Thread {
    public void run() {
      byte[] buf = new byte[4 * 1024 * 1024];
      while (active) {
        try {
          int len4 = raf.read(buf, 0, 4);
          if (len4 != 4) throw new Exception("failed to read packet length");
          int length = LE.getuint32(buf, 0);
          JFLog.log("length=" + length);
          int read = raf.read(buf, 0, length);
          if (read != length) throw new Exception("packeting error");
          if (read <= 0) break;
          appendBuffer(buf, 0, read);
        } catch (Exception e) {
          JFLog.log(e);
          break;
        }
        JF.sleep(100);  //10 fps
      }
      JFLog.log("Reader done");
    }
  }

  private int appendBuffer(byte[] data, int offset, int length) {
    JFLog.log("appendBuffer:" + length + ":" + getCurrentTime());
    client.sendDataEvent(data, offset, length, video_playback.getID(), "media_add_buffer", null);
    return length;
  }
}
