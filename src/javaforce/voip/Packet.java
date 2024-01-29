package javaforce.voip;

/** SIP/RTSP Packet
 *
 * @author pquiring
 */

public class Packet {
  public byte[] data;
  public int offset;
  public int length;
  public int port;
  public String host;
}
