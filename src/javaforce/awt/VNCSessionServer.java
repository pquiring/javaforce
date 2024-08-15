package javaforce.awt;

/** VNCSessionServer
 *
 * RPC server that accepts connections from VNCSessionClient on port 5999.
 *
 * @author peter.quiring
 */

import java.io.*;
import java.net.*;
import java.awt.*;

import javaforce.*;
import javaforce.jni.*;

import static javaforce.awt.VNCSessionClient.*;

public class VNCSessionServer {

  public static final int port = 5999;

  private Server server;

  public void start() {
    server = new Server();
    server.start();
  }

  public void stop() {
    if (server != null) {
      server.cancel();
      server = null;
    }
  }

  private Object lock = new Object();
  private Client client;

  public class Server extends Thread {
    private ServerSocket ss;
    private boolean active;
    public void run() {
      active = true;
      try {
        ss = new ServerSocket(port, 0, Inet4Address.getLoopbackAddress());
      } catch (Exception e) {
        JFLog.log(e);
        return;
      }
      while (active) {
        try {
          Socket s = ss.accept();
          synchronized (lock) {
            client = new Client(s);
            lock.notify();
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }

    public void cancel() {
      active = false;
      try {
        if (ss != null) {
          ss.close();
          ss = null;
        }
      } catch (Exception e) {}
    }
  }

  public Client getClient() {
    synchronized (lock) {
      int count = 60;
      while (client == null) {
        try {lock.wait(1000);} catch (Exception e) {}
        count--;
        if (count == 0) return null;
      }
      Client ret = client;
      client = null;
      return ret;
    }
  }

  public static class Client implements VNCRobot {
    private Socket s;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean connected;

    public long token;
    public int sid;

    public Client(Socket s) {
      this.s = s;
      connected = true;
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
      dis = new DataInputStream(is);
      dos = new DataOutputStream(os);
    }

    public Rectangle getScreenSize() {
      try {
        if (JF.isWindows() && VNCServer.update_sid) {
          int newsid = WinNative.getSessionID();
          if ((newsid != -1) && (newsid != sid)) {
            if (!WinNative.setSessionID(token, newsid)) {
              JFLog.log("setSessionID failed");
            }
            if (debug) {
              JFLog.log("Update Session:old=" + sid + ":new=" + newsid + ":token=0x" + Long.toString(token, 16));
            }
            sid = newsid;
          }
          String log = WinNative.getLog();
          if (log != null) {
            JFLog.log(log);
          }
        }
        os.write(CMD_GET_SCREEN_SIZE);
        int width = dis.readInt();
        int height = dis.readInt();
        return new Rectangle(0, 0, width, height);
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
      return null;
    }

    public int[] getScreenCapture(int pf) {
      try {
        os.write(CMD_GET_SCREEN);
        os.write(pf);
        int width = dis.readInt();
        int height = dis.readInt();
        int pixels = width * height;
        int bytes = pixels * 4;
        byte[] px8 = dis.readNBytes(bytes);
        int[] px32 = new int[pixels];
        int i8 = 0;
        for(int i=0;i<pixels;i++) {
          px32[i] = BE.getuint32(px8, i8);
          i8 += 4;
        }
        return px32;
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
      return null;
    }

    public void keyPress(int code) {
      try {
        os.write(CMD_KEY_DOWN);
        dos.writeInt(code);
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
    }

    public void keyRelease(int code) {
      try {
        os.write(CMD_KEY_UP);
        dos.writeInt(code);
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
    }

    public void mouseMove(int x, int y) {
      try {
        os.write(CMD_MOUSE_MOVE);
        dos.writeInt(x);
        dos.writeInt(y);
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
    }

    public void mousePress(int button) {
      try {
        os.write(CMD_MOUSE_DOWN);
        dos.writeInt(button);
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
    }

    public void mouseRelease(int button) {
      try {
        os.write(CMD_MOUSE_UP);
        dos.writeInt(button);
      } catch (Exception e) {
        JFLog.log(e);
        connected = false;
      }
    }

    public boolean active() {
      return connected;
    }

    public void close() {
      try {
        if (connected) {
          os.write(CMD_EXIT);
          os.flush();
          s.close();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      connected = false;
      if (JF.isWindows()) {
        if (token != 0) {
          WinNative.closeSession(token);
        }
        token = 0;
      }
    }

    public String toString() {
      return "VNCSessionServer.Client:s=" + s;
    }
  }
}
