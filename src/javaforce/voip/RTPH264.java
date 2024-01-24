package javaforce.voip;

/**
 * Encodes/Decodes RTP/H264 packets
 *
 * RTP packets are "fragments" of H264 packets.
 *
 * http://tools.ietf.org/html/rfc3984
 * http://tools.ietf.org/html/rfc6184
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPH264 extends RTPCodec {

  public static int decodeSize = 4 * 1024 * 1024;
  public int log;

  private static final int FU = 28;

  public RTPH264() {
    ssrc = random.nextInt();
    packet = new Packet();
    packet.data = new byte[decodeSize];
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
   * NAL Header : F(1) NRI(2) TYPE(5) : F=0 NRI=0-3 TYPE=1-23:full_packet 28=FU-A
   * FUA Header : S(1) E(1) R(1) TYPE(5) : S=start E=end R=reserved TYPE=1-23
   */

  /** Encodes raw H.264 packets into multiple RTP packets (fragments). */
  public byte[][] encode(byte data[], int x, int y, int id) {
    ArrayList<byte[]> packets = new ArrayList<byte[]>();
    int len = data.length;
    int packetLength;
    int offset = 0;
    byte packet[];
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
          packet[12] = FU;  //FU-A
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
        packet[12] = FU;  //F=0 TYPE=28 (FU-A)
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
   * Combines RTP fragments into full H264 packets.
   */
  public Packet decode(byte[] rtp, int offset, int length) {
    //assumes offset == 0
    if (length < 12 + 2) return null;  //bad packet
    if (reset_packet) {
      packet.length = 0;
      reset_packet = false;
    }
    int h264Length = length - 12;
    int type = rtp[12] & 0x1f;
    int thisseqnum = RTPChannel.getseqnum(rtp, 0);
    if (type >= 1 && type <= 23) {
      //a full NAL packet
      System.arraycopy(rtp, 12, packet.data, 4, h264Length);
      packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
      packet.length = 4 + h264Length;
      reset_packet = true;
      return packet;
    } else if (type == FU) {
      //FU-A Packet
      boolean first = (rtp[13] & 0x80) == 0x80;
      boolean last = (rtp[13] & 0x40) == 0x40;
      int realtype = rtp[13] & 0x1f;
      boolean M = (rtp[12] & 0x80) == 0x80;  //ERROR : this should be rtp[1]
      if (M && !last) {
        JFLog.log(log, "Error : H264 : FU-A : M bit set but not last packet : seq=" + thisseqnum);
        return null;
      }
      if (first) {
        if (packet.length != 0) {
          JFLog.log(log, "Warning : H264 : FU-A : first packet again, last frame lost?");
        }
        int nri = rtp[12] & 0x60;
        h264Length -= 2;
        System.arraycopy(rtp, 12 + 2, packet.data, 4 + 1, h264Length);
        packet.length = 4 + 1 + h264Length;
        packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
        packet.data[4] = (byte)(nri + realtype);
        lastseqnum = thisseqnum;
      } else {
        if (packet.length == 0) {
          JFLog.log(log, "Error : H264 : partial packet received before first packet : seq=" + thisseqnum);
          return null;
        }
        if (thisseqnum != nextseqnum()) {
          JFLog.log(log, "Error : H264 : Received FU-A packet out of order, discarding frame : seq=" + thisseqnum);
          packet.length = 0;
          lastseqnum = -1;
          return null;
        }
        lastseqnum = thisseqnum;
        int partialLength = packet.length;
        h264Length -= 2;
        System.arraycopy(rtp, 12+2, packet.data, partialLength, h264Length);
        packet.length += h264Length;
        if (last) {
          reset_packet = true;
          return packet;
        }
      }
    } else {
      JFLog.log(log, "H264:Unsupported packet type:" + type);
      packet.length = 0;
      lastseqnum = -1;
      return null;
    }
    return null;
  }

  private int nextseqnum() {
    if (lastseqnum == 65535) return 0;
    return lastseqnum + 1;
  }

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) = 1446 bytes payload per packet
  private static final int mtu = 1446;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private Packet packet;
  private boolean reset_packet;
  private int lastseqnum = -1;
}

/*
 Type Name
    0 [invalid]
    1 Coded slice (incremental frame)
    2 Data Partition A
    3 Data Partition B
    4 Data Partition C
    5 IDR (Instantaneous Decoding Refresh) Picture (key frame)
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
