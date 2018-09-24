package javaforce.voip;

/**
 * Encodes/Decodes RTP/H263+ packets (H263-1998)
 *
 * Payload type is Dynamic
 *
 * SDP Syntax = "H263-1998"
 *
 * http://tools.ietf.org/html/rfc4629
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPH263_1998 {

  public RTPH263_1998() {
    ssrc = new Random().nextInt();
  }

  private int find_best_length(byte data[], int offset, int length) {
    //see if there is a 0,0,x and return a length to that
    //this way the next packet will start at a resync point
    for(int a=1;a<length-3;a++) {
      if (data[offset + a] == 0 && data[offset + a + 1] == 0 && data[offset + a + 2] != 0) return a;
    }
    return length;
  }

  /** Encodes raw H.263 data into multiple RTP packets. */
  public byte[][] encode(byte data[], int x, int y, int id) {
    ArrayList<byte[]> packets = new ArrayList<byte[]>();
    int len = data.length;
    int packetLength;
    int offset = 0;
    byte packet[];
    boolean P;  //was 0,0 stripped off?
    while (len > 0) {
      P = false;
      if (len > 2 && data[offset] == 0 && data[offset + 1] == 0) {
        P = true;
        offset += 2;
        len -= 2;
      }
      if (len > mtu) {
        packetLength = find_best_length(data, offset, len);
      } else {
        packetLength = len;
      }
      packet = new byte[packetLength + 12 + 2];  //12=RTP.length 2=rtp_h263+_header.length
      RTPChannel.buildHeader(packet, id, seqnum++, timestamp, ssrc, len == packetLength);
      //build H.263 header (2 bytes)
      packet[12] = (byte)(P ? 0x04 : 0x00);
//      packet[13] = 0x00;
      System.arraycopy(data, offset, packet, 12 + 2, packetLength);
      offset += packetLength;
      len -= packetLength;
      packets.add(packet);
    }
    timestamp += 100;  //??? 10 fps ???
    return packets.toArray(new byte[0][0]);
  }

  /**
   * Returns last full packet.
   */
  public byte[] decode(byte rtp[]) {
    if (rtp.length < 12 + 2) return null;  //bad packet
    if (partial == null) {
      partial = new byte[0];
    }
    int h263Length = rtp.length - 12 - 2;
    boolean P = (rtp[12] & 0x04) == 0x04;
    int partialLength = partial.length;
    partial = Arrays.copyOf(partial, partial.length + (P ? 2 : 0) + h263Length);
    //if P is true a 0,0 is left between last packet and this packet
    System.arraycopy(rtp, 12 + 2, partial, partialLength + (P ? 2 : 0), h263Length);
    if ((rtp[1] & 0x80) == 0x80) {  //RTP.M flag
      byte ret[] = partial;
      partial = null;
      return ret;
    }
    return null;
  }

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 2(rtp_h263_header) = 1444 bytes payload per packet
  private static final int mtu = 1444;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private byte partial[];
}
