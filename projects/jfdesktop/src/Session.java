/** Desktop Session.
 *
 * @author pquiring
 */

import javaforce.*;

public class Session {
  private static int LOG_DEFAULT = 0;
  public static void main(String[] args) {
    JFLog.init(LOG_DEFAULT, JF.getUserPath() + "/.jfdesktop-session.log", true);
    log_env();
    try {
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
