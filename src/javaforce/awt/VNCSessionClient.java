package javaforce.awt;

/** VNCSessionClient
 *
 * Runs in current session context to capture screen and simulate input events.
 * Used only when VNCServer runs as a system service.
 *
 * This Session acts as a VNCRobot over a TCP connection.
 *
 * @author peter.quiring
 */

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

import javaforce.*;
import javaforce.jni.*;

public class VNCSessionClient implements VNCRobot {

  public static final byte CMD_GET_SCREEN_SIZE = 1;
  public static final byte CMD_GET_SCREEN = 2;
  public static final byte CMD_KEY_DOWN = 3;
  public static final byte CMD_KEY_UP = 4;
  public static final byte CMD_MOUSE_MOVE = 5;
  public static final byte CMD_MOUSE_DOWN = 6;
  public static final byte CMD_MOUSE_UP = 7;
  public static final byte CMD_EXIT = 99;

  public static boolean debug = true;

  public static void main(String[] args) {
    if (debug) {
      JFLog.append(JF.getLogPath() + "/jfvncsession-" + System.currentTimeMillis() + ".log", true);
    }
    GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
    VNCSessionClient session = new VNCSessionClient(gfx.getDefaultScreenDevice());
    session.run();
  }

  private Robot robot;
  private GraphicsDevice screen;

  public VNCSessionClient(GraphicsDevice screen) {
    this.screen = screen;
    try {
      robot = new Robot(screen);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public Rectangle getScreenSize() {
    WinNative.setInputDesktop();
    String log = WinNative.getLog();
    if (log != null) {
      JFLog.log(log);
    }
    return screen.getDefaultConfiguration().getBounds();
  }

  public int[] getScreenCapture(int pf) {
    WinNative.setInputDesktop();
    String log = WinNative.getLog();
    if (log != null) {
      JFLog.log(log);
    }
    int[] rgb = JFImage.createScreenCapture(screen).getBuffer();
    if (pf == RFB.PF_RGB) return rgb;
    return RFB.swapPixelFormat(rgb);
  }

  public void keyPress(int code) {
    code = VNCRobot.convertRFBKeyCode(code);
    if (debug) {
      JFLog.log("keyPress:0x" + Integer.toString(code, 16));
    }
    try {
      robot.keyPress(code);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void keyRelease(int code) {
    code = VNCRobot.convertRFBKeyCode(code);
    if (debug) {
      JFLog.log("keyRelease:0x" + Integer.toString(code, 16));
    }
    try {
      robot.keyRelease(code);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void mouseMove(int x, int y) {
    try {
      robot.mouseMove(x, y);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void mousePress(int button) {
    button = VNCRobot.convertMouseButtons(button);
    try {
      robot.mousePress(button);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void mouseRelease(int button) {
    button = VNCRobot.convertMouseButtons(button);
    try {
      robot.mouseRelease(button);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void run() {
    //connect to VNCSessionServer
    try {
      Socket s = new Socket("127.0.0.1", VNCSessionServer.port);
      InputStream is = s.getInputStream();
      OutputStream os = s.getOutputStream();
      DataInputStream dis = new DataInputStream(is);
      DataOutputStream dos = new DataOutputStream(os);
      while (s.isConnected()) {
        byte cmd = (byte)is.read();
        switch (cmd) {
          case CMD_GET_SCREEN_SIZE: {
            Rectangle size = getScreenSize();
            dos.writeInt(size.width);
            dos.writeInt(size.height);
            break;
          }
          case CMD_GET_SCREEN: {
            Rectangle size = getScreenSize();
            int pf = is.read();
            dos.writeInt(size.width);
            dos.writeInt(size.height);
            int[] px32 = getScreenCapture(pf);
            int pixels = px32.length;
            byte[] px8 = new byte[pixels * 4];
            int i8 = 0;
            for(int i=0;i<pixels;i++) {
              BE.setuint32(px8, i8, px32[i]);
              i8 += 4;
            }
            dos.write(px8);
            break;
          }
          case CMD_KEY_DOWN: {
            keyPress(dis.readInt());
            break;
          }
          case CMD_KEY_UP: {
            keyRelease(dis.readInt());
            break;
          }
          case CMD_MOUSE_MOVE: {
            int x = dis.readInt();
            int y = dis.readInt();
            mouseMove(x, y);
            break;
          }
          case CMD_MOUSE_DOWN: {
            mousePress(is.read());
            break;
          }
          case CMD_MOUSE_UP: {
            mouseRelease(is.read());
            break;
          }
          case CMD_EXIT: {
            System.exit(0);
            break;
          }
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void close() {System.exit(0);}
}
