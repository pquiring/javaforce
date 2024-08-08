package javaforce.awt;

/** VNCServer.
 *
 * Runs in user mode only, does not run as a system service.
 *
 * @author peter.quiring
 */

import java.io.*;
import java.net.*;
import java.awt.*;

import javaforce.*;

public class VNCServer {
  public boolean start(String pass) {
    return start(pass, 5900);
  }
  public boolean start(String pass, int port) {
    if (active) {
      stop();
    }
    if (pass == null || pass.length() != 8) {
      JFLog.log("VNCServer:pass must be 8 chars (pad with zero)");
      return false;
    }
    this.pass = pass;
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
    private GraphicsDevice screen;
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
        JFLog.log("client version=" + ver);
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
        screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        size = screen.getDefaultConfiguration().getBounds();
        rfb.writeServerInit(size.width, size.height);
        rfb.readClientInit();
        updater = new Updater();
        updater.start();
        Robot robot = new Robot();
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
              int vk = RFB.convertRFBKeyCode(event.code);
              if (vk > 0xff00) break;  //invalid key codes
              try {
                if (event.down) {
                  robot.keyPress(vk);
                } else {
                  robot.keyRelease(vk);
                }
              } catch (Exception e) {
                JFLog.log(e);
                JFLog.log("vk=" + String.format("0x%04x", vk));
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
      private JFImage img;
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
            Rectangle new_size = screen.getDefaultConfiguration().getBounds();
            if (new_size.width != size.width || new_size.height != size.height) {
              //TODO : screen size changed
            }
            if (refresh) {
              img = JFImage.createScreenCapture(screen);
              rfb.setBuffer(img.getBuffer());
              RFB.Rectangle rect = new RFB.Rectangle();
              rect.width = size.width;
              rect.height = size.height;
              rfb.writeBufferUpdate(rect);
              refresh = false;
            } else {
              JFImage update = JFImage.createScreenCapture(screen);
              boolean changed = false;
              int x1 = INF, x2 = -1;
              int y1 = INF, y2 = -1;
              int x = 0;
              int width = size.width;
              int y = 0;
              int height = size.height;
              int idx = 0;
              int[] ipx = img.getBuffer();
              int[] upx = update.getBuffer();
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
                rfb.setBuffer(update.getBuffer());
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

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage:VNCServer password");
      System.exit(1);
    }
    VNCServer server = new VNCServer();
    server.start(args[0], 5900);
  }
}
