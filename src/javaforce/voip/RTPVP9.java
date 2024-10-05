package javaforce.voip;

/**
 * Encodes/Decodes RTP/VP9 packets
 *
 * https://www.ietf.org/archive/id/draft-ietf-payload-vp9-00.txt
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPVP9 extends RTPVideoCoder {

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 1 (VP9) = 1445 bytes payload per packet
  private static final int mtu = 1445;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private int lastseqnum = -1;
  private Packet packet;
  private static int maxSize = 4 * 1024 * 1024;

  //RTP bits
  private static final int M = 0x80;  //M bit

  //VP9 bits : I|P|L|F|B|E|V|-
  private static final int I = 0x80;  //pic ID present
  private static final int P = 0x40;  //Inter-picture predicted layer frame
  private static final int L = 0x20;  //Layer indices present
  private static final int F = 0x10;  //flex mode
  private static final int B = 0x08;  //first
  private static final int E = 0x04;  //last
  private static final int V = 0x02;  //Scalability structure present

  public RTPVP9() {
    ssrc = random.nextInt();
    packet = new Packet();
    packet.data = new byte[maxSize];
  }

  /** Encodes raw VP9 packet into multiple RTP packets. */
  public void encode(byte[] data, int offset, int length, int x, int y, int id, PacketReceiver pr) {
    int len = length;
    int packetLength;
    int pos = offset;
    boolean first = true;
    boolean last;
    while (len > 0) {
      if (len > mtu) {
        packetLength = mtu;
      } else {
        packetLength = len;
      }
      packet.length = packetLength + 1 + 12;  //1=VP9 header, 12=RTP.length
      last = len == packetLength;
      RTPChannel.buildHeader(packet.data, id, seqnum++, timestamp, ssrc, last);
      if (first) {
        packet.data[12] = (byte)(data[0] | B);
        first = false;
      } else {
        packet.data[12] = 0;
      }
      if (last) {
        packet.data[12] = E;
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
    int vp9Length = rtp.length - 12;
    int payloadOffset = 12;
    byte bits = rtp[12];  //I|P|L|F|B|E|V|-
    boolean f = (bits & F) != 0x0;  //f
    boolean i = (bits & I) != 0x0;  //i
    boolean p = (bits & P) != 0x0;  //p
    boolean l = (bits & L) != 0x0;  //l
    boolean b = (bits & B) != 0x0;  //first
    boolean e = (bits & E) != 0x0;  //last
    boolean v = (bits & V) != 0x0;  //v

    if (b) {
      packet.length = 0;
    } else {
      payloadOffset++;
      vp9Length--;
      if (i) {
        boolean m = (rtp[payloadOffset] & 0x80) != 0;
        payloadOffset++;
        vp9Length--;
        if (m) {
          payloadOffset++;
          vp9Length--;
        }
      }
      if (l) {
        payloadOffset++;
        vp9Length--;
      }
      if (p || f) {
        boolean x = (rtp[payloadOffset] & 0x02) != 0;
        payloadOffset++;
        vp9Length--;
        if (x) {
          payloadOffset++;
          vp9Length--;
        }
      }
      if (!f) {
        //TL0PICIDX
        payloadOffset++;
        vp9Length--;
      }
      if (v) {
        byte ss = rtp[payloadOffset];
        payloadOffset++;
        vp9Length--;
        int n_s = (ss & 0xe0) + 1;
        boolean y = (ss & 0x10) != 0;
        int n_g = (ss & 0x0f) + 1;
        if (y) {
          int dims = n_s * 4;
          payloadOffset += dims;
          vp9Length -= dims;
        }
        for(int i1=0;i1<n_g;i1++) {
          byte tur = rtp[payloadOffset];  //T(3) U R(2) - -
          payloadOffset++;
          vp9Length--;
          int r = (tur & 0x0c) >> 2;
          payloadOffset += r;
          vp9Length -= r;
        }
      }
    }

    //copy to packet
    System.arraycopy(rtp, payloadOffset, packet.data, packet.length, vp9Length);

    int thisseqnum = RTPChannel.getseqnum(rtp, 0);
    if (lastseqnum != -1 && thisseqnum != lastseqnum + 1) {
      JFLog.log("VP9:Received packet out of order, discarding frame.");
      lastseqnum = -1;
      packet.length = 0;
      return;
    }
    lastseqnum = thisseqnum;
    if ((rtp[1] & M) == M) {  //check RTP.M flag
      if (!e) {
        JFLog.log("Error : VP9 : E bit not set with M bit");
      }
      pr.onPacket(packet);
      lastseqnum = -1;
      packet.length = 0;
    }
  }
}
