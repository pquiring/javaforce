package javaforce.voip;

/** TransportInterface
 *
 * @author pquiring
 */

public interface TransportInterface {
  public void onConnect(String host, int port);
  public void onDisconnect(String host, int port);
}
