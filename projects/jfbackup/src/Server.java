/** Server API
 *
 * @author pquiring
 */

import java.net.*;
import java.util.*;

import javaforce.*;

public class Server extends Thread {
  private ServerSocket ss;
  private Object lock = new Object();
  private ArrayList<ServerClient> clients = new ArrayList<ServerClient>();
  private Client local;

  public void run() {
    //accepts connections from clients
    try {
      JFLog.log("Starting server on port 33200");
      ss = new ServerSocket(33200);
      local = new Client();
      local.start();
      while (Status.active) {
        Socket s = ss.accept();
        ServerClient c = new ServerClient(s);
        c.start();
        synchronized(lock) {
          clients.add(c);
        }
      }
      local.cancel();
    } catch (Exception e) {
//      JFLog.log(1, e);
    }
  }
  public void cancel() {
    try{ss.close();} catch (Exception e) {}
  }
  public ServerClient getClient(String name) {
    for(ServerClient client : clients) {
      String cname = client.getClientName();
      if (cname == null) continue;
      if (cname.equals(name)) return client;
    }
    return null;
  }
  public void removeClient(ServerClient client) {
    synchronized(lock) {
      clients.remove(client);
    }
  }
}
