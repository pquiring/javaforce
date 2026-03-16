/**
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
  private static boolean wayland = false;
  private static Properties props;
  private static ShellProcess display_mgr;

  private static int LOG_DEFAULT = 0;
  private static int LOG_DISPLAY = 1;


  public static void main(String args[]) {
    JFLog.init(LOG_DEFAULT, JF.getUserPath() + "/.jfdesktop.log", true);
    Linux.init();
    props = Linux.getJFLinuxProperties();
    wayland = getProperty("wayland").equals("true");
    try {
      if (wayland) {
        /* Start wayland compositor */
        config_wayland();
        JFLog.log("Starting wayland...");
        start();
        String[] env = JF.getEnvironment();
        for(String e : env) {
          JFLog.log(LOG_DEFAULT, e);
        }
      } else {
        /* Setup display */
        Monitor cfg[] = Linux.x11_rr_load_user();
        cfg = Linux.x11_rr_get_setup(cfg);
        /* Start openbox */
        Linux.x11_rr_set(cfg);
        if (!new File(JF.getUserPath() + "/openbox/rc.xml").exists()) {
          config_openbox();
        }
        JFLog.log("Starting openbox");
        Runtime.getRuntime().exec(new String[] {"/usr/bin/openbox"});
      }
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
    if (wayland) {
      start(new String[] {"/usr/bin/weston", "--modules", "jf-desktop-shell.so"}, new String[] {"XDG_RUNTIME_DIR=/run"});
    } else {
      start(new String[] {"/usr/bin/X"}, null);
    }
  }

  private static void start(String[] cmds, String[] env) throws Exception {
    new Thread() {
      public void run() {
        display_mgr = new ShellProcess();
        display_mgr.keepOutput(false);
        display_mgr.addListener(new Startup());
        if (env != null) {
          for(String e : env) {
            int idx = e.indexOf('=');
            if (idx == -1) continue;
            String name = e.substring(0, idx);
            String value = e.substring(idx + 1);
            display_mgr.addEnvironmentVariable(name, value);
          }
        }
        JFLog.log("Starting Display Server...");
        display_mgr.run(cmds, true);
      }
    }.start();
  }

  public static void stop() throws Exception {
    if (display_mgr != null) {
      JFLog.log("Stopping Display Manager...");
      display_mgr.destroy();
      JF.sleep(500);
      for(int a=0;a<3;a++) {
        if (!display_mgr.isAlive()) break;
        JF.sleep(1000);
      }
      if (display_mgr.isAlive()) {
        display_mgr.destroyForcibly();
        JF.sleep(500);
      }
      display_mgr = null;
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
  private static void config_wayland() {

  }
  private static void config_openbox() {
    String openbox =  JF.getUserPath() + "/openbox";
    new File(openbox).mkdir();
    JF.copyAll("/etc/jfdesktop/openbox-rc.xml", openbox + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/openbox-menu.xml", openbox + "/menu.xml");
  }
}
