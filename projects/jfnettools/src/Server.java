/**
 *
 * @author pquiring
 */

import java.net.*;

public class Server extends Thread {
  private NetApp win;
  private ServerSocket ss;
  private int port;
  private boolean active;
  public Server(NetApp win, int port) {
    this.win = win;
    this.port = port;
  }
  public void close() {
    active = false;
    try {ss.close();} catch (Exception e) {}
  }
  public void run() {
    try {
      active = true;
      ss = new ServerSocket(port);
      if (win != null) {
        win.setServerStatus("Running...");
      }
      while (active) {
        Socket s = ss.accept();
        new ServerClient(s).start();
      }
    } catch (Exception e) {
      if (!active) return;
      e.printStackTrace();
      if (win != null) {
        win.setServerStatus(e.toString());
      }
    }
  }
}
