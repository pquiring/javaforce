package javaforce.controls.opcua;

import java.util.*;
import java.util.function.*;
import java.time.*;
import java.security.cert.*;
import java.util.concurrent.*;

import javaforce.*;
import javaforce.slf.*;

import org.eclipse.milo.opcua.stack.core.*;
import org.eclipse.milo.opcua.stack.core.transport.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.security.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.*;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.eclipse.milo.opcua.sdk.client.*;
import org.eclipse.milo.opcua.sdk.client.identity.*;
import org.eclipse.milo.opcua.sdk.server.*;
import org.eclipse.milo.opcua.sdk.server.util.*;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import static org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig.*;
import static org.eclipse.milo.opcua.stack.core.Stack.*;

/** OPCUA Client.
 *
 * WIP - Not working!
 * 
 *  See : https://github.com/eclipse-milo/milo/blob/main/milo-examples/client-examples/src/main/java/org/eclipse/milo/examples/client/ClientExample.java
 *
 * @author pquiring
 */

@Deprecated
public class OPCUAMiloClient {
  private String host;
  private int port;
  private OpcUaClient client;

  private static boolean netty;

  public boolean connect(String host, String user, String pass) {
    return connect(host, 4840, user, pass);
  }

  public boolean connect(String host, int port, String user, String pass) {
    this.host = host;
    this.port = port;
    try {
      if (!netty) {
        netty = true;
        JFLog.log("Disabling netty unsafe usage!");
        System.setProperty("org.jboss.netty.debug", "true");
        System.setProperty("io.netty.allocator.numDirectArenas", "0");
        System.setProperty("io.netty.noPreferDirect", "true");
        System.setProperty("io.netty.noUnsafe", "true");
      }
      client = createClient1(host, port, user, pass);
      println("connecting...");
      client.connect();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

 private OpcUaClient createClient1(String host, int port, String user, String pass) throws Exception {
    String endpointUrl = createUrl(host, port);
    /*
      create(String endpointUrl, Function<List<EndpointDescription>, Optional<EndpointDescription>> selectEndpoint, Consumer<OpcTcpClientTransportConfigBuilder> configureTransport, Consumer<OpcUaClientConfigBuilder> configureClient)
    */
    OpcUaClient client = OpcUaClient.create(
      endpointUrl,
      endpoints -> {
        List<EndpointDescription> eps = endpoints;
        for(EndpointDescription ep : eps) {
          JFLog.log("Endpoint=" + ep);
        }
        Optional<EndpointDescription> oep = endpoints.stream()
//        .filter(e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri()))
        .filter(e -> e.getSecurityMode() == MessageSecurityMode.SignAndEncrypt)
//        .filter(e -> endpointUrl.equals(e.getEndpointUrl()))
        .findFirst();
        EndpointDescription ep = oep.get();
        JFLog.log("Selected Endpoint=" + ep);
        return oep;
      },
      transport -> {},
      builder -> builder
        .setApplicationUri("urn:eclipse:milo:client")
        .setIdentityProvider(new UsernameProvider(user, pass)));
    return client;
  }

  private OpcUaClient createClient2(String host, int port, String user, String pass) {
    try {
      String endpointURL = createUrl(host, port);
      EndpointDescription[] endpoints = DiscoveryClient.getEndpoints(endpointURL).get().toArray(new EndpointDescription[0]);

      EndpointDescription endpoint = endpoints[0];

      JFLog.log("endpoint=" + endpoint);

      UsernameProvider usernameProvider = new UsernameProvider(user, pass);

      OpcUaClientConfig cfg = new OpcUaClientConfigBuilder()
        .setApplicationName(org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText.english("eclipse-milo-client"))
        .setApplicationUri("urn:eclipse:milo:client")
        .setEndpoint(endpoint)
        .setIdentityProvider(usernameProvider)
        .build();

      OpcUaClient client = OpcUaClient.create(cfg);

      return client;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private String createUrl(String host, int port) {
    String url = "opc.tcp://" + host + ":" + port + "/";
    println("URL=" + url);
    return url;
  }

  private static void println(String ln) {
    System.out.println(ln);
  }

  public static void main(String[] args) {
    try {
      if (args.length != 4) {
        println("Usage: OPCUAClient host port user pass");
        return;
      }
      new JFSLFServiceProvider().initialize();  //setup slf4j
      OPCUAMiloClient client = new OPCUAMiloClient();
      if (!client.connect(args[0], Integer.valueOf(args[1]), args[2], args[3])) {
        println("Connection failed!");
      } else {
        println("Connection successful!");
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}
