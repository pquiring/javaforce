package server;

/** Server
 *
 * @author pquiring
 */

import java.net.*;

import javaforce.*;

import common.Config;

public class App extends Thread {
  public static void main(String args[]) {
    App server = new App();
    server.start();
  }

  private ServerSocket ss;
  private boolean active;

  public void run() {
    try {
      ss = new ServerSocket(Config.port);
      active = true;
      JFLog.log("Waiting for connection on port " + Config.port + "...");
      while (active) {
        Socket s = ss.accept();
        Session sess = new Session();
        sess.start(s);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
