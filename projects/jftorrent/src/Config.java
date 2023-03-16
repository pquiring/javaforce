/**
 *
 * @author pquiring
 */

public class Config {
  public static Config config;
  public Torrent torrent[] = new Torrent[0];
  public int port = 6881;
  public boolean multicast = false;
  public boolean dht = true;  //BEP 005
  public boolean fast = true;  //BEP 006
}
