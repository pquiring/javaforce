package javaforce.awt;

/** VNCServer.
 *
 * @author peter.quiring
 */

import java.net.*;
import java.awt.*;

import javaforce.*;

public class VNCServer {
  public boolean start(String pass, VNCRobot robot) {
    return start(pass, robot, 5900);
  }
  public boolean start(String pass, VNCRobot robot, int port) {
    if (active) {
      stop();
    }
    if (pass == null || pass.length() != 8) {
      JFLog.log("VNCServer:pass must be 8 chars (pad with zero)");
      return false;
    }
    this.pass = pass;
    this.robot = robot;
    try {
      JFLog.log("VNCServer starting on port " + port + "...");
      active = true;
      ss = new ServerSocket(port);
      server = new Server();
      server.start();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
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
  }

  private Server server;
  private ServerSocket ss;
  private boolean active;
  private String pass;
  private VNCRobot robot;

  private class Server extends Thread {
    public void run() {
      while (active) {
        try {
          Socket s = ss.accept();
          Client client = new Client(s);
          client.start();
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }

  private class Client extends Thread {
    private Socket s;
    private RFB rfb;
    private Rectangle size;
    private Updater updater;
    private boolean connected;
    private int buttons;
    public Client(Socket s) {
      this.s = s;
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
                } else {
                  robot.keyRelease(event.code);
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

  public static void serviceStart(String[] args) {
    JFLog.append(JF.getLogPath() + "/jfvncsvc.log", true);
    VNCRobot robot;
    GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] devs = gfx.getScreenDevices();
    for(GraphicsDevice dev : devs) {
      JFLog.log("device=" + dev);
    }
    if (JF.isWindows()) {
      robot = new VNCWinRobot();
    } else {
      robot = new VNCJavaRobot(gfx.getDefaultScreenDevice());
    }
    vnc = new VNCServer();
    vnc.start("password", robot, 5900);
  }

  public static void serviceStop() {
    vnc.stop();
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage:VNCServer password");
      System.exit(1);
    }
    VNCRobot robot;
    VNCServer server = new VNCServer();
    GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
    boolean win = JF.isWindows();
    if (win) {
      robot = new VNCWinRobot();
    } else {
      robot = new VNCJavaRobot(gfx.getDefaultScreenDevice());
    }
    server.start(args[0], robot, 5900);
  }
}
