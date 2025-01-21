package javaforce.voip;

/*
 struct rtp_header {
 byte version;  //usually 0x80
 byte type;  //0x00
 short seqnum;
 int timestamp;
 int syncsrcid;
 };
 */

import javaforce.*;

/**
 * Encodes/decodes g711 packets (A-Law).  (European format)
 */

public class g711a implements RTPAudioCoder {

  private RTP rtp;

  public g711a(RTP rtp) {
    this.rtp = rtp;
  }

  public void setid(int id) {};  //ignored - fixed value

  public int getPacketSize() {
    return 160;
  }

  private static short[] alaw_lut;  //signed
  private static int[] exp_lut = {
		1,1,2,2,3,3,3,3,
		4,4,4,4,4,4,4,4,
		5,5,5,5,5,5,5,5,
		5,5,5,5,5,5,5,5,
		6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,
		7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7
  };

  static {
    g711a.init();
  }

  /** init the decode lookup table (256 entries) */
  private static void init() {
    short i, seg, linear, alaw;
    alaw_lut = new short[256];
    for (short idx = 0; idx < 256; idx++) {
      alaw = idx;
      alaw ^= 0x55;  //AMI_MASK  01010101
      i = (short)(((alaw & 0x0f) << 4) + 8);  /* rounding error */
      seg = (short)(((int)alaw & 0x70) >> 4);
      if (seg != 0) {
        i = (short)((i + 0x100) << (seg - 1));
      }
      alaw_lut[idx] = (short)((((alaw & 0x80) == 0x80) ? i : -i));
    }
  }
  private static final short CLIP = 32767;  //not MAX short

  private byte[] encoded = new byte[160 + 12];

  //samples must be 160 samples
  public byte[] encode(short[] samples) {
    int sign, exponent, mantissa, mag;
    byte alawbyte;
    short sample;
    RTPChannel rtpChannel = rtp.getDefaultChannel();

    RTPChannel.buildHeader(encoded, 8, rtpChannel.getseqnum(), rtpChannel.gettimestamp(160), rtpChannel.getssrc(), false);

    for (int i = 0; i < 160; i++) {
      /* Get the sample into sign-magnitude. */
      sample = samples[i];
      sign = (sample >> 8) & 0x80;     /* set aside the sign */
      if (sign != 0) {
        mag = -sample;
      } else {
        mag = sample;
      }
      if (mag > CLIP) {
        mag = CLIP;     /* clip the magnitude */
      }
      /* Convert from 16 bit linear to alaw. */
      exponent = exp_lut[(mag >> 8) & 0x7f];
      mantissa = ((mag >> (exponent + 3)) & 0x0f);
      if (mag < 0x100) exponent = 0;
      alawbyte = (byte) ((sign | (exponent << 4) | mantissa));
      alawbyte ^= 0x55;
      encoded[12 + i] = alawbyte;
    }
    return encoded;
  }

  private int decode_timestamp;

  private short[] decoded = new short[160];

  //encoded must be 160+12 bytes at least
  public short[] decode(byte[] encoded, int off, int length) {
    int decode_timestamp = BE.getuint32(encoded, off + 4);
    if (this.decode_timestamp == 0) {
      this.decode_timestamp = decode_timestamp;
    } else {
      if (RTP.debug) {
        JFLog.log("G711a:timestamp = " + decode_timestamp + ":" + ((this.decode_timestamp + 160 == decode_timestamp) ? "ok" : "lost packet"));
      }
      this.decode_timestamp = decode_timestamp;
    }
    for (int i = 0; i < 160; i++) {
      decoded[i] = alaw_lut[encoded[off + i + 12] & 0xff];
    }
    return decoded;
  }

  public int getSampleRate() {return 8000;}

  public void close() {}
}
