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
import static javaforce.linux.Linux.*;

public class Startup  implements ShellProcessListener {
  private static Properties props;
  private static boolean wayland = false;
  private static String window_mgr = "openbox";
  private static ShellProcess window_mgr_process;

  private static int LOG_DEFAULT = 0;
  private static int LOG_DISPLAY = 1;

  public static void main(String args[]) {
    JFLog.init(LOG_DEFAULT, JF.getUserPath() + "/.jfdesktop.log", true);
    Linux.init();
    props = Linux.getJFLinuxProperties();
    wayland = getProperty("wayland").equals("true");
    if (wayland) {
      window_mgr = getProperty("window_manager");
      if (window_mgr.length() == 0) {
        window_mgr = "labwc";
      }
    }
    try {
      /* Setup display */
      if (!wayland) {
        Monitor cfg[] = Linux.x11_rr_load_user();
        cfg = Linux.x11_rr_get_setup(cfg);
        /* Start Window Manager */
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
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          JFLog.log("Creating Dock");
          new Dock().setVisible(true);
          JFLog.log("Creating Desktop");
          new Desktop().setVisible(true);
        } catch (Throwable t) {
          JFLog.log(t);
        }
      }
    });
    JF.sleep(1000);
  }

  public void shellProcessOutput(String out) {
    JFLog.log(LOG_DISPLAY, out);
  }

  private static void start() throws Exception {
    JFLog.log("Starting window manager:" + window_mgr);
    switch (window_mgr) {
      case "javaforce": /* TODO */ break;
      case "sway": /* already running */ break;
      case "labwc": /* already running */ break;
      case "openbox": config_openbox(); start(new String[] {"/usr/bin/openbox"}, null); break;
    }
  }

  private static void start(String[] cmds, String[] env) throws Exception {
    new Thread() {
      public void run() {
        window_mgr_process = new ShellProcess();
        window_mgr_process.keepOutput(false);
        window_mgr_process.addListener(new Startup());
        if (env != null) {
          for(String e : env) {
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
  private static void config_sway() {
    String sway =  JF.getUserPath() + "/.config/sway";
    new File(sway).mkdirs();
  }
  private static void config_labwc() {
    String labwc =  JF.getUserPath() + "/.config/labwc";
    new File(labwc).mkdirs();
    JF.copyAll("/etc/jfdesktop/labwc-rc.xml", labwc + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/labwc-menu.xml", labwc + "/menu.xml");
  }
  private static void config_jf_wayland() {

  }
  private static void config_openbox() {
    String openbox =  JF.getUserPath() + "/openbox";
    new File(openbox).mkdir();
    JF.copyAll("/etc/jfdesktop/openbox-rc.xml", openbox + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/openbox-menu.xml", openbox + "/menu.xml");
  }
}
