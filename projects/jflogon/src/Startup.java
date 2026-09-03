/** jfLogon startup.
 *
 * Created : Mar 31, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.bus.*;
import javaforce.linux.*;

public class Startup implements ShellProcessListener {
  private static ShellProcess display_mgr_process;
  private static boolean rebootFlag, shutdownFlag;
  private static boolean is_wayland = false;
  private static String display_mgr = "X";
  private static Properties props;
  private static Wayland wayland;

  public static AutoMounter autoMounter;
  public static JBusServer jbusServer;

  private static int LOG_DEFAULT = 0;
  private static int LOG_DISPLAY = 1;

  /** Main entry point for jfLinux system.*/
  public static void serviceStart(String[] args) {
    JFLog.init(LOG_DEFAULT, "/var/log/jflogon-system.log", true);
    JFLog.init(LOG_DISPLAY, "/var/log/jflogon-display.log", true);
    JFLog.log("jfLogon:Startup");
    log_env();
    try {
      fixSudoers();
      Linux.init();
      props = Linux.getJFLinuxProperties();
      is_wayland = getProperty("wayland").equals("true");
      if (is_wayland) {
        display_mgr = getProperty("display_manager");
        if (display_mgr.equals("")) {
          display_mgr = "labwc";
        }
        JFLog.log("wayland:display_manager=" + display_mgr);
      }
      //start jfsystemmgr
      jbusServer = new JBusServer(SystemBusNames.system, new JBusMethods());
      jbusServer.connect();
      //start automounter
      autoMounter = new AutoMounter();
      autoMounter.start();
      //start device monitor
      new DeviceMonitor().start();
      //stop plymouth
      hidePlymouth();
      create_server_xauth();
      boolean retry;
      do {
        retry = false;
        try {
          start();
        } catch (Exception e) {
          JFLog.log(e);
        }
        JF.sleep(1500);  //wait for display manager to start

        JF.exec(new String[] {"numlockx", "on"});
        try {
          if (new File("/etc/.live").exists()) {
            doLiveLogon();
          } else {
            createLogon();
          }
        } catch (java.awt.HeadlessException he) {
          JFLog.log(he);
          JF.sleep(500);
          File xorgconf = new File("/etc/X11/xorg.conf");
          if (xorgconf.exists()) {
            JFLog.log("X Failed : Attempting to delete /etc/X11/xorg.conf and try again");
            xorgconf.delete();
          }
          stop();
          retry = true;
        } catch (Exception e) {
          JFLog.log(e);
          stop();
          retry = true;
        }
      } while (retry);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void serviceStop() {
    //nop
  }

  private static void start_jf_wayland() {
    new Thread() {
      public void run() {
        wayland.start();
      }
    }.start();
  }

  private static void stop_jf_wayland() {
    wayland.stop();
  }

  private static void start() throws Exception {
    JFLog.log("Starting Display Manager...{" + display_mgr + "}");
    switch (display_mgr) {
      case "X": config_X(); start(new String[] {"/usr/bin/X"}, null); break;
      case "weston": config_weston(); start(new String[] {"/usr/bin/weston", "--modules", "jf-desktop-shell.so"}, new String[] {"XDG_RUNTIME_DIR=/run"}); break;
      case "labwc": config_labwc(); start(new String[] {"/usr/bin/labwc"}, new String[] {"XDG_RUNTIME_DIR=/run"}); break;
      case "sway": config_sway(); start(new String[] {"/usr/bin/sway"}, new String[] {"XDG_RUNTIME_DIR=/run"}); break;
      case "javaforce": config_jf_wayland(); start_jf_wayland(); break;
    }
  }

  private static void start(String[] cmds, String[] env) throws Exception {
    new Thread() {
      public void run() {
        display_mgr_process = new ShellProcess();
        display_mgr_process.keepOutput(false);
        display_mgr_process.addListener(new Startup());
        if (env != null) {
          for(String e : env) {
            int idx = e.indexOf('=');
            if (idx == -1) continue;
            String name = e.substring(0, idx);
            String value = e.substring(idx + 1);
            display_mgr_process.addEnvironmentVariable(name, value);
          }
        }
        display_mgr_process.run(cmds, true);
      }
    }.start();
  }

  public static void stop() throws Exception {
    if (display_mgr_process != null) {
      JFLog.log("Stopping Display Manager...");
      display_mgr_process.destroy();
      JF.sleep(500);
      for(int a=0;a<3;a++) {
        if (!display_mgr_process.isAlive()) break;
        JF.sleep(1000);
      }
      if (display_mgr_process.isAlive()) {
        display_mgr_process.destroyForcibly();
        JF.sleep(500);
      }
      display_mgr_process = null;
      JFLog.log("Display Manager stopped...");
    }
    if (display_mgr.equals("javaforce")) {
      stop_jf_wayland();
    }
  }

  private static void startUI(String[] cmds, String[] env) throws Exception {
    ShellProcess process = new ShellProcess();
    process.keepOutput(false);
    process.addListener(new Startup());
    if (env != null) {
      for(String e : env) {
        int idx = e.indexOf('=');
        if (idx == -1) continue;
        String name = e.substring(0, idx);
        String value = e.substring(idx + 1);
        process.addEnvironmentVariable(name, value);
      }
    }
    JFLog.log("Starting Logon Greeter...");
    process.run(cmds, true);
  }

  public static byte mcookie[] = new byte[16];

  private static void create_server_xauth() throws Exception {
    //write auth data to /root/.Xauthority
    Random r = new Random();
    for(int a=0;a<16;a++) {
      mcookie[a] = (byte)('a' + (Math.abs(r.nextInt()) % 26));
    }
    write_xauth("/root/.Xauthority");
  }

  private static void write_xauth(String fn) throws Exception {
    FileOutputStream fos = new FileOutputStream(fn);
    fos.write(new byte[] { (byte)0xfc, 0x00 });  //uint16 = 252
    fos.write(new byte[] { 0x00, 0x00 });  //uint16 = 0 (string length)
    fos.write(new byte[] { 0x00, 0x00 });  //uint16 = 0 (string length)
    fos.write(new byte[] { 0x12, 0x00 });  //uint16 = 0x12 (string length)
    fos.write("MIT-MAGIC-COOKIE-1".getBytes());  //magic string
    fos.write(new byte[] { 0x10, 0x00 });  //uint16 = 0x10 (data length)
    fos.write(mcookie);  //cookie
    fos.close();
  }

  private static void chown_xauth(String fn, String user) throws Exception {
    try {
      JF.exec(new String[] {"chown", user+":"+user, fn});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static void doLiveLogon() {
    try {
      FileInputStream fis = new FileInputStream("/etc/.live");
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      String user = props.getProperty("user");
      if (user == null) user = "jflive";
      //run session as live user
      runSession(user, "/usr/bin/jfdesktop", null, false);
      if (!is_wayland) {
        stop();
      }
      JF.sleep(1000);
      System.out.println("" + (char)0x1b + "[2J");  //clear screen
      System.out.println("\n\n\n\n\n\t\tPlease remove installation media and reboot\n\n\n\n\n");
//      shutdown("-H");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void runSession(String user, String session, String[] envs, boolean domainLogon) {
    try {
      if (is_wayland) {
        //stop();
      }
      getUserDetails(user);
      String xauthFile = homePath + "/.Xauthority";
      write_xauth(xauthFile);
      chown_xauth(xauthFile, user);
      if (!Linux.isMemberOf(user, "audio")) {
        //pulseaudio requires user to be member of 'audio' group
        JF.exec(new String[] {"usermod", "-aG", "audio", user});
      }
      if (!Linux.isMemberOf(user, "sambashare")) {
        //net usershare requires user to be member of 'sambashare' group
        JF.exec(new String[] {"usermod", "-aG", "sambashare", user});
      }
      if (!Linux.isMemberOf(user, "video")) {
        //video4linux requires user to be a member of 'video' group
        JF.exec(new String[] {"usermod", "-aG", "video", user});
      }
      String jid = "j" + Math.abs(new Random().nextInt());
      String cmd[] = new String[] {"/usr/bin/sudo", "-E", "-u", user,
        domainLogon ? "/usr/sbin/jflogon-rundomain" : "/usr/sbin/jflogon-runsession",
        session};
      ProcessBuilder pb = new ProcessBuilder(cmd);
      Map<String,String> env = pb.environment();
      env.put("USER", user);
      env.put("LOGNAME", user);
      env.put("SHELL", shellPath);
      env.put("HOME", homePath);
      env.put("XAUTHORITY", homePath + "/.Xauthority");
      env.put("JID", jid);
      if (is_wayland) {
        env.put("WAYLAND_DISPLAY", "wayland-0");
      } else {
        env.put("DISPLAY", ":0");
      }
      String xdg_runtime_dir = "/run/user/" + user;    // should be /run/user/{uid}
      new File(xdg_runtime_dir).mkdir();
      Linux.chown(xdg_runtime_dir, user);
      env.put("XDG_RUNTIME_DIR", xdg_runtime_dir);
      if (envs != null) {
        for(String e : envs) {
          int idx = e.indexOf('=');
          if (idx == -1) continue;
          String name = e.substring(0, idx);
          String value = e.substring(idx + 1);
          env.put(name, value);
        }
      }
      JFLog.log("JID=" + jid);
      JFLog.log("Starting session:" + session + " for user " + user);
      Process p = pb.start();
      p.waitFor();
      JFLog.log("Session has terminated");
      JFLog.log("Killing all processes for user " + user);
      JF.exec(new String[] {"killall", "-u", user});  //ensure session ended
      JF.sleep(1500);  //wait for windows to close
      if (!globalConfig.disableSleep) {
        if (rebootFlag) {
          JFLog.log("Rebooting...");
          rebootFlag = false;
          Startup.reboot();
          return;
        }
        if (shutdownFlag) {
          JFLog.log("Shutting down...");
          shutdownFlag = false;
          Startup.shutdown("-P");
          return;
        }
        if (is_wayland) {
          start();
        }
      } else {
        JFLog.log("Power functions disabled by security policy.");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static String homePath, shellPath;

  public static void getUserDetails(String user) throws Exception {
    //find path from /etc/passwd
    //passwd = user_name:x:uid:gid:full_name:home_dir:shell
    FileInputStream fis = new FileInputStream("/etc/passwd");
    int len = fis.available();
    byte passwd[] = new byte[len];
    fis.read(passwd);
    String text = new String(passwd);
    String lns[] = text.split("\n");
    for(int ln=0;ln<lns.length;ln++) {
      String fs[] = lns[ln].split(":");
      if (!fs[0].equals(user)) continue;
      homePath = fs[5];
      shellPath = fs[6];
      fis.close();
      return;
    }
    fis.close();
    throw new Exception("user not found");
  }

  /** Reboots PC */
  public static void reboot() {
    try {
      stop();
      showPlymouth();
      JFLog.log("Rebooting...");
      JF.exec(new String[] {"reboot"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Shuts down PC
   * @param type "-P" = powerdown, "-H"=halt
   */
  public static void shutdown(String type) {
    try {
      stop();
      showPlymouth();
      JFLog.log("Shutting down...,type=" + type);
      JF.exec(new String[] {"shutdown " + type + " now"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void createLogon() {
    if (!is_wayland) {
      Linux.x11_rr_reset("800x600");
    }
    try {
      startUI(new String[] {"/usr/bin/jflogon-ui"}, new String[] {"XAUTHORITY=/root/.Xauthority", "DISPLAY=:0", "XDG_RUNTIME_DIR=/run", "WAYLAND_DISPLAY=wayland-0"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static void hidePlymouth() {
    if (new File("/bin/plymouth").exists()) {
      try {
        JF.exec(new String[] {"/bin/plymouth","--quit"});
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  //this is interferring with reboot/shutdown
  private static void showPlymouth() {
    if (new File("/bin/plymouth").exists()) {
      try {
//        JF.exec(new String[] {"/bin/plymouth","--show-splash"});  //causes reboot to fail
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public void shellProcessOutput(String out) {
    JFLog.log(LOG_DISPLAY, out);
  }

  /** sudoers can NOT require tty or jflogon (and other apps that use sudo) fail. */
  public static void fixSudoers() {
    try {
      FileInputStream fis = new FileInputStream("/etc/sudoers");
      byte data[] = JF.readAll(fis);
      fis.close();
      String sudoers = new String(data);
      String lns[] = sudoers.split("\n");
      boolean patched = false;
      StringBuilder sb = new StringBuilder();
      for(int a=0;a<lns.length;a++) {
        if (lns[a].indexOf("requiretty") != -1 && !lns[a].startsWith("#")) {
          lns[a] = "#" + lns[a];
          patched = true;
        }
        sb.append(lns[a]);
        sb.append("\n");
      }
      if (!patched) return;
      FileOutputStream fos = new FileOutputStream("/etc/sudoers");
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static class GlobalConfig {
    public boolean disableSleep;
  }
  private static GlobalConfig globalConfig = new GlobalConfig();
  private String globalConfigFolder = "/etc/jfconfig.d/";
  private String globalConfigFile = "global.xml";

  public void loadGlobalConfig() {
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(globalConfigFolder + "/" + globalConfigFile);
      xml.read(fis);
      xml.writeClass(globalConfig);
    } catch (FileNotFoundException fnfe) {
      defaultGlobalConfig();
    } catch (Exception e) {
      defaultGlobalConfig();
      JFLog.log(e);
    }
  }

  private void defaultGlobalConfig() {
    globalConfig.disableSleep = false;
  }

  private static String quote(String str) {
    return "\"" + str + "\"";
  }

  public static class JBusMethods {
    public boolean sleep() {
      if (globalConfig.disableSleep) return false;
      try {
        JF.exec(new String[] {"systemctl", "suspend"});
      } catch (Exception e) {
        JFLog.log(e);
      }
      return true;
    }
    public boolean reboot() {
      if (globalConfig.disableSleep) return false;
      JFLog.log("Reboot requested on Session stop");
      rebootFlag = true;
      return true;
    }
    public boolean shutdown() {
      if (globalConfig.disableSleep) return false;
      JFLog.log("Shutdown requested on Session stop");
      shutdownFlag = true;
      return true;
    }
    public boolean upgradesAvailable(int upgrades) {
      //TODO : broadcast
      jbusServer.invoke("javaforce.jflinux.jfdesktop.*", "updatesAvailable", upgrades);
      return true;
    }
    public boolean mount(String dev) {
      JFLog.log("mount:" + dev);
      Startup.autoMounter.mount(dev);
      return true;
    }
    public boolean umount(String path) {
      JFLog.log("umount:" + path);
      AutoMounter.Mount mount = Startup.autoMounter.getMount(path);
      if (mount == null) {
        JFLog.log("umount:" + path + ":Error:never mounted by AutoMounter");
        //try to fix this
        mount = new AutoMounter.Mount();
        mount.media = path;
      }
      Startup.autoMounter.umount(mount);
      return true;
    }
    private String cleanName(String name) {
      //filter out bad chars in volume names
      StringBuilder sb = new StringBuilder();
      char in[] = name.toCharArray();
      for(int a=0;a<in.length;a++) {
        switch (in[a]) {
          case ':':
          case ';':
          case '*':
          case '?':
            break;
          default:
            sb.append(in[a]);
            break;
        }
      }
      return sb.toString();
    }
    public boolean renameDevice(String media, String newName) {
      newName = cleanName(newName);
      if (newName.length() == 0) return false;
      //get device name
      AutoMounter.Mount mount = Startup.autoMounter.getMount("/media/" + media);
      if (mount == null) return false;
      if (mount.fs.equals("iso9660")) return false;
      //umount it
      Startup.autoMounter.umount(mount.dev);
      JF.sleep(500);  //just in case
      //change name
      String cmd[] = {mount.fs + "fslabel", mount.dev, newName};
      try {JF.exec(cmd);} catch (Exception e) {JFLog.log(e);}
      JF.sleep(500);  //this is needed
      //mount it back
      Startup.autoMounter.mount(mount.dev);
      return true;
    }
    public String getStorageInfo(String dev) {
      AutoMounter.Mount tmp = new AutoMounter.Mount();
      String volName = Startup.autoMounter.getVolumeName(dev, tmp);
      if (volName == null) volName = "";
      if (tmp.fs == null) tmp.fs = "unknown";
      String mountPt = Startup.autoMounter.getMountPoint(dev);
      if (mountPt == null) mountPt = "";
      //TODO : xml , json ???
      return "storageInfo:" + dev + "," + volName + "," + tmp.fs + "," + mountPt;
    }
    public boolean stopAutoMounter() {
      AutoMounter.paused--;
      return true;
    }
    public boolean startAutoMounter() {
      AutoMounter.paused++;
      return true;
    }
    public boolean broadcastWAPList(String list) {
      //TODO : broadcast
      jbusServer.invoke("javaforce.jflinux.jfdesktop.*", "setWAPList", list);
      return true;
    }
    public boolean broadcastVideoChanged(String reason) {
      //TODO : broadcast
      jbusServer.invoke("javaforce.jflinux.jfdesktop.*", "videoChanged", reason);
      jbusServer.invoke("javaforce.jflinux.jfconfig.*", "videoChanged", reason);
      return true;
    }
  }
  private static String getProperty(String name) {
    String prop = props.getProperty(name);
    if (prop == null) prop = "";
    return prop.trim();
  }
  private static void config_X() {
    //nop
  }
  private static void config_weston() {
    JF.copyAll("/etc/jflogon/weston.ini", "/etc/xdg/weston/weston.ini");
  }
  private static void config_labwc() {
    String labwc =  JF.getUserPath() + "/.config/labwc";
    new File(labwc).mkdirs();
    JF.copyAll("/etc/jflogon/labwc-rc.xml", labwc + "/rc.xml");
    JF.copyAll("/etc/jflogon/labwc-menu.xml", labwc + "/menu.xml");
  }
  private static void config_sway() {
    String sway =  JF.getUserPath() + "/.config/sway";
    new File(sway).mkdirs();
    JF.copyAll("/etc/jflogon/labwc-rc.xml", sway + "/rc.xml");
    JF.copyAll("/etc/jflogon/labwc-menu.xml", sway + "/menu.xml");
  }
  private static void config_jf_wayland() {
    wayland = new Wayland();
  }
  private static void log_env() {
    JFLog.log(LOG_DEFAULT, "Environment:");
    String[] env = JF.getEnvironment();
    for(String e : env) {
      JFLog.log(LOG_DEFAULT, e);
    }
  }
}
