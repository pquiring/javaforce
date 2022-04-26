/** QueryClients
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class QueryClients extends Thread {
  public void run() {
    for(int a=0;a<5 && Status.active;a++) {
      JF.sleep(1000);
    }
    while (Status.active) {
      if (Config.current.notify_unknown_device) {
        Notify.notify_unknowns();
      }
      ArrayList<ServerClient> clients = MonitorService.server.getClients();
      long now = System.currentTimeMillis();
      long past = now - (10 * 1000 * 60);  //10 mins ago
      for(ServerClient client : clients) {
        //query any networks/filesystems for this client
        if (client.last > past) {
          //client still busy
          continue;
        }
        client.last = now;
        //query file systems
        try {
          client.writeString("fs");
        } catch (Exception e) {}
        //query networks
        String client_host = client.getHost();
        boolean didScan = false;
        for(Network network : Config.current.getNetworks()) {
          if (network.host.equals(client_host)) {
            String id = network.getID();
            if (!client.networks.contains(id)) {
              didScan = true;
              client.networks.add(id);
              try {
                client.writeString("pn4");
                client.writeString(network.ip_nic);
                client.writeString(network.ip_first);
                client.writeString(network.ip_last);
              } catch (Exception e) {}
              break;
            }
          }
        }
        if (!didScan) {
          client.networks.clear();
        }
      }
      for(int a=0;a<60 && Status.active;a++) {
        JF.sleep(1000);
      }
    }
  }
}
