package javaforce.voip;

/** SIP/RTSP Packet
 *
 * @author pquiring
 */

public class Packet {
  public Packet() {}
  public Packet(byte[] data, int offset, int length) {
    this.data = data;
    this.offset = offset;
    this.length = length;
  }
  public byte[] data;
  public int offset;
  public int length;

  public String host;
  public int port;

  public int stream;
  public long ts;
  public boolean keyFrame;

  public String toString() {
    return "Packet:{data:" + data + ",offset:" + offset + ",length:" + length + "}";
  }
}
