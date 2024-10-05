package javaforce.voip;

/** Codec Type.
 *
 * Not related to Codec ID found in SDP packets.
 *
 * @author pquiring
 */

public class CodecType {
  public static final int UNKNOWN = -1;
  public static final int RAW = 0;
  public static final int RTP = 1;
  public static final int RTCP = 2;
  public static final int G711u = 10;
  public static final int G711a = 11;
  public static final int GSM = 12;
  public static final int G729 = 13;
  public static final int SPEEX = 14;
  public static final int JPEG = 20;
  public static final int VP8 = 30;
  public static final int VP9 = 31;
  public static final int H263 = 40;
  public static final int H263_1998 = 41;
  public static final int H263_2000 = 42;
  public static final int H264 = 50;
  public static final int H265 = 60;
}
