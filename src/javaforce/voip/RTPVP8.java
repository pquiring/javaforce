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

public class RTPVP8 extends RTPVideoCoder {

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
  public void encode(byte[] data, int offset, int length, int x, int y, int id, PacketReceiver pr) {
    int len = length;
    int packetLength;
    int pos = offset;
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
      System.arraycopy(data, pos, packet, 13, packetLength);
      pr.onPacket(packet);
      pos += packetLength;
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
    byte bits = rtp[12];  //X R N S R PartitionIndex(3)
    boolean x = (bits & 0x80) == 0x80;  //extended bits
    boolean n = (bits & 0x20) == 0x20;  //non-ref frame (can be discarded)
    boolean s = (bits & 0x10) == 0x10;  //start
    if (s) {
      packet.length = 0;
    } else {
      payloadOffset++;
      vp8Length--;
      if (x) {
        byte iltk = rtp[13];  //I L T K RSV(3)
        payloadOffset++;
        vp8Length--;
        if ((iltk & 0x80) == 0x80) {  //Picture ID
          byte pid = rtp[14];
          if ((pid & 0x80) == 0x80) {
            //15 bit PID
            payloadOffset++;
            vp8Length--;
          }
          payloadOffset++;
          vp8Length--;
        }
        if ((iltk & 0x40) == 0x40) {  //TL0PICIDX
          payloadOffset++;
          vp8Length--;
        }
        if ((iltk & 0x30) != 0x00) {  //TID RSV-B
          payloadOffset++;
          vp8Length--;
        }
      }
    }
    if (n) return;

    //copy to packet
    System.arraycopy(rtp, payloadOffset, packet.data, packet.length, vp8Length);

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
