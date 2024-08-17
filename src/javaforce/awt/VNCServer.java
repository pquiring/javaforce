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
import javaforce.jbus.*;
import javaforce.jni.*;

public class VNCServer {
  public final static String busPack = "net.sf.jfvnc";

  public boolean start() {
    config = loadConfig();
    return start(config.password, true, config.port);
  }

  public boolean start(String pass, boolean service) {
    return start(pass, service, 5900);
  }
  public boolean start(String pass, boolean service, int port) {
    if (active) {
      stop();
    }
    this.pass = RFB.checkPassword(pass);
    this.service_mode = service;
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
    if (busClient != null) {
      busClient.close();
      busClient = null;
    }
  }

  private Server server;
  private ServerSocket ss;
  private VNCSessionServer session_server;
  private JBusClient busClient;
  private boolean active;
  private boolean service_mode;
  private String pass;
  private static boolean debug = false;
  public static final boolean update_sid = false;  //unfortunately java does not support switching the session ID - a new process must be created

  private static class Config {
    public String password = "password";
    public int port = 5900;
    public String user;  //linux only
    public String display = ":0";  //linux only (default = :0)
  }

  private static Config loadConfig() {
    try {
      File file = new File(getConfigFile());
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
      if (JF.isUnix()) {
        String user = props.getProperty("user");
        if (user != null) {
          config.user = user;
        }
        String display = props.getProperty("display");
        if (display != null) {
          config.display = display;
        }
      }
      return config;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private VNCRobot newSession() {
    if (debug) {
      JFLog.log("Starting new Session");
    }
    long token = -1;
    if (JF.isWindows()) {
      if (debug) {
        JFLog.log("Session ID=" + WinNative.getSessionID());
      }
      token = WinNative.executeSession(System.getProperty("java.app.home") + "/jfvncsession.exe", new String[] {});
    } else {
      try {
        //support X11 now (wayland in the future)
        ProcessBuilder pb = new ProcessBuilder();
        pb.environment().put("DISPLAY", config.display);
        pb.environment().put("XAUTHORITY", "/home/" + getLinuxUser() + "/.Xauthority");
        pb.command(new String[] {System.getProperty("java.app.home") + "/jfvncsession"});
        pb.start();
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }
    VNCSessionServer.Client robot = session_server.getClient();
    if (JF.isWindows()) {
      robot.token = token;
      robot.sid = -1;
      while (robot.sid == -1) {
        robot.sid = WinNative.getSessionID();
        JF.sleep(100);
      }
    }
    if (debug) {
      JFLog.log("robot=" + robot);
    }
    return robot;
  }

  private String getLinuxUser() {
    if (config.user != null) return config.user;
    //return first user in /home
    File[] users = new File("/home").listFiles();
    if (users == null || users.length == 0) return "null";
    return users[0].getName();
  }

  private class Server extends Thread {
    public void run() {
      VNCRobot robot;
      busClient = new JBusClient(busPack, new JBusMethods());
      busClient.setPort(getBusPort());
      busClient.start();
      while (active) {
        try {
          Socket s = ss.accept();
          if (service_mode) {
            robot = newSession();
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
  }

  private boolean[] keys = new boolean[256];

  private class Client extends Thread {
    private Socket s;
    private RFB rfb;
    private Object lock = new Object();
    private VNCRobot robot;
    private Rectangle size;
    private Updater updater;
    private boolean connected;
    private int buttons;
    private int pf = RFB.PF_BGR;  //default for VNC
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
          synchronized (lock) {
            if (!robot.active()) {
              //should not get here
              robot.close();
              robot = newSession();
            }
          }
          switch (cmd) {
            case RFB.C_MSG_BUFFER_REQUEST: {
              RFB.Rectangle rect = rfb.readBufferUpdateRequest();
              updater.refresh(rect);
              break;
            }
            case RFB.C_MSG_MOUSE_EVENT: {
              RFB.RFBMouseEvent event = rfb.readMouseEvent();
              try {
                synchronized (lock) {
                  robot.mouseMove(event.x, event.y);
                  int mask = 1;
                  for(int a=0;a<3;a++) {
                    if ((buttons & mask) != (event.buttons & mask)) {
                      if ((event.buttons & mask) == 0) {
                        robot.mouseRelease(VNCRobot.convertMouseButtons(mask));
                      } else {
                        robot.mousePress(VNCRobot.convertMouseButtons(mask));
                      }
                    }
                    mask <<= 1;
                  }
                }
              } catch (Exception e) {
                JFLog.log(e);
              }
              buttons = event.buttons;
              break;
            }
            case RFB.C_MSG_KEY_EVENT: {
              RFB.RFBKeyEvent event = rfb.readKeyEvent();
              if (debug) {
                JFLog.log("KeyEvent:" + (event.down ? "down" : "up"));
                JFLog.log("old.key=0x" + Integer.toString(event.code, 16));
              }
              int code = VNCRobot.convertRFBKeyCode(event.code);
              if (debug) {
                JFLog.log("new.key=0x" + Integer.toString(code, 16));
              }
              try {
                if (event.down) {
                  if (JF.isWindows()) {
                    //check for Ctrl+Alt+Delete
                    if (code > 0 && code < 256) {
                      keys[code] = true;
                    }
                    boolean shift = keys[KeyEvent.VK_SHIFT];
                    boolean ctrl = keys[KeyEvent.VK_CONTROL];
                    boolean alt = keys[KeyEvent.VK_ALT];
                    if (debug) {
                      JFLog.log("shift=" + shift + ",ctrl=" + ctrl + ",alt=" + alt);
                    }
                    if (code == KeyEvent.VK_DELETE && !shift && ctrl && alt) {
                      if (debug) {
                        JFLog.log("Simulating Ctrl+Alt+Del");
                      }
                      WinNative.simulateCtrlAltDel();
                    }
                  }
                  if (code != -1) {
                    synchronized (lock) {
                      robot.keyPress(code);
                    }
                  }
                } else {
                  if (JF.isWindows()) {
                    if (code > 0 && code < 256) {
                      keys[code] = false;
                    }
                  }
                  if (code != -1) {
                    synchronized (lock) {
                      robot.keyRelease(code);
                    }
                  }
                }
              } catch (Exception e) {
                JFLog.log(e);
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
              RFB.PixelFormat rfb_pf = rfb.readPixelFormat();
              pf = rfb_pf.getFormat();
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
      synchronized (lock) {
        robot.close();
      }
    }
    public void close() {
      connected = false;
      try { s.close(); } catch (Exception e) {}
    }
    private class Updater extends Thread {
      private boolean updater_active;
      private int[] img;
      private boolean refresh;
      private int sid;
      private static final int INF = 64 * 1024;
      public void run() {
        updater_active = true;
        //write screen changes to client
        refresh = true;  //send init full update
        try {
          if (JF.isWindows()) {
            sid = -1;
            while (sid == -1) {
              sid = WinNative.getSessionID();
              JF.sleep(100);
            }
          }
          while (updater_active && connected) {
            if (!rfb.haveEncodings()) {
              JF.sleep(100);
              continue;
            }
            if (JF.isWindows() && service_mode && !update_sid) {
              int newsid = WinNative.getSessionID();
              if (newsid != -1 && newsid != sid) {
                //client needs a new robot
                if (debug) {
                  JFLog.log("Session ID change detected, creating a new session.");
                }
                synchronized (lock) {
                  robot.close();
                  robot = null;
                  robot = newSession();
                }
                sid = newsid;
              }
            }
            Rectangle new_size = null;
            synchronized (lock) {
              new_size = robot.getScreenSize();
            }
            if (new_size.width != size.width || new_size.height != size.height) {
              size = new_size;
              rfb.writeBufferUpdate(new RFB.Rectangle(new_size), RFB.TYPE_DESKTOP_SIZE);
            }
            if (refresh) {
              synchronized (lock) {
                img = robot.getScreenCapture(pf);
              }
              rfb.setBuffer(img);
              RFB.Rectangle rect = new RFB.Rectangle();
              rect.width = size.width;
              rect.height = size.height;
              rfb.writeBufferUpdate(rect, -1);
              refresh = false;
            } else {
              int[] update;
              synchronized (lock) {
                update = robot.getScreenCapture(pf);
              }
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
                rfb.writeBufferUpdate(rect, -1);
              }
            }
            JF.sleep(100);
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
      public void cancel() {
        updater_active = false;
        JF.sleep(250);
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

  private static VNCServer service;
  private static Config config;
  private static JBusServer busServer;

  public static void serviceStart(String[] args) {
    JFLog.init(getLogFile(), true);
    service = new VNCServer();
    service.start();
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
  }

  public static void serviceStop() {
    service.stop();
    if (busServer != null) {
      busServer.close();
      busServer = null;
    }
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33014;
    } else {
      return 777;
    }
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfvnc.log";
  }

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfvnc.cfg";
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      JFAWT.showError("Usage", "Usage:VNCServer {password} [port]");
      System.exit(1);
    }
    JFLog.init("jfvnccli.log", true);
    String password = args[0];
    int port = 5900;
    if (args.length > 1) {
      port = JF.atoi(args[1]);
      if (port < 0 || port > 65535) port = 5900;
    }
    VNCServer server = new VNCServer();
    server.start(password, false, port);
  }

  public static class JBusMethods {
    public void getConfig(String pack) {
      byte[] cfg = JF.readFile(getConfigFile());
      if (cfg == null) cfg = new byte[0];
      String config = new String(cfg);
      service.busClient.call(pack, "getConfig", service.busClient.quote(service.busClient.encodeString(config)));
    }
    public void setConfig(String cfg) {
      //write new file
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      service.stop();
      service = new VNCServer();
      service.start();
    }
  }
}
