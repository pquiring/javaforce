package javaforce.voip;

/** SIP/RTSP Packet
 *
 * @author peterq.admin
 */
public class Packet {
  public byte data[];
  public int length;
  public int port;
  public String host;
}
