package javaforce.voip;

/**
 * Encodes/Decodes RTP/H265 (HEVC) packets
 *
 * http://tools.ietf.org/html/rfc7798
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPH265 extends RTPCodec {

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) = 1446 bytes payload per packet
  private static final int mtu = 1446;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private Packet packet;
  private int lastseqnum = -1;
  private static int maxSize = 4 * 1024 * 1024;
  private int log;

  private static final int AP = 48;
  private static final int FU = 49;
  private static final int PACI = 50;

  //FU bits
  private static final int S = 0x80;  //start or first
  private static final int E = 0x40;  //last or end

  //RTP bits
  private static final int M = 0x80;  //M bit

  private static final int LAYER_TID = 1;  //LAYER | TID (must not be zero)

  public RTPH265() {
    ssrc = random.nextInt();
    packet = new Packet();
    packet.data = new byte[maxSize];
  }

  public void setLog(int id) {
    log = id;
  }

  private int find_best_length(byte data[], int offset, int length) {
    //see if there is a 0,0,1 and return a length to that
    //this way the next packet will start at a resync point
    for(int a=1;a<length-3;a++) {
      if (data[offset + a] == 0 && data[offset + a + 1] == 0 && data[offset + a + 2] == 1) return a;
    }
    return length;
  }

  /*
   * NAL Header : 16 bits : F(1) TYPE(6) LAYER(6) TID(3) : F=0 TYPE=1-63 LAYER=0 TID=1-7 (2 bytes)
   * FUA Header :  8 bits : S(1) E(1) TYPE(6) : S=start E=end TYPE=1-63
   */

  /** Encodes raw H.265 packets into multiple RTP packets (fragments).
   *
   * TODO : Use AP packets to increase efficiency.
   */
  public void encode(byte data[], int x, int y, int id, PacketReceiver pr) {
    int len = data.length;
    int packetLength;
    int offset = 0;
    while (len > 0) {
      //skip 0,0,0,1
      while (data[offset] == 0) {offset++; len--;}
      offset++; len--;  //skip 1
      if (len > mtu) {
        packetLength = find_best_length(data, offset, len);
      } else {
        packetLength = len;
      }
      if (packetLength > mtu) {
        //need to split up into Frag Units
        int nalLength = mtu - 2;
        byte fu_type = (byte)((data[offset] & 0x7e) >> 1);  //shited into FU style
        byte layer_tid = data[offset + 1];
        offset++;
        len--;
        packetLength--;
        boolean first = true;
        while (packetLength > nalLength) {
          packet.length = 12 + 3 + nalLength;
          RTPChannel.buildHeader(packet.data, id, seqnum++, timestamp, ssrc, false);
          //NAL header (16 bit)
          packet.data[12] = FU << 1;
          packet.data[13] = LAYER_TID;
          //FU header (8bit)
          packet.data[14] = fu_type;
          if (first) {
            packet.data[14] |= S;  //first FU packet
            first = false;
          }
          //followed by real NAL unit
          System.arraycopy(data, offset, packet.data, 15, nalLength);
          offset += nalLength;
          len -= nalLength;
          packetLength -= nalLength;
          pr.onPacket(packet);
        }
        //add last NAL packet
        nalLength = packetLength;
        packet.length = 12 + 3 + nalLength;
        RTPChannel.buildHeader(packet.data, id, seqnum++, timestamp, ssrc, len == nalLength);
        //NAL header (16 bit)
        packet.data[12] = FU << 1;
        packet.data[13] = LAYER_TID;
        //FU header (8bit)
        packet.data[14] = fu_type;
        packet.data[14] |= E;
        //followed by real NAL unit
        System.arraycopy(data, offset, packet.data, 15, nalLength);
        offset += nalLength;
        len -= nalLength;
        packetLength -= nalLength;
        pr.onPacket(packet);
      } else {
        //full NAL packet "as is"
        packet.length = packetLength + 12;  //12=RTP.length
        RTPChannel.buildHeader(packet.data, id, seqnum++, timestamp, ssrc, len == packetLength);
        System.arraycopy(data, offset, packet.data, 12, packetLength);
        pr.onPacket(packet);
        offset += packetLength;
        len -= packetLength;
      }
    }
    timestamp += 100;  //??? 10 fps ???
  }

  /**
   * Combines RTP fragments into full H265 packets.
   */
  public void decode(byte[] rtp, int offset, int length, PacketReceiver pr) {
    //assumes offset == 0
    if (length < 12 + 3) return;  //bad packet
    int h265Length = length - 12;
    //NAL header bits (16bits) [12-13]
    int type = (rtp[12] & 0x7e) >> 1;
    //int layer = (rtp[13] >> 3);
    //int tid = (rtp[13] & 0x7);
    int thisseqnum = RTPChannel.getseqnum(rtp, 0);
    if (type > 0 && type < FU) {
      //a full NAL packet
      System.arraycopy(rtp, 12, packet.data, 4, h265Length);
      packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
      packet.length = 4 + h265Length;
      pr.onPacket(packet);
      lastseqnum = thisseqnum;
      packet.length = 0;
      return;
    } else if (type == FU) {
      //FU header bits [14]
      boolean first = (rtp[14] & S) == S;
      boolean last = (rtp[14] & E) == E;
      byte nal_type = (byte)((rtp[14] & 0x3f) << 1);
      //RTP header bits
      boolean m = (rtp[1] & M) == M;
      if (m && !last) {
        JFLog.log(log, "Error : H265 : FU : M bit set but not last packet : seq=" + thisseqnum);
        lastseqnum = -1;
        packet.length = 0;
        return;
      }
      if (first) {
        if (packet.length != 0) {
          JFLog.log(log, "Warning : H265 : FU : first packet again, last frame lost?");
        }
        h265Length -= 3;
        System.arraycopy(rtp, 12 + 3, packet.data, 4, h265Length);
        packet.length = 4 + h265Length;
        packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
        //NAL header (16 bits)
        packet.data[4] = nal_type;
        //[5] = layer + tid ???
        lastseqnum = thisseqnum;
      } else {
        if (packet.length == 0) {
          JFLog.log(log, "Error : H265 : partial packet received before first packet : seq=" + thisseqnum);
          lastseqnum = -1;
          packet.length = 0;
          return;
        }
        if (thisseqnum != nextseqnum()) {
          JFLog.log(log, "Error : H265 : Received FU-A packet out of order, discarding frame : seq=" + thisseqnum);
          lastseqnum = -1;
          packet.length = 0;
          return;
        }
        lastseqnum = thisseqnum;
        int partialLength = packet.length;
        h265Length -= 3;
        System.arraycopy(rtp, 12+3, packet.data, partialLength, h265Length);
        packet.length += h265Length;
        if (last) {
          pr.onPacket(packet);
          packet.length = 0;
        }
      }
    } else if (type == AP) {
      //AP Packet (2 or more full NAL units in one RTP packet)
      //each NAL unit is preceded by a 16bit size (network byte order)
      offset += 12 + 2;
      while (h265Length > 2) {
        int len = BE.getuint16(rtp, offset);
        if (len == 0) break;
        offset += 2;
        h265Length -= 2;
        packet.length = len;
        System.arraycopy(rtp, offset, packet.data, 0, len);
        pr.onPacket(packet);
        offset += len;
        h265Length -= len;
      }
      lastseqnum = thisseqnum;
      packet.length = 0;
    } else if (type == PACI) {
      //ignore packet?
      lastseqnum = thisseqnum;
      packet.length = 0;
    } else {
      JFLog.log(log, "H265:Unsupported packet type:" + type);
      lastseqnum = -1;
      packet.length = 0;
    }
  }

  private int nextseqnum() {
    if (lastseqnum == 65535) return 0;
    return lastseqnum + 1;
  }
}

/*
 Type Name
    0 [invalid]
 1-47 NAL packets
   48 AP (aggregate NAL packets)
   49 FU (fragment NAL packet)
   50 PACI
50-63 reserved?
*/
