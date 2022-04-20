/** QueryClients
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class QueryClients extends Thread {
  public void run() {
    while (Status.active) {
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
        for(Network network : Config.current.getNetworks()) {
          if (network.host.equals(client_host)) {
            try {
              client.writeString("pn4");
              client.writeString(network.ip_nic);
              client.writeString(network.ip_first);
              client.writeString(network.ip_last);
            } catch (Exception e) {}
          }
        }
      }
      for(int a=0;a<60 && Status.active;a++) {
        JF.sleep(1000);
      }
    }
  }
}
