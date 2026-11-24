package javaforce.controls.opcua;

import java.util.*;
import java.security.cert.*;

import javaforce.*;

import org.eclipse.milo.opcua.stack.core.transport.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.security.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.*;
import org.eclipse.milo.opcua.stack.transport.server.*;
import org.eclipse.milo.opcua.stack.transport.server.tcp.*;
import org.eclipse.milo.opcua.sdk.server.*;
import org.eclipse.milo.opcua.sdk.server.util.*;
import static org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig.*;

/** OPC-UA Server.
 *
 * WIP - Not working!
 *
 * See : https://github.com/eclipse-milo/milo/blob/main/milo-examples/server-examples/src/main/java/org/eclipse/milo/examples/server/ExampleServer.java
 *
 * @author pquiring
 */

@Deprecated
public class OPCUAMiloServer {
  private OpcUaServer server;
  private int tcpBindPort;

  public boolean start(int port) {
    this.tcpBindPort = port;
    try {
      OpcUaServerConfigBuilder configBuilder = new OpcUaServerConfigBuilder();
      configBuilder.setApplicationName(LocalizedText.english("Milo Minimal Server"));
      configBuilder.setEndpoints(createEndpointConfigs());
      OpcUaServerConfig config = configBuilder.build();
      OpcTcpServerTransportConfigBuilder transportBuilder = OpcTcpServerTransportConfig.newBuilder();
      OpcTcpServerTransportConfig transportConfig = transportBuilder.build();
      server = new OpcUaServer(config, new OpcServerTransportFactory() {
        public OpcServerTransport create(TransportProfile tp) {
          return new OpcTcpServerTransport(transportConfig);
        }
      });
      server.startup().get();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void stop() {
    try {
      server.shutdown().get();
      server = null;
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private Set<EndpointConfig> createEndpointConfigs() {
    var endpointConfigs = new LinkedHashSet<EndpointConfig>();

    String bindAddress = "0.0.0.0";

    String hostname = HostnameUtil.getHostname();

    EndpointConfig.Builder builder =
      EndpointConfig.newBuilder()
        .setBindAddress(bindAddress)
        .setHostname(hostname)
        .setPath("/milo")
        .addTokenPolicies(
          USER_TOKEN_POLICY_ANONYMOUS,
          USER_TOKEN_POLICY_USERNAME,
          USER_TOKEN_POLICY_X509);

    EndpointConfig.Builder noSecurityBuilder =
      builder
        .copy()
        .setSecurityPolicy(SecurityPolicy.None)
        .setSecurityMode(MessageSecurityMode.None);

    endpointConfigs.add(buildTcpEndpoint(noSecurityBuilder));

    // TCP Basic256Sha256 / SignAndEncrypt
    endpointConfigs.add(
      buildTcpEndpoint(
        builder
          .copy()
          .setSecurityPolicy(SecurityPolicy.Basic256Sha256)
          .setSecurityMode(MessageSecurityMode.SignAndEncrypt)));

    /*
     * It's good practice to provide a discovery-specific endpoint with no security.
     * It's required practice if all regular endpoints have security configured.
     *
     * Usage of the  "/discovery" suffix is defined by OPC UA Part 6:
     *
     * Each OPC UA Server Application implements the Discovery Service Set. If the OPC UA Server requires a
     * different address for this Endpoint it shall create the address by appending the path "/discovery" to
     * its base address.
     */

    EndpointConfig.Builder discoveryBuilder =
      builder
        .copy()
        .setPath("/milo/discovery")
        .setSecurityPolicy(SecurityPolicy.None)
        .setSecurityMode(MessageSecurityMode.None);

    endpointConfigs.add(buildTcpEndpoint(discoveryBuilder));

    return endpointConfigs;
  }

  private EndpointConfig buildTcpEndpoint(EndpointConfig.Builder base) {
    return base.copy()
      .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
      .setBindPort(tcpBindPort)
      .build();
  }
}
