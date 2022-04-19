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
  public static ArrayList<ServerClient> clients = new ArrayList<ServerClient>();
  private Client local;

  public void run() {
    //accepts connections from clients
    try {
      JFLog.log("Starting server on port 33201");
      ss = new ServerSocket(33201);
      local = new Client();
      local.start();
      while (Status.active) {
        Socket s = ss.accept();
        ServerClient c = new ServerClient(s);
        c.start();
      }
      local.close();
    } catch (Exception e) {
//      JFLog.log(1, e);
    }
  }
  public void cancel() {
    try{ss.close();} catch (Exception e) {}
  }
  public ServerClient getClient(String name) {
    for(ServerClient client : clients) {
      String cname = client.getHost();
      if (cname == null) continue;
      if (cname.equals(name)) return client;
    }
    return null;
  }
  public ArrayList<ServerClient> getClients() {
    return clients;
  }
  public boolean addClient(ServerClient client) {
    synchronized(lock) {
      String host = client.getHost();
      if (Config.current.hosts.contains(host)) return false;
      Config.current.hosts.add(client.getHost());
//      Config.save();
      clients.add(client);
      return true;
    }
  }
  public void removeClient(ServerClient client) {
    synchronized(lock) {
      clients.remove(client);
      if (Config.current.hosts.contains(client.getHost())) {
        Config.current.hosts.remove(client.getHost());
//        Config.save();
      }
    }
  }
  public void dropClients() {
    //drop carrier on all clients
    synchronized(lock) {
      for(ServerClient client : clients) {
        client.close(true);
      }
    }
  }
}
