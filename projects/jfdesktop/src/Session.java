import java.util.*;

import javaforce.*;
import javaforce.linux.*;

/** Desktop Session.
 *
 * @author pquiring
 */

public class Session {
  private static Properties props;
  public static boolean is_wayland = false;
  private static int LOG_DEFAULT = 0;
  public static Dock dock;
  public static Desktop desktop;
  private static Object lock = new Object();

  private static void load_config() {
    props = Linux.getJavaForceProperties();
    is_wayland = getProperty("wayland").equals("true");
  }

  private static String getProperty(String name) {
    String prop = props.getProperty(name);
    if (prop == null) prop = "";
    return prop.trim();
  }

  public static void dispose() {
    synchronized (lock) {
      if (desktop != null) {
        desktop.dispose();
        desktop = null;
      }
      if (dock != null) {
        dock.dispose();
        dock = null;
      }
    }
  }

  public static void main(String[] args) {
    JFLog.init(LOG_DEFAULT, JF.getUserPath() + "/.jfdesktop-session.log", true);
    log_env();
    load_config();
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          try {
            JFLog.log("Creating Dock");
            try {
              dock = new Dock();
              dock.setVisible(true);
            } catch (Throwable t1) {
              JFLog.log(t1);
            }
            JFLog.log("Creating Desktop");
            try {
              desktop = new Desktop();
              desktop.setVisible(true);
            } catch (Throwable t2) {
              JFLog.log(t2);
            }
          } catch (Throwable t) {
            JFLog.log(t);
          }
        }
      });
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
  private static void log_env() {
    JFLog.log(LOG_DEFAULT, "Environment:");
    String[] envs = JF.getEnvironment();
    for(String e : envs) {
      JFLog.log(LOG_DEFAULT, e);
    }
  }
}
