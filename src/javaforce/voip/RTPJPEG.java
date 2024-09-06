package javaforce.voip;

/**
 * Encodes/Decodes RTP/JPEG packets (type 26).
 *
 * http://tools.ietf.org/rfc/rfc2435.txt
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPJPEG extends RTPCodec {

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 8(rtp_jpeg_header) = 1438 bytes payload per packet
  private static final int mtu = 1438;
  private int seqnum;
  private int timestamp;
  private int ssrc;
  private Packet packet;
  public static final int maxSize = 4 * 1024 * 1024;

  //RTP bits
  private static final int M = 0x80;  //M bit

  public RTPJPEG() {
    ssrc = random.nextInt();
    packet = new Packet();
    packet.data = new byte[maxSize];
  }

  /** Encodes a raw JPEG data into multiple RTP packets. */
  public void encode(byte[] jpeg, int offset, int length, int x, int y, int id, PacketReceiver pr) {
    int cnt = (length + mtu - 1) / mtu;
    int len = length;
    int packetLength;
    int pos = offset;
    for(int a=0;a<cnt;a++) {
      if (len > mtu) packetLength = mtu; else packetLength = len;
      packet.length = packetLength + 12 + 8;  //12=RTP.length 8=rtp_jpeg_header.length
      RTPChannel.buildHeader(packet.data, RTP.CODEC_JPEG.id, seqnum, timestamp, ssrc, a == cnt-1);
      buildHeader(packet.data, x, y, pos);
      System.arraycopy(jpeg, pos, packet.data, 12 + 8, packetLength);
      pos += packetLength;
      len -= packetLength;
      if (a == cnt-1) {
        packet.data[1] |= M;  //mark last packet (marker)
      }
      pr.onPacket(packet);
    }
    seqnum++;
    packet.length = 0;
    timestamp += 100;  //??? 10 fps ???
  }

  public void decode(byte[] rtp, int offset, int length, PacketReceiver pr) {
    if (rtp.length < 12 + 8) return;  //bad packet
    int off = ((int)rtp[13]) & 0xff;
    off <<= 8;
    off += ((int)rtp[14]) & 0xff;
    off <<= 8;
    off += ((int)rtp[15]) & 0xff;
    if (off != packet.length) {
      //lost a packet
      JFLog.log("RTPJPEG:decode:lost a packet or not in order");
      packet.length = 0;
      return;
    }
    int jpegLength = rtp.length - 12 - 8;
    int partialLength = packet.length;
    System.arraycopy(rtp, 12+8, packet.data, partialLength, jpegLength);
    if ((rtp[1] & M) == M) {  //check for marker
      pr.onPacket(packet);
      packet.length = 0;
    }
  }

  private static void buildHeader(byte[] data, int x, int y, int offset) {
    data[12] = 0;  //type-specfic ???
    data[13] = (byte) ((offset & 0xff0000) >> 16);
    data[14] = (byte) ((offset & 0xff00) >> 8);
    data[15] = (byte) (offset & 0xff);
    data[16] = 0;  //type ??
    data[17] = 0;  //Q ??
    data[18] = (byte)(x/8);  //width
    data[19] = (byte)(y/8);  //height
  }
}
