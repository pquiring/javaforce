/**
 * Created : July 10, 2012
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.linux.*;
import static javaforce.linux.Linux.*;
import javaforce.utils.*;

public class Startup {
  public static boolean openboxFailed = false;
  public static void main(String args[]) {
    JFLog.init(JF.getUserPath() + "/.jfdesktop.log", true);
    Linux.init();
    monitordir.init();
    /* Setup display */
    Monitor cfg[] = Linux.x11_rr_load_user();
    cfg = Linux.x11_rr_get_setup(cfg);
    Linux.x11_rr_set(cfg);
    /* Start openbox */
    if (!new File(JF.getUserPath() + "/.openbox.xml").exists()) {
      JF.copyAll("/etc/jfdesktop/openbox.xml", JF.getUserPath() + "/.openbox.xml");
    }
    JFLog.log("Starting openbox");
    try {
      Runtime.getRuntime().exec(new String[] {"/usr/bin/openbox", "--config-file", JF.getUserPath() + "/.openbox.xml"});
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
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    });
    JF.sleep(1000);
  }
}
