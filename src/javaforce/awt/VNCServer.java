package javaforce.awt;

/** VNCServer.
 *
 * Default port : 5900
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
import javaforce.utils.*;

public class VNCServer {
  public final static String busPack = "net.sf.jfvnc";

  private VNCWebServer web;

  private boolean start() {
    return start(loadConfig(), true);
  }

  public boolean start(String pass) {
    Config config = new Config();
    config.password = pass;
    config.port = 5900;
    return start(config, false);
  }

  public boolean start(String pass, int port) {
    Config config = new Config();
    config.password = pass;
    config.port = port;
    return start(config, false);
  }

  private boolean start(Config config, boolean service_mode) {
    if (active) {
      stop();
    }
    this.config = config;
    config.validate();
    this.service_mode = service_mode;
    try {
      JFLog.log("VNCServer starting on port " + config.port + "...");
      active = true;
      ss = new ServerSocket(config.port);
      server = new Server();
      server.start();
      if (service_mode) {
        session_server = new VNCSessionServer();
        session_server.start();
      }
      if (config.web) {
        web = new VNCWebServer();
        web.start(getWebPort(), getWebSecurePort());
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
    if (web != null) {
      web.stop();
      web = null;
    }
  }
  public void setViewOnlyPassword(String viewonly_password) {
    config.viewonly = viewonly_password;
  }

  private Server server;
  private ServerSocket ss;
  private VNCSessionServer session_server;
  private JBusClient busClient;
  private boolean active;
  private boolean service_mode;
  private static boolean debug = false;
  public static final boolean update_sid = false;  //unfortunately java does not support switching the session ID - a new process must be created

  private static class Config {
    public String password;  //full control password
    public String viewonly;  //view only password
    public int port = 5900;
    public boolean web = true;
    public int webport = 5800;
    public int websecureport = 5843;
    public int delay = 100;  //screen poll delay
    public String user;  //linux only
    public String display = ":0";  //linux only (default = :0)

    private static final int min_delay = 100;
    private static final int max_delay = 5 * 1000;

    public void validate() {
      password = RFB.checkPassword(password);
      if (viewonly != null) {
        viewonly = RFB.checkPassword(viewonly);
      }
      if (delay < min_delay) {
        delay = min_delay;
      }
      if (delay > max_delay) {
        delay = max_delay;
      }
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("port=" + port + "\n");
      sb.append("web=" + web + "\n");
      sb.append("webport=" + webport + "\n");
      sb.append("websecureport=" + websecureport + "\n");
      sb.append("password=" + password + "\n");
      sb.append("delay=" + delay + "\n");
      if (viewonly != null) {
        sb.append("viewonly=" + viewonly + "\n");
      } else {
        sb.append("#viewonly=password\n");
      }
      if (user != null) {
        sb.append("user=" + user + "\n");
      } else {
        sb.append("#user=username  #linux account with X11 authorization\n");
      }
      sb.append("display=" + display + "  #linux display name\n");
      return sb.toString();
    }
  }

  private static Config loadConfig() {
    try {
      File file = new File(getConfigFile());
      FileInputStream fis = new FileInputStream(file);
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      Config config = new Config();
      String password = props.getProperty("password");
      if (password != null) {
        config.password = password;
      } else {
        config.password = randomPassword();
      }
      String viewonly = props.getProperty("viewonly");
      if (viewonly != null) {
        config.viewonly = viewonly;
      } else {
        config.viewonly = randomPassword();
      }
      String port = props.getProperty("port");
      if (port != null) {
        config.port = JF.atoi(port);
        if (config.port < 1 || config.port > 65535) {
          config.port = 5900;
        }
      }
      String web = props.getProperty("web");
      if (web != null) {
        config.web = web.equals("true");
      }
      String webport = props.getProperty("webport");
      if (webport != null) {
        config.webport = JF.atoi(webport);
        if (config.webport < 1 || config.webport > 65535) {
          config.webport = 5800;
        }
      }
      String websecureport = props.getProperty("websecureport");
      if (websecureport != null) {
        config.websecureport = JF.atoi(websecureport);
        if (config.websecureport < 1 || config.websecureport > 65535) {
          config.websecureport = 5843;
        }
      }
      String delaystr = props.getProperty("delay");
      if (delaystr != null) {
        config.delay = JF.atoi(delaystr);
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
      config.validate();
      return config;
    } catch (FileNotFoundException e) {
      //create default config
      Config config = new Config();
      config.password = randomPassword();
      config.viewonly = randomPassword();
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(config.toString().getBytes());
        fos.close();
      } catch (Exception e2) {
        JFLog.log(e2);
      }
      return config;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private static int getWebPort() {
    if (config == null) return 5800;
    return config.webport;
  }

  private static int getWebSecurePort() {
    if (config == null) return 5843;
    return config.websecureport;
  }

  private static String randomPassword() {
    byte[] cs = new byte[8];
    Random r = new Random();
    for(int a=0;a<8;a++) {
      cs[a] = (byte)(r.nextInt(26) + 'a');
    }
    return new String(cs);
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

  private ArrayList<Client> clients = new ArrayList<>();
  private Object clientsLock = new Object();

  private void addClient(Client client) {
    synchronized (clientsLock) {
      clients.add(client);
    }
  }

  private void removeClient(Client client) {
    synchronized (clientsLock) {
      clients.remove(client);
    }
  }

  private int getClientCount() {
    return clients.size();
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
          addClient(client);
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
    private boolean control;
    private int buttons;
    private int pf = RFB.PF_LE_RGB;  //LE : default for VNC
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
        byte[] password_encoded = RFB.encodeResponse(challenge, config.password.getBytes());
        byte[] viewonly_encoded = null;
        if (viewonly_encoded != null) {
          viewonly_encoded = RFB.encodeResponse(challenge, config.viewonly.getBytes());
        }
        byte[] reply = rfb.readAuthChallenge();
        if (Arrays.equals(password_encoded, reply)) {
          rfb.writeAuthResult(true);
          control = true;
        } else if (viewonly_encoded != null && Arrays.equals(viewonly_encoded, reply)) {
          rfb.writeAuthResult(true);
          control = false;
        } else {
          rfb.writeAuthResult(false);
          throw new Exception("Auth Failed");
        }
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
              if (!control) break;
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
              if (!control) break;
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
              if (!control) break;
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
        try{robot.close();} catch (Exception e) {}
      }
      removeClient(this);
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
            JF.sleep(config.delay);
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
      return 33015;
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

  private static String getServiceFile() {
    return System.getProperty("user.dir") + "\\jfvncsvc.exe";
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      String usage = "";
      if (JF.isWindows()) {
        usage = "Usage:\nVNCServer {password} [port]\nVNCServer -install | -remove | -start | -stop";
      } else {
        usage = "Usage:\nVNCServer {password} [port]";
      }
      JFAWT.showError("Usage", usage);
      System.exit(1);
    }
    if (args[0].startsWith("-")) {
      if (!JF.isWindows()) {
        JFAWT.showError("Error", "Not supported");
        System.exit(1);
      }
      String exe = getServiceFile();
      if (!new File(exe).exists()) {
        JFAWT.showError("Error", "Unable to find jfvncsvc.exe");
        System.exit(1);
      }
      switch (args[0]) {
        case "-install": {
          WinService.create("jfVNCServer", exe);
          break;
        }
        case "-remove": {
          WinService.delete("jfVNCServer");
          break;
        }
        case "-start": {
          WinService.start("jfVNCServer");
          break;
        }
        case "-stop": {
          WinService.stop("jfVNCServer");
          break;
        }
        default: {
          JFAWT.showError("Error", "Unknown option:" + args[0]);
          System.exit(1);
          break;
        }
      }
    } else {
      JFLog.init("jfvnccli.log", true);
      String password = args[0];
      int port = 5900;
      if (args.length > 1) {
        port = JF.atoi(args[1]);
        if (port < 0 || port > 65535) port = 5900;
      }
      VNCServer server = new VNCServer();
      server.start(password, port);
    }
  }

  public String getStatus() {
    StringBuilder msg = new StringBuilder();
    try {
      msg.append("Service active on port:" + service.config.port + "\n");
      msg.append("Clients active:" + getClientCount() + "\n");
    } catch (Exception e) {
      msg.append("Exception:" + e);
    }
    return msg.toString();
  }

  public static class JBusMethods {
    public void getConfig(String pack) {
      byte[] cfg = JF.readFile(getConfigFile());
      if (cfg == null) cfg = new byte[0];
      String config = new String(cfg);
      service.busClient.call(pack, "getConfig", JBusClient.quote(JBusClient.encodeString(config)));
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
    public void getStatus(String pack) {
      String status = null;
      if (service != null) {
        status = service.getStatus();
      } else {
        status = "Service not running!";
      }
      service.busClient.call(pack, "getStatus", JBusClient.quote(JBusClient.encodeString(status)));
    }
  }
}
