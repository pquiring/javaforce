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
import javaforce.api.linux.*;

public class Startup implements ShellProcessListener {
  private static ShellProcess display_mgr_process;
  private static boolean rebootFlag, shutdownFlag;
  public static boolean is_wayland = false;
  private static String display_mgr = "X";
  private static Properties props;
  private static Wayland wayland;

  public static AutoMounter autoMounter;
  public static JBusServer jbusServer;
  public static long pam;

  private static int LOG_DEFAULT = 0;
  private static int LOG_DISPLAY = 1;

  public static void load_config() {
    props = Linux.getJFLinuxProperties();
    is_wayland = getProperty("wayland").equals("true");
    if (is_wayland) {
      display_mgr = getProperty("display_manager");
      if (display_mgr.equals("")) {
        display_mgr = "labwc";
      }
      JFLog.log("wayland:display_manager=" + display_mgr);
    }
  }

  /** Main entry point for jfLinux system.*/
  public static void serviceStart(String[] args) {
    JFLog.init(LOG_DEFAULT, "/var/log/jflogon-system.log", true);
    JFLog.init(LOG_DISPLAY, "/var/log/jflogon-display.log", true);
    JFLog.log("jfLogon:Startup");
    log_env();
    try {
      fixSudoers();
      Linux.init();
      load_config();
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
          createLogon();
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

/*

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

 */

  public static void serviceStop() {
    //nop
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

  private static boolean start_jf_wayland() {
    new Thread() {
      public void run() {
        wayland.start();
      }
    }.start();
    return true;
  }

  private static boolean stop_jf_wayland() {
    wayland.stop();
    return true;
  }

  private static boolean start() throws Exception {
    JFLog.log("Starting Display Manager:" + display_mgr);
    boolean res = false;
    switch (display_mgr) {
      case "X": config_X(); res = start(new String[] {"/usr/bin/X"}, null); break;
      case "weston": config_weston(); res = start(new String[] {"/usr/bin/weston", "--modules", "jf-desktop-shell.so"}, new String[] {"XDG_RUNTIME_DIR=/run/user/0"}); break;
      case "labwc": config_labwc(); res = start(new String[] {"/usr/bin/labwc"}, new String[] {"XDG_RUNTIME_DIR=/run/user/0"}); break;
      case "sway": config_sway(); res = start(new String[] {"/usr/bin/sway"}, new String[] {"XDG_RUNTIME_DIR=/run/user/0"}); break;
      case "javaforce": config_jf_wayland(); res = start_jf_wayland(); break;
    }
    return res;
  }

  private static boolean start(String[] cmds, String[] envs) throws Exception {
    if (display_mgr_process != null) return false;
    new Thread() {
      public void run() {
        display_mgr_process = new ShellProcess();
        display_mgr_process.keepOutput(false);
        display_mgr_process.addListener(new Startup());
        if (envs != null) {
          for(String e : envs) {
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
    return true;
  }

  public static boolean stop() throws Exception {
    if (display_mgr_process == null) return false;
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
      return stop_jf_wayland();
    }
    return true;
  }

  private static void startUI(String[] cmds, String[] envs) throws Exception {
    ShellProcess process = new ShellProcess();
    process.keepOutput(false);
    process.addListener(new Startup());
    if (envs != null) {
      for(String e : envs) {
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
      if (is_wayland) {
        startUI(new String[] {"/usr/bin/jflogon-ui"}, new String[] {"XDG_RUNTIME_DIR=/run/user/0", "XDG_SESSION_TYPE=wayland", "WAYLAND_DISPLAY=wayland-0"});
      } else {
        startUI(new String[] {"/usr/bin/jflogon-ui"}, new String[] {"XDG_RUNTIME_DIR=/run/user/0", "XAUTHORITY=/root/.Xauthority", "DISPLAY=:0"});
      }
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
    public boolean startDisplayManager() {
      JFLog.log("startDisplayManager");
      try {
        return start();
      } catch (Throwable t) {
        JFLog.log(t);
        return false;
      }
    }
    public boolean stopDisplayManager() {
      JFLog.log("stopDisplayManager");
      try {
        return stop();
      } catch (Throwable t) {
        JFLog.log(t);
        return false;
      }
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
    String[] envs = JF.getEnvironment();
    for(String e : envs) {
      JFLog.log(LOG_DEFAULT, e);
    }
  }
}
