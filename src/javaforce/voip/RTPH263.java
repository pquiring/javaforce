package javaforce.voip;

/**
 * Encodes/Decodes RTP/H263 packets
 *
 * Payload type = 34
 *
 * http://tools.ietf.org/html/rfc2190
 *
 * NOTE : This file is INCOMPLETE!!!
 *   RFC2190 is too complex for me to understand right now.
 *   How do you find MB boundaries???
 *   See ff_rtp_send_h263_rfc2190() in libavformat/rtpenc_h263_rfc2190.c
 *   FFMPEG's passes mb boundary info from encoder directly to function.
 *   MB = macroblocks
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class RTPH263 extends RTPCodec {

  public RTPH263() {
    ssrc = random.nextInt();
  }

  private int find_best_length(byte[] data, int offset, int length) {
    //see if there is a double zero and return a length to that
    //this way the next packet will start at a resync point
    for(int a=1;a<length-3;a++) {
      if (data[offset + a] == 0 && data[offset + a + 1] == 0 && data[offset + a + 2] != 0) return a;
    }
    return length;
  }

  /** Encodes raw H.263 data into multiple RTP packets. */
  public void encode(byte[] data, int x, int y, int id, PacketReceiver pr) {
    //TODO
  }

  /**
   * Returns last full packet.
   */
  public void decode(byte[] rtp, int offset, int length, PacketReceiver pr) {
    //TODO
  }

  //mtu = 1500 - 14(ethernet) - 20(ip) - 8(udp) - 12(rtp) - 4(rtp_h263_header) = 1442 bytes payload per packet
  private static final int mtu = 1442;
  private int seqnum;
  private int timestamp;
  private final int ssrc;
  private byte[] partial;
}
