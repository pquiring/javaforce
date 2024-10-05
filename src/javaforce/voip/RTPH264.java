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

import javaforce.*;
import javaforce.media.*;

public class RTPH264 implements RTPVideoCoder {

  private static boolean debug = false;

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) = 1446 bytes payload per packet
  private static final int mtu = 1446;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private Packet packet;
  private int lastseqnum = -1;
  private static final int maxSize = 4 * 1024 * 1024;
  private int log;

  private static final int FU = 28;

  //FU bits
  private static final int S = 0x80;  //start or first
  private static final int E = 0x40;  //last or end

  //RTP bits
  private static final int M = 0x80;  //M bit

  public RTPH264() {
    ssrc = new java.util.Random().nextInt();
    packet = new Packet();
    packet.data = new byte[maxSize];
  }

  private int rtp_id;
  public void setid(int id) {
    rtp_id = id;
  }

  public void setLog(int id) {
    log = id;
  }

  private int find_best_length(byte[] data, int offset, int length) {
    //see if there is a 0,0,1 and return a length to that
    //this way the next packet will start at a resync point
    for(int a=1;a<length-3;a++) {
      if (data[offset + a] == 0 && data[offset + a + 1] == 0 && data[offset + a + 2] == 1) return a;
    }
    return length;
  }

  /*
   * NAL Header : 8 bits : F(1) NRI(2) TYPE(5) : F=0 NRI=0-3 TYPE=1-23:full_packet 28=FU
   * FUA Header : 8 bits : S(1) E(1) R(1) TYPE(5) : S=start E=end R=reserved TYPE=1-23
   */

  /** Encodes raw H.264 packets into multiple RTP packets (fragments). */
  public void encode(byte[] data, int offset, int length, int x, int y, PacketReceiver pr) {
    int len = length;
    int packetLength;
    int pos = offset;
    while (len > 0) {
      //skip 0,0,0,1
      while (data[pos] == 0) {pos++; len--;}
      pos++; len--;  //skip 1
      if (len > mtu) {
        packetLength = find_best_length(data, pos, len);
      } else {
        packetLength = len;
      }
      if (packetLength > mtu) {
        //need to split up into Frag Units (mode A)
        int nalLength = mtu - 2;
        byte type = get_nal_type(data, pos);
        byte nri = (byte)(data[pos] & 0x60);
        pos++;
        len--;
        packetLength--;
        boolean first = true;
        while (packetLength > nalLength) {
          packet.length = 12 + 2 + nalLength;
          RTPChannel.buildHeader(packet.data, rtp_id, seqnum++, timestamp, ssrc, false);
          packet.data[12] = FU;
          packet.data[12] |= nri;
          packet.data[13] = type;
          if (first) {
            packet.data[13] |= S;  //first FU packet
            first = false;
          }
          System.arraycopy(data, pos, packet.data, 14, nalLength);
          pos += nalLength;
          len -= nalLength;
          packetLength -= nalLength;
          pr.onPacket(packet);
        }
        //add last NAL packet
        nalLength = packetLength;
        packet.length = 12 + 2 + nalLength;
        RTPChannel.buildHeader(packet.data, rtp_id, seqnum++, timestamp, ssrc, len == nalLength);
        packet.data[12] = FU;
        packet.data[12] |= nri;
        packet.data[13] = type;
        packet.data[13] |= E;  //last FU packet
        System.arraycopy(data, pos, packet.data, 14, nalLength);
        pos += nalLength;
        len -= nalLength;
        packetLength -= nalLength;
        pr.onPacket(packet);
      } else {
        //full NAL packet
        packet.length = packetLength + 12;  //12=RTP.length
        RTPChannel.buildHeader(packet.data, rtp_id, seqnum++, timestamp, ssrc, len == packetLength);
        System.arraycopy(data, pos, packet.data, 12, packetLength);
        pr.onPacket(packet);
        pos += packetLength;
        len -= packetLength;
      }
    }
    timestamp += 100;  //??? 10 fps ???
  }

  /**
   * Combines RTP fragments into full H264 packets.
   */
  public void decode(byte[] rtp, int offset, int length, PacketReceiver pr) {
    //assumes offset == 0
    if (length < 12 + 2) return;  //bad packet
    int h264Length = length - 12;
    byte type = get_nal_type(rtp, 12);
    int thisseqnum = RTPChannel.getseqnum(rtp, 0);
    if (type >= 1 && type <= 23) {
      //a full NAL packet
      System.arraycopy(rtp, 12, packet.data, 4, h264Length);
      packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
      packet.length = 4 + h264Length;
      if (debug) log(packet);
      pr.onPacket(packet);
      lastseqnum = thisseqnum;
      packet.length = 0;
    } else if (type == FU) {
      //FU header bits
      boolean first = (rtp[13] & S) == S;
      boolean last = (rtp[13] & E) == E;
      byte fu_type = get_fu_type(rtp, 13);
      //RTP header bits
      boolean m = (rtp[1] & M) == M;
      if (m && !last) {
        JFLog.log(log, "Error : H264 : FU : M bit set but not last packet : seq=" + thisseqnum);
        lastseqnum = -1;
        packet.length = 0;
        return;
      }
      if (first) {
        if (packet.length != 0) {
          JFLog.log(log, "Warning : H264 : FU : first packet again, last frame lost?");
        }
        int nri = rtp[12] & 0x60;
        h264Length -= 2;
        System.arraycopy(rtp, 12 + 2, packet.data, 4 + 1, h264Length);
        packet.length = 4 + 1 + h264Length;
        packet.data[3] = 0x01;  //start code = 0x00 0x00 0x00 0x01
        //NAL header (8 bits)
        packet.data[4] = (byte)(nri + fu_type);

        lastseqnum = thisseqnum;
      } else {
        if (packet.length == 0) {
          JFLog.log(log, "Error : H264 : partial packet received before first packet : seq=" + thisseqnum);
          lastseqnum = -1;
          packet.length = 0;
          return;
        }
        if (thisseqnum != nextseqnum()) {
          JFLog.log(log, "Error : H264 : Received FU packet out of order, discarding frame : seq=" + thisseqnum);
          lastseqnum = -1;
          packet.length = 0;
          return;
        }
        lastseqnum = thisseqnum;
        h264Length -= 2;
        System.arraycopy(rtp, 12 + 2, packet.data, packet.length, h264Length);
        packet.length += h264Length;
        if (last) {
          if (debug) log(packet);
          pr.onPacket(packet);
          packet.length = 0;
        }
      }
    } else {
      JFLog.log(log, "H264:Unsupported packet type:" + type);
      lastseqnum = -1;
      packet.length = 0;
    }
  }

  private int nextseqnum() {
    if (lastseqnum == 65535) return 0;
    return lastseqnum + 1;
  }

  private static byte get_fu_type(byte[] rtp, int offset) {
    return (byte)(rtp[offset] & 0x1f);
  }

  public static byte get_nal_type(byte[] packet, int offset) {
    return (byte)(packet[offset] & 0x1f);
  }


  public boolean isKeyFrame(byte type) {
    return type == 5;
  }

  public boolean isIFrame(byte type) {
    return type == 1;
  }

  public boolean isFrame(byte type) {
    return type == 5 || type == 1;
  }

  public boolean isStart(byte type) {
    return type == 7;
  }

  public static boolean canDecodePacket(byte type) {
    switch (type) {
      case 7:  //SPS
      case 8:  //PPS
      case 1:  //i frame
      case 5:  //key frame
        return true;
      default:
        return false;  //all others ignore
    }
  }

  public static CodecInfo getCodecInfo(Packet sps) {
    MediaVideoDecoder decoder = new MediaVideoDecoder();
    decoder.start(MediaCoder.AV_CODEC_ID_H264, 320, 200);
    decoder.decode(sps.data, sps.offset, sps.length);  //ignore return
    CodecInfo info = new CodecInfo();
    info.width = decoder.getWidth();
    info.height = decoder.getHeight();
    info.fps = decoder.getFrameRate();
    decoder.stop();
    return info;
  }

  private void log(Packet packet) {
    //0x00 0x00 0x00 0x01 NAL[8] PIC[8]
    JFLog.log(String.format("H264:%x,%x", packet.data[4], packet.data[5]));
  }
}

/*
https://github.com/GStreamer/gstreamer/blob/main/subprojects/gst-plugins-bad/gst-libs/gst/codecparsers/gsth264parser.h

 Type Name
    0 [invalid]
 1-23 NAL packets
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
----- rtp types -----
24-27 [unspecified]
   28 FU
29-31 [unspecified]

Typical sequence : 7 8 5 1...

*/
