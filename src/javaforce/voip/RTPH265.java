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

  public static int decodeSize = 4 * 1024 * 1024;
  public int log;

  private static final int AP = 48;
  private static final int FU = 49;
  private static final int PACI = 50;

  private static final int LAYER_TID = 1;  //value TID (must not be zero)

  public RTPH265() {
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
   * NAL Header : F(1) TYPE(6) LAYER(6) TID(3) : F=0 TYPE=1-63 LAYER=0 TID=1-7 (2 bytes)
   * FUA Header : S(1) E(1) TYPE(6) : S=start E=end TYPE=1-63
   */

  /** Encodes raw H.265 packets into multiple RTP packets (fragments). */
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
        //need to split up into Frag Units
        int nalLength = mtu - 2;
        byte type = (byte)((data[offset] & 0x7e) >> 1);
        offset++;
        len--;
        packetLength--;
        boolean first = true;
        while (packetLength > nalLength) {
          packet = new byte[12 + 3 + nalLength];
          RTPChannel.buildHeader(packet, id, seqnum++, timestamp, ssrc, false);
          packet[12] = FU << 1;  //FU
          packet[13] = LAYER_TID;
          packet[14] = type;
          if (first) {
            packet[14] |= 0x80;  //first FU packet
            first = false;
          }
          System.arraycopy(data, offset, packet, 15, nalLength);
          offset += nalLength;
          len -= nalLength;
          packetLength -= nalLength;
          packets.add(packet);
        }
        //add last NAL packet
        nalLength = packetLength;
        packet = new byte[12 + 3 + nalLength];
        RTPChannel.buildHeader(packet, id, seqnum++, timestamp, ssrc, len == nalLength);
        packet[12] = FU << 1;  //FU
        packet[13] = LAYER_TID;
        packet[14] = type;
        packet[14] |= 0x40;  //last FU packet
        System.arraycopy(data, offset, packet, 15, nalLength);
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
   * Combines RTP fragments into full H265 packets.
   */
  public Packet decode(byte[] rtp, int offset, int length) {
    //assumes offset == 0
    if (length < 12 + 3) return null;  //bad packet
    if (reset_packet) {
      packet.length = 0;
      reset_packet = false;
    }
    int h265Length = length - 12;
    int type = (rtp[12] & 0x7e) >> 1;
    int thisseqnum = RTPChannel.getseqnum(rtp, 0);
    if (type > 0 && type < FU) {
      //a full NAL packet
      System.arraycopy(rtp, 12, packet.data, 4, h265Length);
      packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
      packet.length = 4 + h265Length;
      reset_packet = true;
      return packet;
    } else if (type == FU) {
      //FU Packet
      boolean first = (rtp[14] & 0x80) == 0x80;
      boolean last = (rtp[14] & 0x40) == 0x40;
      byte realtype = (byte)(rtp[14] & 0x3f);
      boolean M = (rtp[12] & 0x80) == 0x80;  //ERROR : this should be rtp[1]
      if (M && !last) {
        JFLog.log(log, "Error : H265 : FU : M bit set but not last packet : seq=" + thisseqnum);
        return null;
      }
      if (first) {
        if (packet.length != 0) {
          JFLog.log(log, "Warning : H265 : FU : first packet again, last frame lost?");
        }
        h265Length -= 3;
        System.arraycopy(rtp, 12 + 3, packet.data, 4 + 1, h265Length);
        packet.length = 4 + 1 + h265Length;
        packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
        packet.data[4] = realtype;
        lastseqnum = thisseqnum;
      } else {
        if (packet.length == 0) {
          JFLog.log(log, "Error : H265 : partial packet received before first packet : seq=" + thisseqnum);
          return null;
        }
        if (thisseqnum != nextseqnum()) {
          JFLog.log(log, "Error : H265 : Received FU-A packet out of order, discarding frame : seq=" + thisseqnum);
          packet.length = 0;
          lastseqnum = -1;
          return null;
        }
        lastseqnum = thisseqnum;
        int partialLength = packet.length;
        h265Length -= 3;
        System.arraycopy(rtp, 12+3, packet.data, partialLength, h265Length);
        packet.length += h265Length;
        if (last) {
          reset_packet = true;
          return packet;
        }
      }
    } else if (type == AP) {
      //AP Packet (2 or more full NAL units in one RTP packet)
      //TODO
    } else {
      JFLog.log(log, "H265:Unsupported packet type:" + type);
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
 1-47 NAL packets
   48 AP (aggregate NAL packets)
   49 FU (fragment NAL packet)
   50 PACI
50-63 reserved?
*/
