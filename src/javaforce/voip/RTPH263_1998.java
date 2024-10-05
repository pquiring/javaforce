package javaforce.voip;

/**
 * Encodes/Decodes RTP/H263+ packets (H263-1998)
 *
 * Payload type is Dynamic
 *
 * SDP Syntax = "H263-1998"
 *
 * http://tools.ietf.org/html/rfc2429
 * http://tools.ietf.org/html/rfc4629
 *
 * @author pquiring
 */

public class RTPH263_1998 implements RTPVideoCoder {

  private Packet packet;
  private int lastseqnum = -1;
  private static int maxSize = 4 * 1024 * 1024;
  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 2(rtp_h263_header) = 1444 bytes payload per packet
  private static final int mtu = 1444;
  private int seqnum;
  private int timestamp;
  private final int ssrc;

  //RTP/H263 header bits
  private static final int P = 0x20;  //picture start

  //RTP header bits
  private static final int M = 0x80;  //M bit

  public RTPH263_1998() {
    ssrc = new java.util.Random().nextInt();
    packet = new Packet();
    packet.data = new byte[maxSize];
  }

  private int rtp_id;
  public void setid(int id) {
    rtp_id = id;
  }

  private int find_best_length(byte[] data, int offset, int length) {
    //see if there is a 0,0,x and return a length to that
    //this way the next packet will start at a resync point
    for(int a=1;a<length-3;a++) {
      if (data[offset + a] == 0 && data[offset + a + 1] == 0 && data[offset + a + 2] != 0) return a;
    }
    return length;
  }

  /** Encodes raw H.263 data into multiple RTP packets. */
  public void encode(byte[] data, int offset, int length, int x, int y, PacketReceiver pr) {
    int len = length;
    int packetLength;
    int pos = offset;
    boolean p;  //was 0,0 stripped off?
    while (len > 0) {
      p = false;
      if (len > 2 && data[pos] == 0 && data[pos + 1] == 0) {
        p = true;
        pos += 2;
        len -= 2;
      }
      if (len > mtu) {
        packetLength = find_best_length(data, pos, len);
      } else {
        packetLength = len;
      }
      packet.length = packetLength + 12 + 2;  //12=RTP.length 2=rtp_h263+_header.length
      RTPChannel.buildHeader(packet.data, rtp_id, seqnum++, timestamp, ssrc, len == packetLength);
      //build H.263 header (2 bytes)
      packet.data[12] = (byte)(p ? 0x20 : 0x00);
//      packet.data[13] = 0x00;
      System.arraycopy(data, pos, packet.data, 12 + 2, packetLength);
      pos += packetLength;
      len -= packetLength;
      pr.onPacket(packet);
    }
    packet.length = 0;
    timestamp += 100;  //??? 10 fps ???
  }

  /**
   * Assembles RTP fragments into H263 packets.
   */
  public void decode(byte[] rtp, int offset, int length, PacketReceiver pr) {
    if (rtp.length < 12 + 2) return;  //bad packet
    int h263Length = rtp.length - 12 - 2;
    boolean p = (rtp[12] & P) == P;
    //if P is true a 0,0 is left between last packet and this packet
    System.arraycopy(rtp, 12 + 2, packet.data, packet.length + (p ? 2 : 0), h263Length);
    if (p) {
      packet.data[packet.length] = 0;
      packet.data[packet.length + 1] = 0;
    }
    packet.length += h263Length;
    if ((rtp[1] & M) == M) {
      pr.onPacket(packet);
      packet.length = 0;
    }
  }
}
