/** Web Server that allows a remote-desktop media player type interface.
 *
 * @author pquiring
 */

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javaforce.*;
import javaforce.media.*;
import javaforce.service.*;

public class JFWebPlayer extends Thread implements WebHandler, WebSocketHandler, ActionListener {
  public static void main(String args[]) {
    try {
      JFLog.init("log.txt", true);
      if (!MediaCoder.init()) {
        if (!MediaCoder.download()) {
          JFLog.log("Error:MediaCoder download failed");
          System.exit(0);
        }
      }
      int port = 80;
      if (new File("jfwebplayer.ini").exists()) {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream("jfwebplayer.ini");
        props.load(fis);
        fis.close();
        String cfg_port = props.getProperty("port");
        if (cfg_port != null) {
          port = JF.atoi(cfg_port);
        }
      }
      JFWebPlayer service = new JFWebPlayer();
      service.start();
      Web server = new Web();
      server.setWebSocketHandler(service);
      server.start(service, port, false);
      JFLog.log("WebPlayer listening on port:" + port);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public JFImage org;
  public JFImage img;
  public boolean changed;
  public WebSocket client;
  public Robot robot;
  public SystemTray tray;
  public TrayIcon icon;
  public MenuItem exit;
  public static JFWebPlayer player;

  public void run() {
    player = this;
    java.awt.EventQueue.invokeLater(new Runnable() {public void run() {
      try {
        robot = new java.awt.Robot();
      } catch (Exception e) {
        JFLog.log(e);
      }
      //set icons
      try {
        JFImage appicon = new JFImage();
        appicon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("jfwebplayer.png"));
        tray = SystemTray.getSystemTray();
        Dimension size = tray.getTrayIconSize();
        JFImage scaled = new JFImage(size.width, size.height);
        scaled.fill(0, 0, size.width, size.height, 0x00000000, true);  //fill with alpha transparent
        if (true) {
          //scaled image (looks bad sometimes)
          scaled.getGraphics().drawImage(appicon.getImage()
            , 0, 0, size.width, size.height
            , 0, 0, appicon.getWidth(), appicon.getHeight()
            , null);
        } else {
          //center image
          scaled.getGraphics().drawImage(appicon.getImage()
            , (size.width - appicon.getWidth()) / 2
            , (size.height - appicon.getHeight()) / 2
            , null);
        }
        //create tray icon
        PopupMenu popup = new PopupMenu();
        exit = new MenuItem("Exit");
        exit.addActionListener(player);
        popup.add(exit);
        icon = new TrayIcon(scaled.getImage(), "jfWebPlayer", popup);
        icon.addActionListener(player);
        tray = SystemTray.getSystemTray();
        tray.add(icon);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }});
    try {
      org = JFImage.createScreenCapture();
      while (true) {
        img = JFImage.createScreenCapture();
        //compare images
        int px1[] = org.getBuffer();
        int px2[] = img.getBuffer();
        org = img;
        if (px1.length != px2.length) {
          changed = true;
        } else {
          int length = px1.length;
          for(int a=0;a<length;a++) {
            if (px1[a] != px2[a]) {
              changed = true;
              break;
            }
          }
        }
        JF.sleep(3000);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void sendIcon(WebResponse res, int clr) {
    JFImage img = new JFImage(32,32);
    img.fill(0, 0, 32, 32, clr);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    img.savePNG(baos);
    try {res.write(baos.toByteArray());} catch (Exception e) {}
  }

  public void doPost(WebRequest req, WebResponse res) {
    doGet(req,res);
  }

  public void doGet(WebRequest req, WebResponse res) {
    String url = req.getURL();
    JFLog.log("request=" + url);
    String params = req.getQueryString();
    int x = 0, y = 0;
    int mx = 0, my = 0;
    String f[] = params.split("&");
    for(int a=0;a<f.length;a++) {
      int idx = f[a].indexOf("=");
      if (idx == -1) continue;
      String key = f[a].substring(0, idx);
      String value = f[a].substring(idx+1);
      if (key.equals("x")) {
        x = Integer.valueOf(value);
      }
      else if (key.equals("y")) {
        y = Integer.valueOf(value);
      }
      else if (key.equals("mx")) {
        mx = Integer.valueOf(value);
      }
      else if (key.equals("my")) {
        my = Integer.valueOf(value);
      }
    }
    switch (url) {
      case "/screen":
        //send desktop image
        JFLog.log("screen size:" + x + "," + y);
        res.setContentType("image/png");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JFImage scaled = new JFImage(x,y);
        scaled.putJFImageScale(img, 0, 0, x, y);
        scaled.savePNG(baos);
        changed = false;
        try { res.write(baos.toByteArray()); } catch (Exception e) {}
        break;
      case "/touch":
        res.setStatus(200, "Ok");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double dx = x;
        double dy = y;
        double dmx = mx;
        double dmy = my;
        int sx = (int)(dmx / dx * screenSize.getWidth());
        int sy = (int)(dmy / dy * screenSize.getHeight());
        JFLog.log("touch@" + sx + "x" + sy);
        try {
          robot.mouseMove(sx, sy);
          robot.mousePress(InputEvent.BUTTON1_MASK);
          JF.sleep(15);
          robot.mouseRelease(InputEvent.BUTTON1_MASK);
        } catch (Exception e) {}
        break;
      case "/":
      case "/webplayer.html":
        try {
          byte[] html = this.getClass().getClassLoader().getResourceAsStream("jfwebplayer.html").readAllBytes();
          res.getOutputStream().write(html);
        } catch (Exception e) {}
        break;
      case "/webplayer.js":
        try {
          byte[] js = this.getClass().getClassLoader().getResourceAsStream("jfwebplayer.js").readAllBytes();
          res.getOutputStream().write(js);
        } catch (Exception e) {}
        break;
      case "/audio":
        res.setContentType("audio/mp3");
        new StreamAudio(res.getLiveOutputStream()).start();
        break;
      case "/red":
        sendIcon(res, 0xff0000);
        break;
      case "/green":
        sendIcon(res, 0x00ff00);
        break;
      case "/yellow":
        sendIcon(res, 0xffff00);
        break;
      case "/loading":
        sendIcon(res, 0xffffff);
        break;
      case "/play":
        //TODO
        break;
      case "/pause":
        //TODO
        break;
      case "/size":
        //TODO : send screen size (to scale touch commands)
        break;
      default:
        JFLog.log("404");
        res.setStatus(404, "Page not found");
        break;
    }
  }

  public boolean doWebSocketConnect(WebSocket sock) {
    client = sock;
    return true;
  }

  public void doWebSocketClosed(WebSocket sock) {
    client = null;
  }

  public void doWebSocketMessage(WebSocket sock, byte[] data, int type) {
    String msg = new String(data);
    JFLog.log("ws:" + msg);
    if (msg.equals("click")) {
      //TODO
      int x = 0;
      int y = 0;
      try {
        Robot bot = new Robot();
        bot.mouseMove(x, y);
        bot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        JF.sleep(10);
        bot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      } catch (Exception e) {}
    }
    if (msg.equals("ping")) {
      if (changed) {
        sock.write("{msg:'updateimage'}".getBytes());
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == exit) {
      System.exit(0);
    }
  }

  public static class StreamAudio extends Thread implements MediaIO {
    public OutputStream os;
    private volatile boolean active;
    public StreamAudio(OutputStream os) {
      this.os = os;
    }
    public void run() {
      active = true;
      AudioInput input = new AudioInput();
      MediaEncoder encoder = new MediaEncoder();
      encoder.audioBitRate = 192000;
      encoder.start(this, 0, 0, 0, 2, 44100, "mp3", false, true);
      int frame_size = encoder.getAudioFramesize() * 2;  //*2=stereo
      input.start(2, 44100, 16, frame_size, "<default>");
      short buf[] = new short[frame_size];
      while (active) {
        if (input.read(buf)) {
          encoder.addAudio(buf);
        } else {
          JF.sleep(100);
        }
      }
      input.stop();
      encoder.stop();
    }

    public int read(MediaCoder coder, byte[] data) {
      return 0;
    }

    public int write(MediaCoder coder, byte[] data) {
      if (!active) return 0;
      try {
        os.write(Web.chunkHeader(data));
        os.write(data);
        os.write("\r\n".getBytes());
      } catch (Exception e) {
        e.printStackTrace();
        active = false;
      }
      return data.length;
    }

    public long seek(MediaCoder coder, long pos, int how) {
      return 0;
    }
  }
}
