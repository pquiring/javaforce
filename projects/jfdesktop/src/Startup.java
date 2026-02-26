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

public class Startup {
  private static boolean openboxFailed = false;
  private static boolean wayland = false;
  private static Properties props;

  public static void main(String args[]) {
    JFLog.init(JF.getUserPath() + "/.jfdesktop.log", true);
    Linux.init();
    props = Linux.getJFLinuxProperties();
    wayland = getProperty("wayland").equals("true");
    try {
      if (wayland) {
        /* Start labwc */
        if (!new File(JF.getUserPath() + "/labwc/rc.xml").exists()) {
          config_labwc();
        }
        JFLog.log("Starting labwc");
        Runtime.getRuntime().exec(new String[] {"/usr/bin/labwc"});
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
  private static String getProperty(String name) {
    String prop = props.getProperty(name);
    if (prop == null) prop = "";
    return prop.trim();
  }
  private static void config_labwc() {
    String labwc =  JF.getUserPath() + "/labwc";
    new File(labwc).mkdir();
    JF.copyAll("/etc/jfdesktop/labwc-rc.xml", labwc + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/labwc-menu.xml", labwc + "/menu.xml");
  }
  private static void config_openbox() {
    String openbox =  JF.getUserPath() + "/openbox";
    new File(openbox).mkdir();
    JF.copyAll("/etc/jfdesktop/openbox-rc.xml", openbox + "/rc.xml");
    JF.copyAll("/etc/jfdesktop/openbox-menu.xml", openbox + "/menu.xml");
  }
}
