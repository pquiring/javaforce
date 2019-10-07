package javaforce.voip;

/**
 * Encodes/Decodes RTP/VP8 packets
 *
 * http://tools.ietf.org/html/draft-ietf-payload-vp8-01
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPVP8 extends RTPCodec {

  public RTPVP8() {
    ssrc = random.nextInt();
  }

  /** Encodes raw VP8 data into multiple RTP packets. */
  public byte[][] encode(byte data[], int x, int y, int id) {
    ArrayList<byte[]> packets = new ArrayList<byte[]>();
    int len = data.length;
    int packetLength;
    int offset = 0;
    byte packet[];
    while (len > 0) {
      if (len > mtu) {
        packetLength = mtu;
      } else {
        packetLength = len;
      }
      packet = new byte[packetLength + 1 + 12];  //1=VP8 header, 12=RTP.length
      RTPChannel.buildHeader(packet, id, seqnum++, timestamp, ssrc, len == packetLength);
      packet[12] = (byte)(packets.size() == 0 ? 0x10 : 0);  //X R N S PartID
      System.arraycopy(data, offset, packet, 13, packetLength);
      packets.add(packet);
      offset += packetLength;
      len -= packetLength;
    }
    timestamp += 100;  //??? 10 fps ???
    return packets.toArray(new byte[0][0]);
  }

  private Packet packet = new Packet();

  /**
   * Returns last full packet.
   */
  public Packet decode(byte rtp[], int offset, int length) {
    if (rtp.length < 12 + 2) return null;  //bad packet
    int vp8Length = rtp.length - 12;
    int payloadOffset = 12;
    if (partial == null) {
      partial = new byte[0];
    }
    byte x = rtp[12];  //X R N S PartID
    payloadOffset++;
    vp8Length--;
    if ((x & 0x80) == 0x80) {
      byte ilt = rtp[13];  //I L T RSV-A
      payloadOffset++;
      vp8Length--;
      if ((ilt & 0x80) == 0x80) {  //picture ID
        payloadOffset++;
        vp8Length--;
      }
      if ((ilt & 0x40) == 0x40) {  //TL0PICIDX
        payloadOffset++;
        vp8Length--;
      }
      if ((ilt & 0x20) == 0x20) {  //TID RSV-B
        payloadOffset++;
        vp8Length--;
      }
    }

    //copy to partial
    int partialLength = partial.length;
    partial = Arrays.copyOf(partial, partial.length + vp8Length);
    System.arraycopy(rtp, payloadOffset, partial, partialLength, vp8Length);

    int thisseqnum = RTPChannel.getseqnum(rtp, 0);
    if (lastseqnum != -1 && thisseqnum != lastseqnum + 1) {
      JFLog.log("VP8:Received packet out of order, discarding frame.");
      partial = null;
      lastseqnum = -1;
      return null;
    }
    lastseqnum = thisseqnum;
    if ((rtp[1] & 0x80) == 0x80) {  //check RTP.M flag
      packet.data = partial;
      partial = null;
      lastseqnum = -1;
      return packet;
    }
    return null;
  }

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 1 (VP8) = 1445 bytes payload per packet
  private static final int mtu = 1445;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private byte partial[];
  private int lastseqnum = -1;
}
