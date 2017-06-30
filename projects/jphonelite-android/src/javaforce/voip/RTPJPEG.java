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

public class RTPJPEG {

  public RTPJPEG() {
    ssrc = new Random().nextInt();
  }

  /** Encodes a raw JPEG data into multiple RTP packets. */
  public byte[][] encode(byte jpeg[], int x, int y) {
    int cnt = (jpeg.length + mtu - 1) / mtu;
    int len = jpeg.length;
    byte packets[][] = new byte[cnt][];
    int packetLength;
    int offset = 0;
    for(int a=0;a<cnt;a++) {
      if (len > mtu) packetLength = mtu; else packetLength = len;
      packets[a] = new byte[packetLength + 12 + 8];  //12=RTP.length 8=rtp_jpeg_header.length
      RTPChannel.buildHeader(packets[a], RTP.CODEC_JPEG.id, seqnum, timestamp, ssrc, a == cnt-1);
      buildHeader(packets[a], x, y, offset);
      System.arraycopy(jpeg, offset, packets[a], 12 + 8, packetLength);
      offset += packetLength;
      len -= packetLength;
    }
    packets[cnt-1][1] |= 0x80;  //mark last packet (marker)
    seqnum++;
    timestamp += 100;  //??? 10 fps ???
    return packets;
  }

  public byte[] decode(byte rtp[]) {
    if (rtp.length < 12 + 8) return null;  //bad packet
    if (partial == null) partial = new byte[0];
    int offset = ((int)rtp[13]) & 0xff;
    offset <<= 8;
    offset += ((int)rtp[14]) & 0xff;
    offset <<= 8;
    offset += ((int)rtp[15]) & 0xff;
    if (offset != partial.length) {
      //lost a packet
      JFLog.log("RTPJPEG:decode:lost a packet or not in order");
      partial = null;
      return null;
    }
    int jpegLength = rtp.length - 12 - 8;
    int partialLength = partial.length;
    partial = Arrays.copyOf(partial, partial.length + jpegLength);
    System.arraycopy(rtp, 12+8, partial, partialLength, jpegLength);
    if ((rtp[1] & 0x80) == 0x80) {  //check for marker
      byte[] full = partial;
      partial = null;
      return full;
    }
    return null;
  }

  private static void buildHeader(byte data[], int x, int y, int offset) {
    data[12] = 0;  //type-specfic ???
    data[13] = (byte) ((offset & 0xff0000) >> 16);
    data[14] = (byte) ((offset & 0xff00) >> 8);
    data[15] = (byte) (offset & 0xff);
    data[16] = 0;  //type ??
    data[17] = 0;  //Q ??
    data[18] = (byte)(x/8);  //width
    data[19] = (byte)(y/8);  //height
  }

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 8(rtp_jpeg_header) = 1438 bytes payload per packet
  private static final int mtu = 1438;
  private int seqnum;
  private int timestamp;
  private int ssrc;
  private byte partial[];
}
