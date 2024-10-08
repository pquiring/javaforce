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

import javaforce.*;
import javaforce.jni.*;

public class VNCSessionClient extends VNCJavaRobot {

  public static final byte CMD_GET_SCREEN_SIZE = 1;
  public static final byte CMD_GET_SCREEN = 2;
  public static final byte CMD_KEY_DOWN = 3;
  public static final byte CMD_KEY_UP = 4;
  public static final byte CMD_MOUSE_MOVE = 5;
  public static final byte CMD_MOUSE_DOWN = 6;
  public static final byte CMD_MOUSE_UP = 7;
  public static final byte CMD_EXIT = 99;

  private int sid;

  public static boolean debug = false;

  public static void main(String[] args) {
    if (debug) {
      JFLog.append(JF.getLogPath() + "/jfvncsession-" + System.currentTimeMillis() + ".log", true);
    }
    try {
      if (debug) {
        JFLog.log("VNCSessionClient Starting...");
      }
      GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
      VNCSessionClient session = new VNCSessionClient(gfx.getDefaultScreenDevice());
      session.run();
    } catch (Throwable e) {
      JFLog.log(e);
    }
  }

  public VNCSessionClient(GraphicsDevice screen) {
    super(screen);
  }

  private Socket s;

  private void run() {
    //connect to VNCSessionServer
    try {
      if (debug) {
        JFLog.log("Connecting to 127.0.0.1:" + VNCSessionServer.port);
      }
      s = new Socket("127.0.0.1", VNCSessionServer.port);
      InputStream is = s.getInputStream();
      OutputStream os = s.getOutputStream();
      DataInputStream dis = new DataInputStream(is);
      DataOutputStream dos = new DataOutputStream(os);
      if (JF.isWindows()) {
        sid = -1;
        while (sid == -1) {
          sid = WinNative.getSessionID();
          JF.sleep(100);
        }
      }
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
            if (JF.isWindows()) {
              WinNative.setInputDesktop();
              if (VNCServer.update_sid) {
                int newsid = WinNative.getSessionID();
                if ((newsid != -1) && (newsid != sid)) {
                  GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
                  setRobot(gfx.getDefaultScreenDevice());
                  sid = newsid;
                }
              }
            }
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
            mousePress(dis.readInt());
            break;
          }
          case CMD_MOUSE_UP: {
            mouseRelease(dis.readInt());
            break;
          }
          case CMD_EXIT: {
            close();
            break;
          }
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    close();
  }

  public void close() {
    JFLog.log("VNCSessionClient Stopped");
    try {
      if (s != null) {
        s.close();
        s = null;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    System.exit(0);
  }
}
