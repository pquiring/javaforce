//    $Id: GSMDecoder.java,v 1.1 2000/08/10 17:47:08 pfisterer Exp $
//    This file is part of the GSM 6.10 audio decoder library for Java
//    Copyright (C) 1998 Steven Pickles (pix@test.at)
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Library General Public
//    License as published by the Free Software Foundation; either
//    version 2 of the License, or (at your option) any later version.
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Library General Public License for more details.
//    You should have received a copy of the GNU Library General Public
//    License along with this library; if not, write to the Free
//    Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  This software is a port of the GSM Library provided by
//  Jutta Degener (jutta@cs.tu-berlin.de) and
//  Carsten Bormann (cabo@cs.tu-berlin.de),
//  Technische Universitaet Berlin
package javaforce.codec.gsm;

public final class GSMDecoder {

  private static final byte GSM_MAGIC = 0x0d;

  private static final int[] FAC = new int[] {18431, 20479, 22527, 24575,
    26623, 28671, 30719, 32767};

  private static final int[] QLB = new int[] {3277, 11469, 21299, 32767};

  // TODO: can be replaced by Short.MIN_VALUE and Short.MAX_VALUE ?
  private static final int MIN_WORD = -32767 - 1;
  private static final int MAX_WORD = 32767;

  private int dp0[] = new int[280];

  private int u[] = new int[8];
  private int LARpp[][] = new int[2][8];
  private int j;

  private int nrp;
  private int v[] = new int[9];
  private int msr;

  public void GSM() {
    nrp = 40;
  }

  public final int[] decode(byte c[])
          throws InvalidGSMFrameException {
    if (c.length != 33) {
      throw new InvalidGSMFrameException();
    }

    int i = 0;

    if (((c[i] >> 4) & 0xf) != GSM_MAGIC) {
      throw new InvalidGSMFrameException();
    }

    int LARc[] = new int[8];
    int Nc[] = new int[4];
    int Mc[] = new int[4];
    int bc[] = new int[4];
    int xmaxc[] = new int[4];
    int xmc[] = new int[13 * 4];

    LARc[0] = ((c[i++] & 0xF) << 2);
    /* 1 */
    LARc[0] |= ((c[i] >> 6) & 0x3);
    LARc[1] = (c[i++] & 0x3F);
    LARc[2] = ((c[i] >> 3) & 0x1F);
    LARc[3] = ((c[i++] & 0x7) << 2);
    LARc[3] |= ((c[i] >> 6) & 0x3);
    LARc[4] = ((c[i] >> 2) & 0xF);
    LARc[5] = ((c[i++] & 0x3) << 2);
    LARc[5] |= ((c[i] >> 6) & 0x3);
    LARc[6] = ((c[i] >> 3) & 0x7);
    LARc[7] = (c[i++] & 0x7);
    Nc[0] = ((c[i] >> 1) & 0x7F);
    bc[0] = ((c[i++] & 0x1) << 1);
    bc[0] |= ((c[i] >> 7) & 0x1);
    Mc[0] = ((c[i] >> 5) & 0x3);
    xmaxc[0] = ((c[i++] & 0x1F) << 1);
    xmaxc[0] |= ((c[i] >> 7) & 0x1);
    xmc[0] = ((c[i] >> 4) & 0x7);
    xmc[1] = ((c[i] >> 1) & 0x7);
    xmc[2] = ((c[i++] & 0x1) << 2);
    xmc[2] |= ((c[i] >> 6) & 0x3);
    xmc[3] = ((c[i] >> 3) & 0x7);
    xmc[4] = (c[i++] & 0x7);
    xmc[5] = ((c[i] >> 5) & 0x7);
    xmc[6] = ((c[i] >> 2) & 0x7);
    xmc[7] = ((c[i++] & 0x3) << 1);
    /* 10 */
    xmc[7] |= ((c[i] >> 7) & 0x1);
    xmc[8] = ((c[i] >> 4) & 0x7);
    xmc[9] = ((c[i] >> 1) & 0x7);
    xmc[10] = ((c[i++] & 0x1) << 2);
    xmc[10] |= ((c[i] >> 6) & 0x3);
    xmc[11] = ((c[i] >> 3) & 0x7);
    xmc[12] = (c[i++] & 0x7);
    Nc[1] = ((c[i] >> 1) & 0x7F);
    bc[1] = ((c[i++] & 0x1) << 1);
    bc[1] |= ((c[i] >> 7) & 0x1);
    Mc[1] = ((c[i] >> 5) & 0x3);
    xmaxc[1] = ((c[i++] & 0x1F) << 1);
    xmaxc[1] |= ((c[i] >> 7) & 0x1);
    xmc[13] = ((c[i] >> 4) & 0x7);
    xmc[14] = ((c[i] >> 1) & 0x7);
    xmc[15] = ((c[i++] & 0x1) << 2);
    xmc[15] |= ((c[i] >> 6) & 0x3);
    xmc[16] = ((c[i] >> 3) & 0x7);
    xmc[17] = (c[i++] & 0x7);
    xmc[18] = ((c[i] >> 5) & 0x7);
    xmc[19] = ((c[i] >> 2) & 0x7);
    xmc[20] = ((c[i++] & 0x3) << 1);
    xmc[20] |= ((c[i] >> 7) & 0x1);
    xmc[21] = ((c[i] >> 4) & 0x7);
    xmc[22] = ((c[i] >> 1) & 0x7);
    xmc[23] = ((c[i++] & 0x1) << 2);
    xmc[23] |= ((c[i] >> 6) & 0x3);
    xmc[24] = ((c[i] >> 3) & 0x7);
    xmc[25] = (c[i++] & 0x7);
    Nc[2] = ((c[i] >> 1) & 0x7F);
    bc[2] = ((c[i++] & 0x1) << 1);
    /* 20 */
    bc[2] |= ((c[i] >> 7) & 0x1);
    Mc[2] = ((c[i] >> 5) & 0x3);
    xmaxc[2] = ((c[i++] & 0x1F) << 1);
    xmaxc[2] |= ((c[i] >> 7) & 0x1);
    xmc[26] = ((c[i] >> 4) & 0x7);
    xmc[27] = ((c[i] >> 1) & 0x7);
    xmc[28] = ((c[i++] & 0x1) << 2);
    xmc[28] |= ((c[i] >> 6) & 0x3);
    xmc[29] = ((c[i] >> 3) & 0x7);
    xmc[30] = (c[i++] & 0x7);
    xmc[31] = ((c[i] >> 5) & 0x7);
    xmc[32] = ((c[i] >> 2) & 0x7);
    xmc[33] = ((c[i++] & 0x3) << 1);
    xmc[33] |= ((c[i] >> 7) & 0x1);
    xmc[34] = ((c[i] >> 4) & 0x7);
    xmc[35] = ((c[i] >> 1) & 0x7);
    xmc[36] = ((c[i++] & 0x1) << 2);
    xmc[36] |= ((c[i] >> 6) & 0x3);
    xmc[37] = ((c[i] >> 3) & 0x7);
    xmc[38] = (c[i++] & 0x7);
    Nc[3] = ((c[i] >> 1) & 0x7F);
    bc[3] = ((c[i++] & 0x1) << 1);
    bc[3] |= ((c[i] >> 7) & 0x1);
    Mc[3] = ((c[i] >> 5) & 0x3);
    xmaxc[3] = ((c[i++] & 0x1F) << 1);
    xmaxc[3] |= ((c[i] >> 7) & 0x1);
    xmc[39] = ((c[i] >> 4) & 0x7);
    xmc[40] = ((c[i] >> 1) & 0x7);
    xmc[41] = ((c[i++] & 0x1) << 2);
    xmc[41] |= ((c[i] >> 6) & 0x3);
    xmc[42] = ((c[i] >> 3) & 0x7);
    xmc[43] = (c[i++] & 0x7);
    /* 30  */
    xmc[44] = ((c[i] >> 5) & 0x7);
    xmc[45] = ((c[i] >> 2) & 0x7);
    xmc[46] = ((c[i++] & 0x3) << 1);
    xmc[46] |= ((c[i] >> 7) & 0x1);
    xmc[47] = ((c[i] >> 4) & 0x7);
    xmc[48] = ((c[i] >> 1) & 0x7);
    xmc[49] = ((c[i++] & 0x1) << 2);
    xmc[49] |= ((c[i] >> 6) & 0x3);
    xmc[50] = ((c[i] >> 3) & 0x7);
    xmc[51] = (c[i] & 0x7);
    /* 33 */

    return decoder(LARc, Nc, bc, Mc, xmaxc, xmc);
  }

  public final static void print(String name, int data[]) {
    System.out.print("[" + name + ":");
    for (int i = 0; i < data.length; i++) {
      System.out.print("" + data[i]);
      if (i < data.length - 1) {
        System.out.print(",");
      } else {
        System.out.println("]");
      }
    }
  }

  public final static void print(String name, int data) {
    System.out.println("[" + name + ":" + data + "]");
  }

  private final int[] decoder(int LARcr[],
          int Ncr[],
          int bcr[],
          int Mcr[],
          int xmaxcr[],
          int xMcr[]) {
    int j, k;
    int erp[] = new int[40];
    int wt[] = new int[160];
    // drp is just dp0+120

    //print("LARcr",LARcr);
    //print("Ncr",Ncr);
    //print("bcr",bcr);
    //print("Mcr",Mcr);
    //print("xmaxcr",xmaxcr);
    //print("xMcr",xMcr);
    for (j = 0; j < 4; j++) {
      // find out what is done with xMcr
      RPEDecoding(xmaxcr[j], Mcr[j], xMcr, j * 13, erp);

      //print("erp",erp);
      longTermSynthesisFiltering(Ncr[j], bcr[j], erp, dp0);

      for (k = 0; k < 40; k++) {
        wt[j * 40 + k] = dp0[120 + k];
      }

    }

    //print("LARcr",LARcr);
    //print("wt",wt);
    int s[] = shortTermSynthesisFilter(LARcr, wt);

    //print("s",s);
    postprocessing(s);

    return s;

  }

  private final void RPEDecoding(int xmaxcr,
          int Mcr,
          int xMcr[],
          int xMcrOffset,
          int erp[]) {
    int expAndMant[];
    int xMp[] = new int[13];

    expAndMant = xmaxcToExpAndMant(xmaxcr);

    //System.out.println("[e&m:"+expAndMant[0]+","+expAndMant[1]+"]");
    APCMInverseQuantization(xMcr, xMcrOffset, expAndMant[0], expAndMant[1], xMp);

    //print("xMp",xMp);
    RPE_grid_positioning(Mcr, xMp, erp);

  }

  private final int[] xmaxcToExpAndMant(int xmaxc) {
    int exp, mant;

    exp = 0;
    if (xmaxc > 15) {
      exp = ((xmaxc >> 3) - 1);
    }
    mant = (xmaxc - (exp << 3));

    if (mant == 0) {
      exp = -4;
      mant = 7;
    } else {
      while (mant <= 7) {
        mant = (mant << 1 | 1);
        exp--;
      }
      mant -= 8;
    }

    //assert(exp>=-4 && exp <= 6);
    //assert(mant>=0 && mant<=7);
    int result[] = new int[2];
    result[0] = exp;
    result[1] = mant;

    return result;

  }

  //private void assert(boolean test) {
  //  if (!test) {
  //    System.out.println("assertion error");
  //  }
  //}
  private final void APCMInverseQuantization(int xMc[],
          int xMcOffset,
          int exp,
          int mant,
          int xMp[]) {
    int i, p;
    int temp, temp1, temp2, temp3;
    int ltmp;

    //assert(mant >0 && mant <= 7 );
    temp1 = FAC[mant];
    temp2 = sub(6, exp);
    temp3 = asl(1, sub(temp2, 1));

    //System.out.println("temp1="+temp1);
    //System.out.println("temp2="+temp2);
    //System.out.println("temp3="+temp3);
    p = 0;

    for (i = 13; i-- > 0;) {
      //assert(xMc[xMcOffset] <= 7 && xMc[xMcOffset] >= 0);

      temp = ((xMc[xMcOffset++] << 1) - 7);

      //System.out.println("s1:temp="+temp);
      //assert(temp<=7 && temp >= -7);
      temp = (temp << 12);//&0xffff;

      //System.out.println("s2:temp="+temp);
      temp = mult_r(temp1, temp);

      //System.out.println("s3:temp="+temp);
      temp = add(temp, temp3);

      //System.out.println("s4:temp="+temp);
      xMp[p++] = asr(temp, temp2);
    }
  }

  private final static int saturate(int x) {
    return (x < MIN_WORD ? MIN_WORD : (x > MAX_WORD ? MAX_WORD : x));
  }

  private final static int sub(int a, int b) {
    int diff = a - b;
    return saturate(diff);
  }

  private final static int add(int a, int b) {
    int sum = a + b;
    return saturate(sum);
  }

  private final static int asl(int a, int n) {
    if (n >= 16) {
      return 0;
    }
    if (n <= -16) {
      return (a < 0 ? -1 : 0);
    }
    if (n < 0) {
      return asr(a, -n);
    }
    return (a << n);
  }

  private final static int asr(int a, int n) {
    if (n >= 16) {
      return (a < 0 ? -1 : 0);
    }
    if (n <= -16) {
      return 0;
    }
    if (n < 0) {
      return (a << -n);//&0xffff;
    }
    return (a >> n);
  }

  private final static int mult_r(int a, int b) {
    if (b == MIN_WORD && a == MIN_WORD) {
      return MAX_WORD;
    } else {
      int prod = a * b + 16384;
      //prod >>= 15;
      return saturate(prod >> 15);//&0xffff;
      //return (prod & 0xffff);
    }
  }

  private final void longTermSynthesisFiltering(int Ncr,
          int bcr,
          int erp[],
          int dp0[]) {
    int ltmp;
    int k;
    int brp, drpp, Nr;

    Nr = Ncr < 40 || Ncr > 120 ? nrp : Ncr;
    nrp = Nr;

    brp = QLB[bcr];

    for (k = 0; k <= 39; k++) {
      drpp = mult_r(brp, dp0[120 + (k - Nr)]);
      dp0[120 + k] = add(erp[k], drpp);
    }

    for (k = 0; k <= 119; k++) {
      dp0[k] = dp0[40 + k];
    }

  }

  private final int[] shortTermSynthesisFilter(int LARcr[],
          int wt[]) {

    //print("wt",wt);
    int LARpp_j[] = LARpp[j];
    int LARpp_j_1[] = LARpp[j ^= 1];

    int LARp[] = new int[8];

    int s[] = new int[160];

    decodingOfTheCodedLogAreaRatios(LARcr, LARpp_j);

    //print("LARpp_j",LARpp_j);
    Coefficients_0_12(LARpp_j_1, LARpp_j, LARp);
    LARp_to_rp(LARp);
    shortTermSynthesisFiltering(LARp, 13, wt, s, 0);

    Coefficients_13_26(LARpp_j_1, LARpp_j, LARp);
    LARp_to_rp(LARp);
    shortTermSynthesisFiltering(LARp, 14, wt, s, 13);

    Coefficients_27_39(LARpp_j_1, LARpp_j, LARp);
    LARp_to_rp(LARp);
    shortTermSynthesisFiltering(LARp, 13, wt, s, 27);

    Coefficients_40_159(LARpp_j, LARp);
    LARp_to_rp(LARp);
    shortTermSynthesisFiltering(LARp, 120, wt, s, 40);

    return s;

  }

  public final static void decodingOfTheCodedLogAreaRatios(int LARc[],
          int LARpp[]) {
    int temp1;
    int ltmp;

    // STEP(      0,  -32,  13107 );
    temp1 = (add(LARc[0], -32) << 10);
    //temp1 = (sub(temp1, 0));
    temp1 = (mult_r(13107, temp1));
    LARpp[0] = (add(temp1, temp1));

    //         STEP(      0,  -32,  13107 );
    temp1 = (add(LARc[1], -32) << 10);
    //temp1 = (sub(temp1, 0));
    temp1 = (mult_r(13107, temp1));
    LARpp[1] = (add(temp1, temp1));

    //         STEP(   2048,  -16,  13107 );
    temp1 = (add(LARc[2], -16) << 10);
    temp1 = (sub(temp1, 4096));
    temp1 = (mult_r(13107, temp1));
    LARpp[2] = (add(temp1, temp1));

    //         STEP(  -2560,  -16,  13107 );
    temp1 = (add(LARc[3], (-16)) << 10);
    temp1 = (sub(temp1, -5120));
    temp1 = (mult_r(13107, temp1));
    LARpp[3] = (add(temp1, temp1));

    //         STEP(     94,   -8,  19223 );
    temp1 = (add(LARc[4], -8) << 10);
    temp1 = (sub(temp1, 188));
    temp1 = (mult_r(19223, temp1));
    LARpp[4] = (add(temp1, temp1));

    //         STEP(  -1792,   -8,  17476 );
    temp1 = (add(LARc[5], (-8)) << 10);
    temp1 = (sub(temp1, -3584));
    temp1 = (mult_r(17476, temp1));
    LARpp[5] = (add(temp1, temp1));

    //         STEP(   -341,   -4,  31454 );
    temp1 = (add(LARc[6], (-4)) << 10);
    temp1 = (sub(temp1, -682));
    temp1 = (mult_r(31454, temp1));
    LARpp[6] = (add(temp1, temp1));

    //         STEP(  -1144,   -4,  29708 );
    temp1 = (add(LARc[7], -4) << 10);
    temp1 = (sub(temp1, -2288));
    temp1 = (mult_r(29708, temp1));
    LARpp[7] = (add(temp1, temp1));

  }

  private final static void Coefficients_0_12(int LARpp_j_1[],
          int LARpp_j[],
          int LARp[]) {
    int i;
    int ltmp;

    for (i = 0; i < 8; i++) {
      LARp[i] = add((LARpp_j_1[i] >> 2), (LARpp_j[i] >> 2));
      LARp[i] = add(LARp[i], (LARpp_j_1[i] >> 1));
    }
  }

  private final static void Coefficients_13_26(int LARpp_j_1[],
          int LARpp_j[],
          int LARp[]) {
    int i;
    int ltmp;

    for (i = 0; i < 8; i++) {
      LARp[i] = add((LARpp_j_1[i] >> 1), (LARpp_j[i] >> 1));
    }
  }

  private final static void Coefficients_27_39(int LARpp_j_1[],
          int LARpp_j[],
          int LARp[]) {
    int i;
    int ltmp;

    for (i = 0; i < 8; i++) {
      LARp[i] = add((LARpp_j_1[i] >> 2), (LARpp_j[i] >> 2));
      LARp[i] = add(LARp[i], (LARpp_j[i] >> 1));
    }
  }

  private final static void Coefficients_40_159(int LARpp_j[],
          int LARp[]) {
    int i;
    int ltmp;

    for (i = 0; i < 8; i++) {
      LARp[i] = LARpp_j[i];
    }
  }

  private final static void LARp_to_rp(int LARp[]) {

    int i;
    int temp;

    for (i = 0; i < 8; i++) {
      if (LARp[i] < 0) {
        temp = ((LARp[i] == MIN_WORD) ? MAX_WORD : -LARp[i]);
        LARp[i] = (-((temp < 11059) ? temp << 1
                : ((temp < 20070) ? temp + 11059
                        : add((temp >> 2), 26112))));
      } else {
        temp = LARp[i];
        LARp[i] = ((temp < 11059) ? temp << 1
                : ((temp < 20070) ? temp + 11059
                        : add((temp >> 2), 26112)));
      }
    }
  }

  //      shortTermSynthesisFiltering(LARp,13,wt,s,0);
  private final void shortTermSynthesisFiltering(int rrp[],
          int k,
          int wt[],
          int sr[],
          int off) {
    int i;
    int sri, tmp1, tmp2;
    int woff = off;
    int soff = off;

    while (k-- > 0) {
      sri = wt[woff++];
      for (i = 8; i-- > 0;) {
        tmp1 = rrp[i];
        tmp2 = v[i];
        tmp2 = ((tmp1 == MIN_WORD && tmp2 == MIN_WORD
                ? MAX_WORD
                : saturate((tmp1 * tmp2 + 16384) >> 15)));
        sri = sub(sri, tmp2);

        tmp1 = ((tmp1 == MIN_WORD && sri == MIN_WORD
                ? MAX_WORD
                : saturate((tmp1 * sri + 16384) >> 15)));
        v[i + 1] = add(v[i], tmp1);
      }
      sr[soff++] = v[0] = sri;
    }
  }

  private final void postprocessing(int s[]) {
    int k, soff = 0;
    int tmp;
    for (k = 160; k-- > 0; soff++) {
      tmp = mult_r(msr, (28180));
      msr = add(s[soff], tmp);
      //s[soff]=(add(msr,msr) & 0xfff8);
      s[soff] = saturate(add(msr, msr) & ~0x7);
    }
  }

  private final static void RPE_grid_positioning(int Mc,
          int xMp[],
          int ep[]) {
    int i = 13;

    int epo = 0;
    int po = 0;

    switch (Mc) {
      case 3:
        ep[epo++] = 0;
      case 2:
        ep[epo++] = 0;
      case 1:
        ep[epo++] = 0;
      case 0:
        ep[epo++] = xMp[po++];
        i--;
    };

    do {
      ep[epo++] = 0;
      ep[epo++] = 0;
      ep[epo++] = xMp[po++];
    } while (--i > 0);

    while (++Mc < 4) {
      ep[epo++] = 0;
    }
  }

}

/**
 * * GSMDecoder.java **
 */
