package javaforce.awt;

/** VNCServer.
 *
 * @author peter.quiring
 */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javaforce.*;
import javaforce.jni.*;

public class VNCServer {
  public boolean start(String pass, boolean service) {
    return start(pass, service, 5900);
  }
  public boolean start(String pass, boolean service, int port) {
    if (active) {
      stop();
    }
    if (pass == null || pass.length() != 8) {
      JFLog.log("VNCServer:pass must be 8 chars (pad with zero)");
      return false;
    }
    this.pass = pass;
    this.service = service;
    try {
      JFLog.log("VNCServer starting on port " + port + "...");
      active = true;
      ss = new ServerSocket(port);
      server = new Server();
      server.start();
      if (service) {
        session_server = new VNCSessionServer();
        session_server.start();
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  public void stop() {
    active = false;
    try {
      if (ss != null) {
        ss.close();
        ss = null;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    if (session_server != null) {
      session_server.stop();
      session_server = null;
    }
  }

  private Server server;
  private ServerSocket ss;
  private VNCSessionServer session_server;
  private boolean active;
  private boolean service;
  private String pass;
  private static boolean debug = true;

  private static class Config {
    public String password = "password";
    public int port = 5900;
  }

  private static Config loadConfig() {
    try {
      File file = new File(JF.getConfigPath() + "/jfvncserver.cfg");
      if (!file.exists()) {
        return new Config();
      }
      FileInputStream fis = new FileInputStream(file);
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      Config config = new Config();
      String password = props.getProperty("password");
      if (password != null && password.length() == 8) {
        config.password = password;
      }
      String port = props.getProperty("port");
      if (port != null) {
        config.port = JF.atoi(port);
        if (config.port < 0 || config.port > 65535) {
          config.port = 5900;
        }
      }
      return config;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private class Server extends Thread {
    public void run() {
      VNCRobot robot;
      while (active) {
        try {
          Socket s = ss.accept();
          if (service) {
            robot = spawnSession();
          } else {
            GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
            robot = new VNCJavaRobot(gfx.getDefaultScreenDevice());
          }
          if (robot == null) {
            s.close();
            continue;
          }
          Client client = new Client(s, robot);
          client.start();
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
    private VNCRobot spawnSession() {
      if (JF.isWindows()) {
        if (!WinNative.executeSession(System.getProperty("java.app.home") + "/jfvncsession.exe", new String[] {})) {
          return null;
        }
      } else {
        try {
          java.lang.Runtime.getRuntime().exec(new String[] {System.getProperty("java.app.home") + "/jfvncsession"});
        } catch (Exception e) {
          JFLog.log(e);
          return null;
        }
      }
      return session_server.getClient();
    }
  }

  private boolean[] keys = new boolean[256];

  private class Client extends Thread {
    private Socket s;
    private RFB rfb;
    private VNCRobot robot;
    private Rectangle size;
    private Updater updater;
    private boolean connected;
    private int buttons;
    public Client(Socket s, VNCRobot robot) {
      this.s = s;
      this.robot = robot;
    }
    public void run() {
      connected = true;
      try {
        rfb = new RFB();
        rfb.connect(s);
        rfb.writeVersion(RFB.VERSION_3_8);
        float ver = rfb.readVersion();
        rfb.writeAuthTypes();
        byte type = rfb.readAuthType();
        if (type != RFB.AUTH_VNC) throw new Exception("Auth failed");
        byte[] challenge = rfb.writeAuthChallenge();
        byte[] response = RFB.encodeResponse(challenge, pass.getBytes());
        byte[] reply = rfb.readAuthChallenge();
        for(int a=0;a<16;a++) {
          if (response[a] != reply[a]) {
            rfb.writeAuthResult(false);
            throw new Exception("Auth Failed");
          }
        }
        rfb.writeAuthResult(true);
        size = robot.getScreenSize();
        rfb.writeServerInit(size.width, size.height);
        rfb.readClientInit();
        updater = new Updater();
        updater.start();
        //read requests from client
        while (active && connected) {
          connected = rfb.isConnected();
          int cmd = rfb.readMessageType();
          switch (cmd) {
            case RFB.C_MSG_BUFFER_REQUEST: {
              RFB.Rectangle rect = rfb.readBufferUpdateRequest();
              updater.refresh(rect);
              break;
            }
            case RFB.C_MSG_MOUSE_EVENT: {
              RFB.RFBMouseEvent event = rfb.readMouseEvent();
              try {
                robot.mouseMove(event.x, event.y);
                int mask = 1;
                int button = 1;
                for(int a=0;a<3;a++) {
                  if ((buttons & mask) != (event.buttons & mask)) {
                    if ((event.buttons & mask) == 0) {
                      robot.mouseRelease(button);
                    } else {
                      robot.mousePress(button);
                    }
                  }
                  mask <<= 1;
                  button++;
                }
              } catch (Exception e) {
                JFLog.log(e);
              }
              buttons = event.buttons;
              break;
            }
            case RFB.C_MSG_KEY_EVENT: {
              RFB.RFBKeyEvent event = rfb.readKeyEvent();
              try {
                if (event.down) {
                  robot.keyPress(event.code);
                  if (JF.isWindows()) {
                    //check for Ctrl+Alt+Delete
                    int code = VNCRobot.convertRFBKeyCode(event.code);
                    if (code > 0 && code < 256) {
                      keys[code] = true;
                    }
                    boolean cad = code == KeyEvent.VK_DELETE;
                    if (keys[KeyEvent.VK_SHIFT]) cad = false;
                    if (!keys[KeyEvent.VK_CONTROL]) cad = false;
                    if (!keys[KeyEvent.VK_ALT]) cad = false;
                    if (cad) {
                      if (debug) {
                        JFLog.log("Simulating Ctrl+Alt+Del");
                      }
                      WinNative.simulateCtrlAltDel();
                    }
                  }
                } else {
                  robot.keyRelease(event.code);
                  if (JF.isWindows()) {
                    int code = VNCRobot.convertRFBKeyCode(event.code);
                    if (code > 0 && code < 256) {
                      keys[code] = false;
                    }
                  }
                }
              } catch (Exception e) {
                JFLog.log(e);
                JFLog.log("vk=" + String.format("0x%04x", event.code));
              }
              break;
            }
            case RFB.C_MSG_SET_ENCODING: {
              int[] encodings = rfb.readEncodings();
              if (encodings == null) {
                throw new Exception("invalid encodings");
              }
              break;
            }
            case RFB.C_MSG_SET_PIXEL_FORMAT: {
              RFB.PixelFormat pf = rfb.readPixelFormat();
              //ignored
              break;
            }
            case RFB.C_MSG_CUT_TEXT: {
              String text = rfb.readCutText();
              //ignored
              break;
            }
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
        close();
      }
      robot.close();
    }
    public void close() {
      connected = false;
      try { s.close(); } catch (Exception e) {}
    }
    private class Updater extends Thread {
      private int[] img;
      private boolean refresh;
      private static final int INF = 64 * 1024;
      public void run() {
        //write screen changes to client
        refresh = true;  //send init full update
        try {
          while (connected) {
            if (!rfb.haveEncodings()) {
              JF.sleep(100);
              continue;
            }
            Rectangle new_size = robot.getScreenSize();
            if (new_size.width != size.width || new_size.height != size.height) {
              //TODO : screen size changed
            }
            if (refresh) {
              img = robot.getScreenCapture();
              rfb.setBuffer(img);
              RFB.Rectangle rect = new RFB.Rectangle();
              rect.width = size.width;
              rect.height = size.height;
              rfb.writeBufferUpdate(rect);
              refresh = false;
            } else {
              int[] update = robot.getScreenCapture();
              boolean changed = false;
              int x1 = INF, x2 = -1;
              int y1 = INF, y2 = -1;
              int x = 0;
              int width = size.width;
              int y = 0;
              int height = size.height;
              int idx = 0;
              int[] ipx = img;
              int[] upx = update;
              for(y=0;y<height;y++) {
                for(x=0;x<width;x++) {
                  if (ipx[idx] != upx[idx]) {
                    if (x1 > x) {
                      x1 = x;
                    }
                    if (x2 < x) {
                      x2 = x;
                    }
                    if (y1 > y) {
                      y1 = y;
                    }
                    if (y2 < y) {
                      y2 = y;
                    }
                    changed = true;
                    ipx[idx] = upx[idx];
                  }
                  idx++;
                }
              }
              if (changed) {
                rfb.setBuffer(update);
                RFB.Rectangle rect = new RFB.Rectangle();
                rect.x = x1;
                rect.y = y1;
                rect.width = x2 - x1 + 1;
                rect.height = y2 - y1 + 1;
                rfb.writeBufferUpdate(rect);
              }
            }
            JF.sleep(100);
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
      public void refresh(RFB.Rectangle rect) {
        refresh = true;
      }
    }
  }

  /** NOTE : THIS SERVICE DOES NOT WORK!!!
   *
   * java.awt.Robot does NOT work in a service.
   *
   * Looking into using native code soon.
   */

  private static VNCServer vnc;
  private static Config config;

  public static void serviceStart(String[] args) {
    JFLog.append(JF.getLogPath() + "/jfvncsvc.log", true);
    config = loadConfig();
    vnc = new VNCServer();
    vnc.start(config.password, true, config.port);
  }

  public static void serviceStop() {
    vnc.stop();
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      JFAWT.showError("Usage", "Usage:VNCServer {password} [port]");
      System.exit(1);
    }
    String password = args[0];
    int port = 5900;
    if (args.length > 1) {
      port = JF.atoi(args[1]);
      if (port < 0 || port > 65535) port = 5900;
    }
    VNCServer server = new VNCServer();
    server.start(password, false, port);
  }
}
