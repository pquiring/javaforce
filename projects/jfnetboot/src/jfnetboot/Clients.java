package jfnetboot;

/** Clients
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Clients {

  public static HashMap<String, Client> clients = new HashMap();

  public static int getCount() {
    return clients.size();
  }

  public static Client[] getClients() {
    return clients.values().toArray(new Client[0]);
  }

  public static void init() {
    File[] files = new File(Paths.clients).listFiles();
    if (files == null) return;
    for(File file : files) {
      if (file.isDirectory()) continue;
      String name = file.getName();
      int idx = name.indexOf(".cfg");
      if (idx == -1) continue;
      name = name.substring(0, idx);
      idx = name.indexOf("-");
      if (idx == -1) continue;
      String serial = name.substring(0, idx);
      String arch = name.substring(idx + 1);
      JFLog.log("Client:init:" + serial + "-" + arch);
      getClient(serial, arch);
    }
  }

  public static void close() {
    Client[] clients = getClients();
    for(Client client : clients) {
      client.close();
    }
  }

  public static Client getClient(String serial, String arch) {
    Client client = clients.get(serial + "-" + arch);
    if (client == null) {
      //try to load client from disk
      if (arch == null) return null;
      client = Client.load(serial, arch);
      if (client == null) {
        //create new client with default filesystem
        client = new Client(serial, arch);
        client.save();
      }
      client.mount();
      clients.put(serial + "-" + arch, client);
    }
    return client;
  }

  public static void remove(Client client) {
    clients.remove(client.serial + "-" + client.arch);
  }

  public static boolean isFileSystemInUse(String name) {
    Client[] list = clients.values().toArray(new Client[0]);
    for(Client client : list) {
      if (client.filesystem.equals(name)) return true;
    }
    return false;
  }
}
