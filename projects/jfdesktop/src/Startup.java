/** jfDesktop startup.
 *
 * Created : July 10, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.linux.*;
import javaforce.api.linux.*;
import static javaforce.linux.Linux.*;

public class Startup  implements ShellProcessListener {
  private static Properties props;
  public static boolean is_wayland = false;
  private static String window_mgr = "openbox";
  private static ShellProcess window_mgr_process;
  private static Wayland wayland;
  private static String user;

  private static int LOG_DEFAULT = 0;
  private static int LOG_DISPLAY = 1;

  public static void main(String args[]) {
    JFLog.init(LOG_DEFAULT, JF.getUserPath() + "/.jfdesktop-system.log", true);
    JFLog.init(LOG_DISPLAY, JF.getUserPath() + "/.jfdesktop-display.log", true);
    JFLog.log("jfDesktop:Startup");
    log_env();
    user = System.getenv("USER");
    Linux.init();
    props = Linux.getJFLinuxProperties();
    is_wayland = getProperty("wayland").equals("true");
    if (is_wayland) {
      window_mgr = getProperty("window_manager");
      if (window_mgr.length() == 0) {
        window_mgr = "labwc";
      }
      JFLog.log("wayland:window_manager=" + window_mgr);
    }
    try {
      if (!is_wayland) {
        /* Setup X11 display */
        Monitor cfg[] = Linux.x11_rr_load_user();
        cfg = Linux.x11_rr_get_setup(cfg);
        Linux.x11_rr_set(cfg);
      }
      start();
    } catch (Exception e) {
      JFLog.log(e);
      System.exit(0);
    }
    /* Start PulseAudio */
    if (new File("/usr/bin/pulseaudio").exists()) {
      JFLog.log("Starting pulseaudio");
      try {
        Runtime.getRuntime().exec(new String[] {"/usr/bin/pulseaudio", "-nF", "/etc/pulse/default.pa"});
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    JFLog.log("jfDesktop:starting UI");
    try {
      int uid = LinuxAPI.getInstance().getUID();
      if (is_wayland) {
        startUI(new String[] {"/usr/bin/jfdesktop-session"}, new String[] {"XDG_RUNTIME_DIR=/run/user/" + uid, "XDG_SESSION_TYPE=wayland", "WAYLAND_DISPLAY=wayland-0"});
      } else {
        startUI(new String[] {"/usr/bin/jfdesktop-session"}, new String[] {"XDG_RUNTIME_DIR=/run/user/" + uid, "XAUTHORITY=/root/.Xauthority", "DISPLAY=:0"});
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
    JF.sleep(1000);
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
    JFLog.log("Starting Desktop Session...");
    process.run(cmds, true);
  }

  public void shellProcessOutput(String out) {
    JFLog.log(LOG_DISPLAY, out);
  }

  private static void start() throws Exception {
    JFLog.log("Starting window manager:" + window_mgr);
    int uid = LinuxAPI.getInstance().getUID();
    switch (window_mgr) {
      case "weston": config_weston(); start(new String[] {"/usr/bin/weston", "--modules", "jf-desktop-shell.so"}, new String[] {"XDG_RUNTIME_DIR=/run/user/" + uid}); break;
      case "labwc": config_labwc(); start(new String[] {"/usr/bin/labwc"}, new String[] {"XDG_RUNTIME_DIR=/run/user/" + uid}); break;
      case "sway": config_sway(); start(new String[] {"/usr/bin/sway"}, new String[] {"XDG_RUNTIME_DIR=/run/user/" + uid}); break;
      case "javaforce": config_jf_wayland(); start_jf_wayland(); break;
      case "openbox": config_openbox(); start(new String[] {"/usr/bin/openbox"}, null); break;
    }
  }

  private static void start(String[] cmds, String[] envs) throws Exception {
    new Thread() {
      public void run() {
        window_mgr_process = new ShellProcess();
        window_mgr_process.keepOutput(false);
        window_mgr_process.addListener(new Startup());
        if (envs != null) {
          for(String e : envs) {
            int idx = e.indexOf('=');
            if (idx == -1) continue;
            String name = e.substring(0, idx);
            String value = e.substring(idx + 1);
            window_mgr_process.addEnvironmentVariable(name, value);
          }
        }
        JFLog.log("Starting Window Manager...");
        window_mgr_process.run(cmds, true);
      }
    }.start();
  }

  public static void stop() throws Exception {
    if (window_mgr_process != null) {
      JFLog.log("Stopping Display Manager...");
      window_mgr_process.destroy();
      JF.sleep(500);
      for(int a=0;a<3;a++) {
        if (!window_mgr_process.isAlive()) break;
        JF.sleep(1000);
      }
      if (window_mgr_process.isAlive()) {
        window_mgr_process.destroyForcibly();
        JF.sleep(500);
      }
      window_mgr_process = null;
      JFLog.log("Display Manager stopped...");
    }
  }
  private static String getProperty(String name) {
    String prop = props.getProperty(name);
    if (prop == null) prop = "";
    return prop.trim();
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

  private static void config_weston() {
    JF.copyAll("/etc/jflogon/weston.ini", "/etc/xdg/weston/weston.ini");
  }
  private static void config_labwc() {
    String labwc =  JF.getUserPath() + "/.config/labwc";
    new File(labwc).mkdirs();
    JF.copyAll("/etc/jfdesktop/labwc-rc.xml", labwc + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/labwc-menu.xml", labwc + "/menu.xml");
  }
  private static void config_sway() {
    String sway =  JF.getUserPath() + "/.config/sway";
    new File(sway).mkdirs();
  }
  private static void config_jf_wayland() {
    wayland = new Wayland();
  }
  private static void config_openbox() {
    String openbox =  JF.getUserPath() + "/openbox";
    new File(openbox).mkdir();
    JF.copyAll("/etc/jfdesktop/openbox-rc.xml", openbox + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/openbox-menu.xml", openbox + "/menu.xml");
  }
  private static void log_env() {
    JFLog.log(LOG_DEFAULT, "Environment:");
    String[] envs = JF.getEnvironment();
    for(String e : envs) {
      JFLog.log(LOG_DEFAULT, e);
    }
  }
}
