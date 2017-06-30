package javaforce.voip;

/**
 * Encodes/Decodes RTP/H264 packets
 *
 * http://tools.ietf.org/html/rfc3984
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPH264 {

  public RTPH264() {
    ssrc = new Random().nextInt();
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
   * NAL Header : F(1) NRI(2) TYPE(5) : F=0 NRI=0-3 TYPE=1-23:full_packet 28=FU-A
   * FUA Header : S(1) E(1) R(1) TYPE(5) : S=start E=end R=reserved TYPE=???
   */

  /** Encodes raw H.264 data into multiple RTP packets. */
  public byte[][] encode(byte data[], int x, int y, int id) {
    ArrayList<byte[]> packets = new ArrayList<byte[]>();
    int len = data.length;
    int packetLength;
    int offset = 0;
    byte packet[];
    while (len > 0) {
      //skip 0,0,1
      while (data[offset] == 0) {offset++; len--;}
      offset++; len--;  //skip 1
      if (len > mtu) {
        packetLength = find_best_length(data, offset, len);
      } else {
        packetLength = len;
      }
      if (packetLength > mtu) {
        //need to split up into Frag Units (mode A)
        int nalLength = mtu - 2;
        byte type = (byte)(data[offset] & 0x1f);
        byte nri = (byte)(data[offset] & 0x60);
        offset++;
        len--;
        packetLength--;
        boolean first = true;
        while (packetLength > nalLength) {
          packet = new byte[12 + 2 + nalLength];
          RTPChannel.buildHeader(packet, id, seqnum++, timestamp, ssrc, false);
          packet[12] = 28;  //FU-A
          packet[12] |= nri;
          packet[13] = type;
          if (first) {
            packet[13] |= 0x80;  //first FU packet
            first = false;
          }
          System.arraycopy(data, offset, packet, 14, nalLength);
          offset += nalLength;
          len -= nalLength;
          packetLength -= nalLength;
          packets.add(packet);
        }
        //add last NAL packet
        nalLength = packetLength;
        packet = new byte[12 + 2 + nalLength];
        RTPChannel.buildHeader(packet, id, seqnum++, timestamp, ssrc, len == nalLength);
        packet[12] = 28;  //F=0 TYPE=28 (FU-A)
        packet[12] |= nri;
        packet[13] = type;
        packet[13] |= 0x40;  //last FU packet
        System.arraycopy(data, offset, packet, 14, nalLength);
        offset += nalLength;
        len -= nalLength;
        packetLength -= nalLength;
        packets.add(packet);
      } else {
        packet = new byte[packetLength + 12];  //12=RTP.length
        RTPChannel.buildHeader(packet, id, seqnum++, timestamp, ssrc, len == packetLength);
        System.arraycopy(data, offset, packet, 12, packetLength);
        packets.add(packet);
        offset += packetLength;
        len -= packetLength;
      }
    }
    timestamp += 100;  //??? 10 fps ???
    return packets.toArray(new byte[0][0]);
  }

  /**
   * Returns last full packet.
   */
  public byte[] decode(byte rtp[]) {
    if (rtp.length < 12 + 2) return null;  //bad packet
    int h264Length = rtp.length - 12;
    int type = rtp[12] & 0x1f;
    if (partial == null) {
      partial = new byte[0];
    }
    if (type >= 1 && type <= 23) {
      //a full packet
      int partialLength = partial.length;
      partial = Arrays.copyOf(partial, partial.length + 4 + h264Length);
      partial[partialLength + 3] = 1;  //0,0,0,1
      System.arraycopy(rtp, 12, partial, partialLength + 4, h264Length);
    } else if (type == 28) {
      //FU-A Packet
      if ((rtp[13] & 0x80) == 0x80) {
        //first NAL packet (restore first byte)
        int nri = rtp[12] & 0x60;
        type = rtp[13] & 0x1f;
        partial = Arrays.copyOf(partial, partial.length + 5);
        partial[partial.length-2] = 1;  //0,0,0,1
        partial[partial.length-1] = (byte)(nri + type);  //NRI TYPE (first byte)
        lastseqnum = RTPChannel.getseqnum(rtp, 0);
      } else {
        int thisseqnum = RTPChannel.getseqnum(rtp, 0);
        if (thisseqnum != lastseqnum + 1) {
          JFLog.log("H264:Received FU-A packet out of order, discarding frame.");
          partial = null;
          lastseqnum = -1;
          return null;
        }
        lastseqnum = thisseqnum;
      }
      if ((rtp[13] & 0x40) == 0x40) {
        //last NAL packet
        lastseqnum = -1;
      }
      int partialLength = partial.length;
      h264Length -= 2;
      partial = Arrays.copyOf(partial, partial.length + h264Length);
      System.arraycopy(rtp, 12+2, partial, partialLength, h264Length);
    } else {
      JFLog.log("H264:Unsupported packet type:" + type);
      partial = null;
      lastseqnum = -1;
      return null;
    }
    if ((rtp[1] & 0x80) == 0x80) {  //check RTP.M flag
      byte full[] = partial;
      partial = null;
      lastseqnum = -1;
      return full;
    }
    return null;
  }

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) = 1446 bytes payload per packet
  private static final int mtu = 1446;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private byte partial[];
  private int lastseqnum = -1;
}

/*
 Type Name
    0 [invalid]
    1 Coded slice
    2 Data Partition A
    3 Data Partition B
    4 Data Partition C
    5 IDR (Instantaneous Decoding Refresh) Picture
    6 SEI (Supplemental Enhancement Information)
    7 SPS (Sequence Parameter Set)
    8 PPS (Picture Parameter Set)
    9 Access Unit Delimiter
   10 EoS (End of Sequence)
   11 EoS (End of Stream)
   12 Filter Data
13-23 [extended]
24-31 [unspecified]
*/