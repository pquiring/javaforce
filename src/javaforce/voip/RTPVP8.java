package javaforce.voip;

/**
 * Encodes/Decodes RTP/VP8 packets
 *
 * http://tools.ietf.org/html/rfc7741
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPVP8 extends RTPCodec {

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 1 (VP8) = 1445 bytes payload per packet
  private static final int mtu = 1445;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private int lastseqnum = -1;
  private Packet packet;
  private static int maxSize = 4 * 1024 * 1024;

  //RTP bits
  private static final int M = 0x80;  //M bit

  public RTPVP8() {
    ssrc = random.nextInt();
    packet = new Packet();
    packet.data = new byte[maxSize];
  }

  /** Encodes raw VP8 data into multiple RTP packets. */
  public void encode(byte[] data, int x, int y, int id, PacketReceiver pr) {
    int len = data.length;
    int packetLength;
    int offset = 0;
    boolean first = true;
    while (len > 0) {
      if (len > mtu) {
        packetLength = mtu;
      } else {
        packetLength = len;
      }
      packet.length = packetLength + 1 + 12;  //1=VP8 header, 12=RTP.length
      RTPChannel.buildHeader(packet.data, id, seqnum++, timestamp, ssrc, len == packetLength);
      if (first) {
        packet.data[12] = (byte)(0x10);  //X R N S PartID(4)
        first = false;
      }
      System.arraycopy(data, offset, packet, 13, packetLength);
      pr.onPacket(packet);
      offset += packetLength;
      len -= packetLength;
    }
    packet.length = 0;
    timestamp += 100;  //??? 10 fps ???
  }

  /**
   * Returns last full packet.
   */
  public void decode(byte[] rtp, int offset, int length, PacketReceiver pr) {
    if (rtp.length < 12 + 2) return;  //bad packet
    int vp8Length = rtp.length - 12;
    int payloadOffset = 12;
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
    int partialLength = packet.length;
    System.arraycopy(rtp, payloadOffset, packet.data, partialLength, vp8Length);

    int thisseqnum = RTPChannel.getseqnum(rtp, 0);
    if (lastseqnum != -1 && thisseqnum != lastseqnum + 1) {
      JFLog.log("VP8:Received packet out of order, discarding frame.");
      lastseqnum = -1;
      packet.length = 0;
      return;
    }
    lastseqnum = thisseqnum;
    if ((rtp[1] & M) == M) {  //check RTP.M flag
      pr.onPacket(packet);
      lastseqnum = -1;
      packet.length = 0;
    }
  }
}
