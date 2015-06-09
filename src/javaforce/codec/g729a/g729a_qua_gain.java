/* g729a_qua_gain - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_qua_gain implements g729a_constants {

  static void g729a_qua_gain(g729a_encode_internal var_g729a_encode_internal,
          g729a_encode_speech var_g729a_encode_speech) {
    float[] fs = var_g729a_encode_internal.c;
    float[] fs_0_ = var_g729a_encode_internal.x;
    float[] fs_1_ = var_g729a_encode_internal.y;
    float[] fs_2_ = var_g729a_encode_internal.z;
    int[] is = var_g729a_encode_speech.U;
    g729a_sinthesis_filter var_g729a_sinthesis_filter = new g729a_sinthesis_filter();
    int i = var_g729a_encode_internal.taming;
    g729a_common.g729a_IIR(fs_2_, fs, var_g729a_encode_internal.a_gamma,
            var_g729a_sinthesis_filter);
    float f = var_g729a_encode_internal.yy;
    float f_3_ = var_g729a_encode_internal.xy;
    float f_4_ = 0.0F;
    float f_5_ = 0.0F;
    float f_6_ = 0.0F;
    for (int i_7_ = 0; i_7_ < 40; i_7_++) {
      f_4_ += fs_2_[i_7_] * fs_2_[i_7_];
      f_5_ += fs_0_[i_7_] * fs_2_[i_7_];
      f_6_ += fs_1_[i_7_] * fs_2_[i_7_];
    }
    float f_8_ = g729a_common.g729a_norma(fs);
    f_8_ = 31.622776F / (float) Math.sqrt((double) (f_8_ / 40.0F));
    float f_9_ = (f_8_ * g729a_tables.g729a_pe0[is[0]]
            * g729a_tables.g729a_pe1[is[1]] * g729a_tables.g729a_pe2[is[2]]
            * g729a_tables.g729a_pe3[is[3]]);
    f_4_ = f_4_ * f_9_ * f_9_;
    f_5_ *= f_9_;
    f_6_ *= f_9_;
    float f_10_ = f * f_4_ - f_6_ * f_6_;
    float f_11_;
    float f_12_;
    if (f_10_ == 0.0F) {
      f_12_ = f_5_ / f_4_;
      f_11_ = 0.9394531F;
    } else {
      f_11_ = (f_4_ * f_3_ - f_5_ * f_6_) / f_10_;
      if (i == 1 && f_11_ > 0.9394531F) {
        f_11_ = 0.9394531F;
      }
      f_12_ = (f_5_ - f_11_ * f_6_) / f_4_;
    }
    float f_13_ = f_12_;
    float f_14_ = f_11_;
    int i_15_;
    int i_16_;
    if (f_9_ > 0.0F) {
      i_15_ = 0;
      i_16_ = 0;
      while (f_13_ > g729a_tables.g729a_thr1[i_15_] && ++i_15_ < 4) {
        /* empty */
      }
      do {
        if (!(f_14_ > g729a_tables.g729a_thr2[i_16_])) {
          break;
        }
      } while (++i_16_ < 8);
    } else {
      i_15_ = 0;
      i_16_ = 0;
      while (f_13_ < g729a_tables.g729a_thr1[i_15_] && ++i_15_ < 4) {
        /* empty */
      }
      while (f_14_ < g729a_tables.g729a_thr2[i_16_] && ++i_16_ < 8) {
        /* empty */
      }
    }
    f_3_ *= 2.0F;
    f_5_ *= 2.0F;
    f_6_ *= 2.0F;
    float f_17_ = (g729a_tables.g729a_gbk1[i_15_][0]
            + g729a_tables.g729a_gbk2[i_16_][0]);
    float f_18_ = (g729a_tables.g729a_gbk1[i_15_][1]
            + g729a_tables.g729a_gbk2[i_16_][1]);
    int i_19_ = i_15_;
    int i_20_ = i_16_;
    float f_21_ = (f_17_ * f_17_ * f + f_18_ * f_18_ * f_4_
            + f_17_ * f_18_ * f_6_ - f_17_ * f_3_ - f_18_ * f_5_);
    if (i == 1) {
      for (int i_22_ = i_16_; i_22_ < i_16_ + 8; i_22_++) {
        for (int i_23_ = i_15_; i_23_ < i_15_ + 4; i_23_++) {
          f_17_ = (g729a_tables.g729a_gbk1[i_23_][0]
                  + g729a_tables.g729a_gbk2[i_22_][0]);
          if (f_17_ < 0.99993896F) {
            f_18_ = (g729a_tables.g729a_gbk1[i_23_][1]
                    + g729a_tables.g729a_gbk2[i_22_][1]);
            float f_24_ = (f_17_ * f_17_ * f + f_18_ * f_18_ * f_4_
                    + f_17_ * f_18_ * f_6_ - f_17_ * f_3_
                    - f_18_ * f_5_);
            if (f_24_ < f_21_) {
              f_21_ = f_24_;
              i_19_ = i_23_;
              i_20_ = i_22_;
            }
          }
        }
      }
    } else {
      for (int i_25_ = i_16_; i_25_ < i_16_ + 8; i_25_++) {
        for (int i_26_ = i_15_; i_26_ < i_15_ + 4; i_26_++) {
          f_17_ = (g729a_tables.g729a_gbk1[i_26_][0]
                  + g729a_tables.g729a_gbk2[i_25_][0]);
          f_18_ = (g729a_tables.g729a_gbk1[i_26_][1]
                  + g729a_tables.g729a_gbk2[i_25_][1]);
          float f_27_ = (f_17_ * f_17_ * f + f_18_ * f_18_ * f_4_
                  + f_17_ * f_18_ * f_6_ - f_17_ * f_3_
                  - f_18_ * f_5_);
          if (f_27_ < f_21_) {
            f_21_ = f_27_;
            i_19_ = i_26_;
            i_20_ = i_25_;
          }
        }
      }
    }
    var_g729a_encode_internal.GA = (short) g729a_tables.g729a_map1[i_19_];
    var_g729a_encode_internal.GB = (short) g729a_tables.g729a_map2[i_20_];
    var_g729a_encode_speech.past_quan_gp = var_g729a_encode_internal.gp_quan = (g729a_tables.g729a_gbk1[i_19_][0]
            + g729a_tables.g729a_gbk2[i_20_][0]);
    f_18_ = (g729a_tables.g729a_gbk1[i_19_][1]
            + g729a_tables.g729a_gbk2[i_20_][1]);
    var_g729a_encode_internal.gc_quan = f_9_ * f_18_;
    is[3] = is[2];
    is[2] = is[1];
    is[1] = is[0];
    is[0] = 16 * i_19_ + i_20_ + 1;
  }

  static void g729a_memory_update_1(g729a_encode_speech var_g729a_encode_speech,
          g729a_encode_internal var_g729a_encode_internal,
          g729a_encode_filters var_g729a_encode_filters) {
    g729a_sinthesis_filter var_g729a_sinthesis_filter = var_g729a_encode_filters.weghted_sinthesis;
    float[] fs = var_g729a_encode_speech.excitation;
    float[] fs_28_ = var_g729a_encode_internal.v;
    float[] fs_29_ = var_g729a_encode_internal.c;
    float[] fs_30_ = var_g729a_encode_internal.x;
    float[] fs_31_ = var_g729a_encode_internal.y;
    float[] fs_32_ = var_g729a_encode_internal.z;
    float[] fs_33_ = var_g729a_encode_internal.r;
    float f = var_g729a_encode_internal.gp_quan;
    float f_34_ = var_g729a_encode_internal.gc_quan;
    for (int i = -114; i < 0; i++) {
      fs[154 + i - 40] = fs[154 + i];
    }
    for (int i = 0; i < 40; i++) {
      fs[154 + i - 40] = f * fs_28_[i] + f_34_ * fs_29_[i];
    }
    for (int i = 0; i < 40; i++) {
      fs[154 + i] = fs_33_[i];
    }
    var_g729a_sinthesis_filter.delay[9] = fs_30_[30] - f * fs_31_[30] - f_34_ * fs_32_[30];
    var_g729a_sinthesis_filter.delay[8] = fs_30_[31] - f * fs_31_[31] - f_34_ * fs_32_[31];
    var_g729a_sinthesis_filter.delay[7] = fs_30_[32] - f * fs_31_[32] - f_34_ * fs_32_[32];
    var_g729a_sinthesis_filter.delay[6] = fs_30_[33] - f * fs_31_[33] - f_34_ * fs_32_[33];
    var_g729a_sinthesis_filter.delay[5] = fs_30_[34] - f * fs_31_[34] - f_34_ * fs_32_[34];
    var_g729a_sinthesis_filter.delay[4] = fs_30_[35] - f * fs_31_[35] - f_34_ * fs_32_[35];
    var_g729a_sinthesis_filter.delay[3] = fs_30_[36] - f * fs_31_[36] - f_34_ * fs_32_[36];
    var_g729a_sinthesis_filter.delay[2] = fs_30_[37] - f * fs_31_[37] - f_34_ * fs_32_[37];
    var_g729a_sinthesis_filter.delay[1] = fs_30_[38] - f * fs_31_[38] - f_34_ * fs_32_[38];
    var_g729a_sinthesis_filter.delay[0] = fs_30_[39] - f * fs_31_[39] - f_34_ * fs_32_[39];
  }

  static void g729a_memory_update_2(g729a_encode_speech var_g729a_encode_speech,
          g729a_encode_internal var_g729a_encode_internal,
          g729a_encode_filters var_g729a_encode_filters) {
    g729a_sinthesis_filter var_g729a_sinthesis_filter = var_g729a_encode_filters.weghted_sinthesis;
    float[] fs = var_g729a_encode_speech.excitation;
    float[] fs_35_ = var_g729a_encode_internal.v;
    float[] fs_36_ = var_g729a_encode_internal.c;
    float[] fs_37_ = var_g729a_encode_internal.x;
    float[] fs_38_ = var_g729a_encode_internal.y;
    float[] fs_39_ = var_g729a_encode_internal.z;
    float[] fs_40_ = var_g729a_encode_speech.weighted_speech;
    float f = var_g729a_encode_internal.gp_quan;
    float f_41_ = var_g729a_encode_internal.gc_quan;
    for (int i = 40; i < 154; i++) {
      fs[i - 40] = fs[i];
    }
    for (int i = 0; i < 40; i++) {
      fs[154 + i - 40] = f * fs_35_[i] + f_41_ * fs_36_[i];
    }
    var_g729a_sinthesis_filter.delay[9] = fs_37_[30] - f * fs_38_[30] - f_41_ * fs_39_[30];
    var_g729a_sinthesis_filter.delay[8] = fs_37_[31] - f * fs_38_[31] - f_41_ * fs_39_[31];
    var_g729a_sinthesis_filter.delay[7] = fs_37_[32] - f * fs_38_[32] - f_41_ * fs_39_[32];
    var_g729a_sinthesis_filter.delay[6] = fs_37_[33] - f * fs_38_[33] - f_41_ * fs_39_[33];
    var_g729a_sinthesis_filter.delay[5] = fs_37_[34] - f * fs_38_[34] - f_41_ * fs_39_[34];
    var_g729a_sinthesis_filter.delay[4] = fs_37_[35] - f * fs_38_[35] - f_41_ * fs_39_[35];
    var_g729a_sinthesis_filter.delay[3] = fs_37_[36] - f * fs_38_[36] - f_41_ * fs_39_[36];
    var_g729a_sinthesis_filter.delay[2] = fs_37_[37] - f * fs_38_[37] - f_41_ * fs_39_[37];
    var_g729a_sinthesis_filter.delay[1] = fs_37_[38] - f * fs_38_[38] - f_41_ * fs_39_[38];
    var_g729a_sinthesis_filter.delay[0] = fs_37_[39] - f * fs_38_[39] - f_41_ * fs_39_[39];
    for (int i = 0; i < 144; i++) {
      fs_40_[i] = fs_40_[i + 80];
    }
  }
}