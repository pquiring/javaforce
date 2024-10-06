package javaforce.voip;

/**
 * G.722 codec
 *
 * Derived from Asterisk source code.
 *
 * NOTE : Although g722 is 16000Hz the SDP should state it's 8000Hz.
 * This was a bug in RFC 1890 (clarified in RFC 3551)
 *
 * @author pquiring
 *
 * Created : July 5, 2014
 *
 */

public class g722 implements RTPAudioCoder {

  private int bits_per_sample = 8;  //6=48k,7=56k,8=64k
  private boolean eight_k = false;  //8k (16k)
  private boolean packed = false;  //6 or 7 bits per sample
  private boolean itu_test_mode = false;

  private RTP rtp;
  private byte[] encoded = new byte[160 + 12];

  public g722(RTP rtp) {
    this.rtp = rtp;
    for(int a=0;a<2;a++) {
      e_band[a] = new Band();
      d_band[a] = new Band();
    }
  }

  public void setid(int id) {};  //ignored - fixed value

  public int getPacketSize() {
    return 160;
  }

  private static class Band {
    int s, sp, sz;
    int[] r = new int[3];
    int[] a = new int[3];
    int[] ap = new int[3];
    int[] p = new int[3];
    int[] d = new int[7];
    int[] b = new int[7];
    int[] bp = new int[7];
    int[] sg = new int[7];
    int nb, det;
  }
  private int in_buffer;
  private int in_bits;
  private int out_buffer;
  private int out_bits;

  private int[] e_x = new int[24];  //signal history for QMF
  private Band[] e_band = new Band[2];

  private int[] d_x = new int[24];  //signal history for QMF
  private Band[] d_band = new Band[2];

  private static short saturate(int amp) {
    if (amp > Short.MAX_VALUE) {
      return Short.MAX_VALUE;
    }
    if (amp < Short.MIN_VALUE) {
      return Short.MIN_VALUE;
    }
    return (short)amp;
  }

  private final int[] q6
          = new int[] {
            0, 35, 72, 110, 150, 190, 233, 276,
            323, 370, 422, 473, 530, 587, 650, 714,
            786, 858, 940, 1023, 1121, 1219, 1339, 1458,
            1612, 1765, 1980, 2195, 2557, 2919, 0, 0
          };
  private final int[] iln
          = new int[] {
            0, 63, 62, 31, 30, 29, 28, 27,
            26, 25, 24, 23, 22, 21, 20, 19,
            18, 17, 16, 15, 14, 13, 12, 11,
            10, 9, 8, 7, 6, 5, 4, 0
          };
  private final int[] ilp
          = new int[] {
            0, 61, 60, 59, 58, 57, 56, 55,
            54, 53, 52, 51, 50, 49, 48, 47,
            46, 45, 44, 43, 42, 41, 40, 39,
            38, 37, 36, 35, 34, 33, 32, 0
          };
  private final int[] wl = new int[] {-60, -30, 58, 172, 334, 538, 1198, 3042};
  private final int[] rl42 = new int[] {0, 7, 6, 5, 4, 3, 2, 1, 7, 6, 5, 4, 3, 2, 1, 0};
  private final int[] ilb
          = new int[] {
            2048, 2093, 2139, 2186, 2233, 2282, 2332,
            2383, 2435, 2489, 2543, 2599, 2656, 2714,
            2774, 2834, 2896, 2960, 3025, 3091, 3158,
            3228, 3298, 3371, 3444, 3520, 3597, 3676,
            3756, 3838, 3922, 4008
          };
  private final int[] qm4
          = new int[] {
            0, -20456, -12896, -8968,
            -6288, -4240, -2584, -1200,
            20456, 12896, 8968, 6288,
            4240, 2584, 1200, 0
          };
  private final int[] qm2 = new int[] {-7408, -1616, 7408, 1616};
  private final int[] qmf_coeffs = new int[] {3, -11, 12, 32, -210, 951, 3876, -805, 362, -156, 53, -11};
  private final int[] ihn = new int[] {0, 1, 0};
  private final int[] ihp = new int[] {0, 3, 2};
  private final int[] wh = new int[] {0, -214, 798};
  private final int[] rh2 = new int[] {2, 1, 2, 1};

  private void block4(Band band, int d) {
    int wd1;
    int wd2;
    int wd3;
    int i;

    /* Block 4, RECONS */
    band.d[0] = d;
    band.r[0] = saturate(band.s + d);

    /* Block 4, PARREC */
    band.p[0] = saturate(band.sz + d);

    /* Block 4, UPPOL2 */
    for (i = 0; i < 3; i++) {
      band.sg[i] = band.p[i] >> 15;
    }
    wd1 = saturate(band.a[1] << 2);

    wd2 = (band.sg[0] == band.sg[1]) ? -wd1 : wd1;
    if (wd2 > 32767) {
      wd2 = 32767;
    }
    wd3 = (wd2 >> 7) + ((band.sg[0] == band.sg[2]) ? 128 : -128);
    wd3 += (band.a[2] * 32512) >> 15;
    if (wd3 > 12288) {
      wd3 = 12288;
    } else if (wd3 < -12288) {
      wd3 = -12288;
    }
    band.ap[2] = wd3;

    /* Block 4, UPPOL1 */
    band.sg[0] = band.p[0] >> 15;
    band.sg[1] = band.p[1] >> 15;
    wd1 = (band.sg[0] == band.sg[1]) ? 192 : -192;
    wd2 = (band.a[1] * 32640) >> 15;

    band.ap[1] = saturate(wd1 + wd2);
    wd3 = saturate(15360 - band.ap[2]);
    if (band.ap[1] > wd3) {
      band.ap[1] = wd3;
    } else if (band.ap[1] < -wd3) {
      band.ap[1] = -wd3;
    }

    /* Block 4, UPZERO */
    wd1 = (d == 0) ? 0 : 128;
    band.sg[0] = d >> 15;
    for (i = 1; i < 7; i++) {
      band.sg[i] = band.d[i] >> 15;
      wd2 = (band.sg[i] == band.sg[0]) ? wd1 : -wd1;
      wd3 = (band.b[i] * 32640) >> 15;
      band.bp[i] = saturate(wd2 + wd3);
    }

    /* Block 4, DELAYA */
    for (i = 6; i > 0; i--) {
      band.d[i] = band.d[i - 1];
      band.b[i] = band.bp[i];
    }

    for (i = 2; i > 0; i--) {
      band.r[i] = band.r[i - 1];
      band.p[i] = band.p[i - 1];
      band.a[i] = band.ap[i];
    }

    /* Block 4, FILTEP */
    wd1 = saturate(band.r[1] + band.r[1]);
    wd1 = (band.a[1] * wd1) >> 15;
    wd2 = saturate(band.r[2] + band.r[2]);
    wd2 = (band.a[2] * wd2) >> 15;
    band.sp = saturate(wd1 + wd2);

    /* Block 4, FILTEZ */
    band.sz = 0;
    for (i = 6; i > 0; i--) {
      wd1 = saturate(band.d[i] + band.d[i]);
      band.sz += (band.b[i] * wd1) >> 15;
    }
    band.sz = saturate(band.sz);

    /* Block 4, PREDIC */
    band.s = saturate(band.sp + band.sz);
  }

  public byte[] encode(short[] src16) {
    RTPChannel rtpChannel = rtp.getDefaultChannel();
    RTPChannel.buildHeader(encoded, 9, rtpChannel.getseqnum(), rtpChannel.gettimestamp(160), rtpChannel.getssrc(), false);

    int dlow;
    int dhigh;
    int el;
    int wd;
    int wd1;
    int ril;
    int wd2;
    int il4;
    int ih2;
    int wd3;
    int eh;
    int mih;
    int i;
    int j;
    /* Low and high band PCM from the QMF */
    int xlow;
    int xhigh;
    int g722_bytes;
    /* Even and odd tap accumulators */
    int sumeven;
    int sumodd;
    int ihigh;
    int ilow;
    int code;

    g722_bytes = 12;
    xhigh = 0;
    for (j = 0; j < src16.length;) {
      if (itu_test_mode) {
        xlow = xhigh = src16[j++] >> 1;
      } else {
        if (eight_k) {
          xlow = src16[j++] >> 1;
        } else {
          /* Apply the transmit QMF */
          /* Shuffle the buffer down */
          for (i = 0; i < 22; i++) {
            e_x[i] = e_x[i + 2];
          }
          e_x[22] = src16[j++];
          e_x[23] = src16[j++];

          /* Discard every other QMF output */
          sumeven = 0;
          sumodd = 0;
          for (i = 0; i < 12; i++) {
            sumodd += e_x[2 * i] * qmf_coeffs[i];
            sumeven += e_x[2 * i + 1] * qmf_coeffs[11 - i];
          }
          xlow = (sumeven + sumodd) >> 14;
          xhigh = (sumeven - sumodd) >> 14;
        }
      }
      /* Block 1L, SUBTRA */
      el = saturate(xlow - e_band[0].s);

      /* Block 1L, QUANTL */
      wd = (el >= 0) ? el : -(el + 1);

      for (i = 1; i < 30; i++) {
        wd1 = (q6[i] * e_band[0].det) >> 12;
        if (wd < wd1) {
          break;
        }
      }
      ilow = (el < 0) ? iln[i] : ilp[i];

      /* Block 2L, INVQAL */
      ril = ilow >> 2;
      wd2 = qm4[ril];
      dlow = (e_band[0].det * wd2) >> 15;

      /* Block 3L, LOGSCL */
      il4 = rl42[ril];
      wd = (e_band[0].nb * 127) >> 7;
      e_band[0].nb = wd + wl[il4];
      if (e_band[0].nb < 0) {
        e_band[0].nb = 0;
      } else if (e_band[0].nb > 18432) {
        e_band[0].nb = 18432;
      }

      /* Block 3L, SCALEL */
      wd1 = (e_band[0].nb >> 6) & 31;
      wd2 = 8 - (e_band[0].nb >> 11);
      wd3 = (wd2 < 0) ? (ilb[wd1] << -wd2) : (ilb[wd1] >> wd2);
      e_band[0].det = wd3 << 2;

      block4(e_band[0], dlow);

      if (eight_k) {
        /* Just leave the high bits as zero */
        code = (0xC0 | ilow) >> (8 - bits_per_sample);
      } else {
        /* Block 1H, SUBTRA */
        eh = saturate(xhigh - e_band[1].s);

        /* Block 1H, QUANTH */
        wd = (eh >= 0) ? eh : -(eh + 1);
        wd1 = (564 * e_band[1].det) >> 12;
        mih = (wd >= wd1) ? 2 : 1;
        ihigh = (eh < 0) ? ihn[mih] : ihp[mih];

        /* Block 2H, INVQAH */
        wd2 = qm2[ihigh];
        dhigh = (e_band[1].det * wd2) >> 15;

        /* Block 3H, LOGSCH */
        ih2 = rh2[ihigh];
        wd = (e_band[1].nb * 127) >> 7;
        e_band[1].nb = wd + wh[ih2];
        if (e_band[1].nb < 0) {
          e_band[1].nb = 0;
        } else if (e_band[1].nb > 22528) {
          e_band[1].nb = 22528;
        }

        /* Block 3H, SCALEH */
        wd1 = (e_band[1].nb >> 6) & 31;
        wd2 = 10 - (e_band[1].nb >> 11);
        wd3 = (wd2 < 0) ? (ilb[wd1] << -wd2) : (ilb[wd1] >> wd2);
        e_band[1].det = wd3 << 2;

        block4(e_band[1], dhigh);
        code = ((ihigh << 6) | ilow) >> (8 - bits_per_sample);
      }

      if (packed) {
        /* Pack the code bits */
        out_buffer |= (code << out_bits);
        out_bits += bits_per_sample;
        if (out_bits >= 8) {
          encoded[g722_bytes++] = (byte) (out_buffer & 0xff);
          out_bits -= 8;
          out_buffer >>= 8;
        }
      } else {
        encoded[g722_bytes++] = (byte) code;
      }
    }
    return encoded;
  }

// DECODING ...
  private final int[] qm5
          = new int[] {
            -280, -280, -23352, -17560,
            -14120, -11664, -9752, -8184,
            -6864, -5712, -4696, -3784,
            -2960, -2208, -1520, -880,
            23352, 17560, 14120, 11664,
            9752, 8184, 6864, 5712,
            4696, 3784, 2960, 2208,
            1520, 880, 280, -280
          };
  private final int[] qm6
          = new int[] {
            -136, -136, -136, -136,
            -24808, -21904, -19008, -16704,
            -14984, -13512, -12280, -11192,
            -10232, -9360, -8576, -7856,
            -7192, -6576, -6000, -5456,
            -4944, -4464, -4008, -3576,
            -3168, -2776, -2400, -2032,
            -1688, -1360, -1040, -728,
            24808, 21904, 19008, 16704,
            14984, 13512, 12280, 11192,
            10232, 9360, 8576, 7856,
            7192, 6576, 6000, 5456,
            4944, 4464, 4008, 3576,
            3168, 2776, 2400, 2032,
            1688, 1360, 1040, 728,
            432, 136, -432, -136
          };

  private int decode_timestamp;

  private final short[] decoded = new short[320];

  public short[] decode(byte[] src8, int off) {
    int dlowt;
    int rlow;
    int ihigh;
    int dhigh;
    int rhigh;
    int xout1;
    int xout2;
    int wd1;
    int wd2;
    int wd3;
    int code;
    int outlen;
    int i;
    int j;

    off += 12;  //skip RTP header
    outlen = 0;
    rhigh = 0;
    for (j = 0; j < 160;) {
      if (packed) {
        /* Unpack the code bits */
        if (in_bits < bits_per_sample) {
          in_buffer |= ((((int)src8[off + j++]) & 0xff) << in_bits);
          in_bits += 8;
        }
        code = in_buffer & ((1 << bits_per_sample) - 1);
        in_buffer >>= bits_per_sample;
        in_bits -= bits_per_sample;
      } else {
        code = ((int)src8[off + j++]) & 0xff;
      }

      switch (bits_per_sample) {
        default:
        case 8:
          wd1 = code & 0x3F;
          ihigh = (code >> 6) & 0x03;
          wd2 = qm6[wd1];
          wd1 >>= 2;
          break;
        case 7:
          wd1 = code & 0x1F;
          ihigh = (code >> 5) & 0x03;
          wd2 = qm5[wd1];
          wd1 >>= 1;
          break;
        case 6:
          wd1 = code & 0x0F;
          ihigh = (code >> 4) & 0x03;
          wd2 = qm4[wd1];
          break;
      }
      /* Block 5L, LOW BAND INVQBL */
      wd2 = (d_band[0].det * wd2) >> 15;
      /* Block 5L, RECONS */
      rlow = d_band[0].s + wd2;
      /* Block 6L, LIMIT */
      if (rlow > 16383) {
        rlow = 16383;
      } else if (rlow < -16384) {
        rlow = -16384;
      }

      /* Block 2L, INVQAL */
      wd2 = qm4[wd1];
      dlowt = (d_band[0].det * wd2) >> 15;

      /* Block 3L, LOGSCL */
      wd2 = rl42[wd1];
      wd1 = (d_band[0].nb * 127) >> 7;
      wd1 += wl[wd2];
      if (wd1 < 0) {
        wd1 = 0;
      } else if (wd1 > 18432) {
        wd1 = 18432;
      }
      d_band[0].nb = wd1;

      /* Block 3L, SCALEL */
      wd1 = (d_band[0].nb >> 6) & 31;
      wd2 = 8 - (d_band[0].nb >> 11);
      wd3 = (wd2 < 0) ? (ilb[wd1] << -wd2) : (ilb[wd1] >> wd2);
      d_band[0].det = wd3 << 2;

      block4(d_band[0], dlowt);

      if (!eight_k) {
        /* Block 2H, INVQAH */
        wd2 = qm2[ihigh];
        dhigh = (d_band[1].det * wd2) >> 15;
        /* Block 5H, RECONS */
        rhigh = dhigh + d_band[1].s;
        /* Block 6H, LIMIT */
        if (rhigh > 16383) {
          rhigh = 16383;
        } else if (rhigh < -16384) {
          rhigh = -16384;
        }

        /* Block 2H, INVQAH */
        wd2 = rh2[ihigh];
        wd1 = (d_band[1].nb * 127) >> 7;
        wd1 += wh[wd2];
        if (wd1 < 0) {
          wd1 = 0;
        } else if (wd1 > 22528) {
          wd1 = 22528;
        }
        d_band[1].nb = wd1;

        /* Block 3H, SCALEH */
        wd1 = (d_band[1].nb >> 6) & 31;
        wd2 = 10 - (d_band[1].nb >> 11);
        wd3 = (wd2 < 0) ? (ilb[wd1] << -wd2) : (ilb[wd1] >> wd2);
        d_band[1].det = wd3 << 2;

        block4(d_band[1], dhigh);
      }

      if (itu_test_mode) {
        decoded[outlen++] = (short) (rlow << 1);
        decoded[outlen++] = (short) (rhigh << 1);
      } else {
        if (eight_k) {
          decoded[outlen++] = (short) (rlow << 1);
        } else {
          /* Apply the receive QMF */
          for (i = 0; i < 22; i++) {
            d_x[i] = d_x[i + 2];
          }
          d_x[22] = rlow + rhigh;
          d_x[23] = rlow - rhigh;

          xout1 = 0;
          xout2 = 0;
          for (i = 0; i < 12; i++) {
            xout2 += d_x[2 * i] * qmf_coeffs[i];
            xout1 += d_x[2 * i + 1] * qmf_coeffs[11 - i];
          }
          decoded[outlen++] = (short) (xout1 >> 11);
          decoded[outlen++] = (short) (xout2 >> 11);
        }
      }
    }
    return decoded;  //outlen;
  }

  public int getSampleRate() {return 16000;}
}
