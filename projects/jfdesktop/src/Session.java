/** Session.
 *
 * @author pquiring
 */

import javaforce.*;

public class Session {
  public static void main(String[] args) {
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
}
