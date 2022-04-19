/**
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class QueryClients extends Thread {
  public void run() {
    while (Status.active) {
      JF.sleep(Config.current.update_delay);
      ArrayList<ServerClient> clients = MonitorService.server.getClients();
      long now = System.currentTimeMillis();
      long five_mins_ago = now - (5 * 1000 * 60);  //5 mins ago
      for(ServerClient client : clients) {
        //query any networks/filesystems for this client
        if (client.last > five_mins_ago) {
          //client still busy
          continue;
        }
        try {
          client.writeString("fs");
        } catch (Exception e) {}
        for(Network network : Config.current.getNetworks()) {
          if (network.host.equals(client.getHost())) {
            try {
              client.writeString("pn4");
              client.writeString(network.ip_nic);
              client.writeString(network.ip_first);
              client.writeString(network.ip_last);
            } catch (Exception e) {}
          }
        }
        client.last = now;
      }
      JF.sleep(1000);  //TODO : increase sleep
    }
  }
}
