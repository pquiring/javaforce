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

  public VNCRobot getClient() {
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

    public Client(Socket s) {
      this.s = s;
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
      } catch (Exception e) {
        JFLog.log(e);
      }
      dis = new DataInputStream(is);
      dos = new DataOutputStream(os);
    }

    public Rectangle getScreenSize() {
      try {
        os.write(CMD_GET_SCREEN_SIZE);
        int width = dis.readInt();
        int height = dis.readInt();
        return new Rectangle(0, 0, width, height);
      } catch (Exception e) {
        JFLog.log(e);
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
      }
      return null;
    }

    public void keyPress(int code) {
      try {
        os.write(CMD_KEY_DOWN);
        dos.writeInt(code);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void keyRelease(int code) {
      try {
        os.write(CMD_KEY_UP);
        dos.writeInt(code);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void mouseMove(int x, int y) {
      try {
        os.write(CMD_MOUSE_MOVE);
        dos.writeInt(x);
        dos.writeInt(y);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void mousePress(int button) {
      try {
        os.write(CMD_MOUSE_DOWN);
        dos.writeInt(button);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void mouseRelease(int button) {
      try {
        os.write(CMD_MOUSE_UP);
        dos.writeInt(button);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void close() {
      try {
        os.write(CMD_EXIT);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
}
