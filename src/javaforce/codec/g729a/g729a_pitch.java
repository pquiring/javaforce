/* g729a_pitch - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_pitch implements g729a_constants {

  static int g729a_opla(float[] fs) {
    float[] fs_0_ = new float[40];
    g729a_utils.g729a_corel_10(fs_0_, 0, fs, -2);
    g729a_utils.g729a_corel_10(fs_0_, 10, fs, -22);
    g729a_utils.g729a_corel_10(fs_0_, 20, fs, -42);
    float f = 0.0F;
    float f_1_ = 0.0F;
    float f_2_ = fs[62];
    for (int i = 0; i < 80; i += 2) {
      float f_3_ = fs[64 + i];
      float f_4_ = fs[144 + i];
      f += f_4_ * f_2_;
      f_1_ += f_4_ * f_3_;
      f_2_ = f_3_;
    }
    fs_0_[30] = f;
    fs_0_[31] = f_1_;
    float f_5_ = fs_0_[31];
    int i = 31;
    for (int i_6_ = 30; i_6_ >= 0; i_6_--) {
      if (fs_0_[i_6_] > f_5_ + 3.0517578E-5F) {
        f_5_ = fs_0_[i_6_];
        i = i_6_;
      }
    }
    i = 142 - i * 2;
    int i_7_ = i;
    f = 0.0F;
    f_1_ = 0.0F;
    f_2_ = fs[144 - i_7_ - 1];
    for (int i_8_ = 0; i_8_ < 80; i_8_ += 2) {
      float f_9_ = fs[144 - i_7_ + 1 + i_8_];
      float f_10_ = fs[144 + i_8_];
      f += f_10_ * f_2_;
      f_1_ += f_10_ * f_9_;
      f_2_ = f_9_;
    }
    if (f > f_5_ + 3.0517578E-5F) {
      f_5_ = f;
      i = i_7_ + 1;
    }
    if (f_1_ > f_5_ + 3.0517578E-5F) {
      f_5_ = f_1_;
      i = i_7_ - 1;
    }
    g729a_utils.g729a_corel_10(fs_0_, 0, fs, -66);
    g729a_utils.g729a_corel_10(fs_0_, 10, fs, -86);
    g729a_utils.g729a_corel_10(fs_0_, 20, fs, -65);
    g729a_utils.g729a_corel_10(fs_0_, 30, fs, -85);
    float f_11_ = fs_0_[19];
    int i_12_ = 19;
    for (int i_13_ = 18; i_13_ >= 0; i_13_--) {
      if (fs_0_[i_13_] > f_11_ + 3.0517578E-5F) {
        f_11_ = fs_0_[i_13_];
        i_12_ = i_13_;
      }
    }
    for (int i_14_ = 39; i_14_ >= 20; i_14_--) {
      if (fs_0_[i_14_] > f_11_ + 3.0517578E-5F) {
        f_11_ = fs_0_[i_14_];
        i_12_ = i_14_;
      }
    }
    if (i_12_ >= 20) {
      i_12_ = 119 - i_12_ * 2;
    } else {
      i_12_ = 78 - i_12_ * 2;
    }
    g729a_utils.g729a_corel_10(fs_0_, 0, fs, -106);
    g729a_utils.g729a_corel_10(fs_0_, 10, fs, -105);
    float f_15_ = fs_0_[9];
    int i_16_ = 9;
    for (int i_17_ = 8; i_17_ >= 0; i_17_--) {
      if (fs_0_[i_17_] > f_15_ + 3.0517578E-5F) {
        f_15_ = fs_0_[i_17_];
        i_16_ = i_17_;
      }
    }
    for (int i_18_ = 19; i_18_ >= 10; i_18_--) {
      if (fs_0_[i_18_] > f_15_ + 3.0517578E-5F) {
        f_15_ = fs_0_[i_18_];
        i_16_ = i_18_;
      }
    }
    if (i_16_ >= 10) {
      i_16_ = 59 - i_16_ * 2;
    } else {
      i_16_ = 38 - i_16_ * 2;
    }
    float f_19_ = 0.0F;
    for (int i_20_ = 0; i_20_ < 40; i_20_++) {
      f_19_ += fs[144 + 2 * i_20_ - i] * fs[144 + 2 * i_20_ - i];
    }
    if (f_19_ == 0.0F) {
      f_5_ = 0.0F;
    } else {
      f_5_ /= Math.sqrt((double) f_19_);
    }
    f_19_ = 0.0F;
    for (int i_21_ = 0; i_21_ < 40; i_21_++) {
      f_19_ += fs[144 + 2 * i_21_ - i_12_] * fs[144 + 2 * i_21_ - i_12_];
    }
    if (f_19_ == 0.0F) {
      f_11_ = 0.0F;
    } else {
      f_11_ /= Math.sqrt((double) f_19_);
    }
    f_19_ = 0.0F;
    for (int i_22_ = 0; i_22_ < 40; i_22_++) {
      f_19_ += fs[144 + 2 * i_22_ - i_16_] * fs[144 + 2 * i_22_ - i_16_];
    }
    if (f_19_ == 0.0F) {
      f_15_ = 0.0F;
    } else {
      f_15_ /= Math.sqrt((double) f_19_);
    }
    if (Math.abs((float) (i_12_ * 2 - i)) < 5.0F) {
      f_11_ += (double) f_5_ * 0.25;
    }
    if (Math.abs((float) (i_12_ * 3 - i)) < 7.0F) {
      f_11_ += (double) f_5_ * 0.25;
    }
    if (Math.abs((float) (i_16_ * 2 - i_12_)) < 5.0F) {
      f_15_ += (double) f_11_ * 0.2;
    }
    if (Math.abs((float) (i_16_ * 3 - i_12_)) < 7.0F) {
      f_15_ += (double) f_11_ * 0.2;
    }
    if (f_15_ < f_11_) {
      f_15_ = f_11_;
      i_16_ = i_12_;
    }
    if (f_15_ < f_5_) {
      i_16_ = i;
    }
    int i_23_ = i_16_;
    return i_23_;
  }

  static void g729a_impulse_response(g729a_encode_internal var_g729a_encode_internal, float[] fs) {
    float[] fs_24_ = var_g729a_encode_internal.h;
    float[] fs_25_ = var_g729a_encode_internal.a_gamma;
    float f = fs_25_[0] = 0.75F * fs[0];
    float f_26_ = fs_25_[1] = 0.5625F * fs[1];
    float f_27_ = fs_25_[2] = 0.421875F * fs[2];
    float f_28_ = fs_25_[3] = 0.31640625F * fs[3];
    float f_29_ = fs_25_[4] = 0.23730469F * fs[4];
    float f_30_ = fs_25_[5] = 0.17797852F * fs[5];
    float f_31_ = fs_25_[6] = 0.13348389F * fs[6];
    float f_32_ = fs_25_[7] = 0.100112915F * fs[7];
    float f_33_ = fs_25_[8] = 0.07508469F * fs[8];
    float f_34_ = fs_25_[9] = 0.056313515F * fs[9];
    fs_24_[0] = 1.0F;
    float f_35_ = 1.0F;
    float f_36_ = 0.0F;
    float f_37_ = 0.0F;
    float f_38_ = 0.0F;
    float f_39_ = 0.0F;
    float f_40_ = 0.0F;
    float f_41_ = 0.0F;
    float f_42_ = 0.0F;
    float f_43_ = 0.0F;
    float f_44_ = 0.0F;
    for (int i = 1; i < 40; i++) {
      float f_45_ = (-f_34_ * f_44_ - f_33_ * f_43_ - f_32_ * f_42_
              - f_31_ * f_41_ - f_30_ * f_40_ - f_29_ * f_39_
              - f_28_ * f_38_ - f_27_ * f_37_ - f_26_ * f_36_
              - f * f_35_);
      f_44_ = f_43_;
      f_43_ = f_42_;
      f_42_ = f_41_;
      f_41_ = f_40_;
      f_40_ = f_39_;
      f_39_ = f_38_;
      f_38_ = f_37_;
      f_37_ = f_36_;
      f_36_ = f_35_;
      f_35_ = f_45_;
      fs_24_[i] = f_45_;
    }
  }

  static void g729a_target(g729a_encode_internal var_g729a_encode_internal,
          g729a_encode_speech var_g729a_encode_speech,
          g729a_encode_filters var_g729a_encode_filters) {
    float[] fs = var_g729a_encode_internal.x;
    float[] fs_46_ = var_g729a_encode_speech.excitation;
    g729a_sinthesis_filter var_g729a_sinthesis_filter = var_g729a_encode_filters.weghted_sinthesis;
    g729a_common.g729a_IIR(fs, fs_46_, 154,
            var_g729a_encode_internal.a_gamma,
            var_g729a_sinthesis_filter);
  }

  static void g729a_ad_cod_search_1(G729aCode g729acode, g729a_encode_internal var_g729a_encode_internal,
          g729a_encode_speech var_g729a_encode_speech, float[][] fs) {
    float[] fs_47_ = var_g729a_encode_speech.excitation;
    float[] fs_48_ = var_g729a_encode_internal.x;
    float[] fs_49_ = new float[40];
    int i = var_g729a_encode_internal.T_opt - 3;
    if (i < 20) {
      i = 20;
    }
    int i_50_ = i + 6;
    if (i_50_ > 143) {
      i_50_ = 143;
      i = i_50_ - 6;
    }
    g729a_backward(fs_49_, fs_48_, var_g729a_encode_internal.a_gamma);
    float f = 0.0F;
    int i_51_ = i;
    for (int i_52_ = 0; i_52_ < 40; i_52_++) {
      f += fs_49_[i_52_] * fs_47_[154 + i_52_ - i];
    }
    for (int i_53_ = i + 1; i_53_ <= i_50_; i_53_++) {
      float f_54_ = 0.0F;
      for (int i_55_ = 0; i_55_ < 40; i_55_++) {
        f_54_ += fs_49_[i_55_] * fs_47_[154 + i_55_ - i_53_];
      }
      if (f_54_ > f + 3.0517578E-5F) {
        f = f_54_;
        i_51_ = i_53_;
      }
    }
    int i_56_;
    int i_57_;
    if (i_51_ < 85) {
      g729a_common.g729a_generation_adaptive_vector(fs[1], fs_47_, i_51_,
              0);
      i_56_ = 0;
      f = 0.0F;
      for (int i_58_ = 0; i_58_ < 40; i_58_++) {
        f += fs_49_[i_58_] * fs[1][i_58_];
      }
      g729a_common.g729a_generation_adaptive_vector(fs[0], fs_47_, i_51_,
              -1);
      float f_59_ = 0.0F;
      for (int i_60_ = 0; i_60_ < 40; i_60_++) {
        f_59_ += fs_49_[i_60_] * fs[0][i_60_];
      }
      if (f_59_ > f + 3.0517578E-5F) {
        i_56_ = -1;
        f = f_59_;
      }
      g729a_common.g729a_generation_adaptive_vector(fs[2], fs_47_,
              i_51_ + 1, -2);
      f_59_ = 0.0F;
      for (int i_61_ = 0; i_61_ < 40; i_61_++) {
        f_59_ += fs_49_[i_61_] * fs[2][i_61_];
      }
      if (f_59_ > f + 3.0517578E-5F) {
        i_56_ = 1;
      }
      i_57_ = 3 * (i_51_ - 19) + i_56_ - 1;
    } else {
      i_56_ = 0;
      g729a_common.g729a_generation_adaptive_vector(fs[1], fs_47_, i_51_,
              0);
      i_57_ = i_51_ - 85 + 197;
    }
    g729acode.setP0((short) (i_57_ >> 2 & 0x1 ^ i_57_ >> 3 & 0x1
            ^ i_57_ >> 4 & 0x1 ^ i_57_ >> 5 & 0x1
            ^ i_57_ >> 6 & 0x1 ^ i_57_ >> 7 & 0x1 ^ 0x1));
    g729acode.setP1((short) i_57_);
    var_g729a_encode_internal.T1 = i_51_;
    var_g729a_encode_internal.frac_T1 = i_56_;
    var_g729a_encode_internal.v = fs[i_56_ + 1];
  }

  static void g729a_ad_cod_search_2(G729aCode g729acode, g729a_encode_internal var_g729a_encode_internal,
          g729a_encode_speech var_g729a_encode_speech, float[][] fs) {
    float[] fs_62_ = var_g729a_encode_speech.excitation;
    float[] fs_63_ = var_g729a_encode_internal.x;
    float[] fs_64_ = new float[40];
    int i = var_g729a_encode_internal.T1;
    int i_65_ = i - 5;
    if (i_65_ < 20) {
      i_65_ = 20;
    }
    int i_66_ = i_65_ + 9;
    if (i_66_ > 143) {
      i_66_ = 143;
      i_65_ = i_66_ - 9;
    }
    g729a_backward(fs_64_, fs_63_, var_g729a_encode_internal.a_gamma);
    float f = 0.0F;
    int i_67_ = i_65_;
    for (int i_68_ = 0; i_68_ < 40; i_68_++) {
      f += fs_64_[i_68_] * fs_62_[154 + i_68_ - i_65_];
    }
    for (int i_69_ = i_65_ + 1; i_69_ <= i_66_; i_69_++) {
      float f_70_ = 0.0F;
      for (int i_71_ = 0; i_71_ < 40; i_71_++) {
        f_70_ += fs_64_[i_71_] * fs_62_[154 + i_71_ - i_69_];
      }
      if (f_70_ > f + 3.0517578E-5F) {
        f = f_70_;
        i_67_ = i_69_;
      }
    }
    g729a_common.g729a_generation_adaptive_vector(fs[1], fs_62_, i_67_, 0);
    int i_72_ = 0;
    f = 0.0F;
    for (int i_73_ = 0; i_73_ < 40; i_73_++) {
      f += fs_64_[i_73_] * fs[1][i_73_];
    }
    g729a_common.g729a_generation_adaptive_vector(fs[0], fs_62_, i_67_,
            -1);
    float f_74_ = 0.0F;
    for (int i_75_ = 0; i_75_ < 40; i_75_++) {
      f_74_ += fs_64_[i_75_] * fs[0][i_75_];
    }
    if (f_74_ > f + 3.0517578E-5F) {
      i_72_ = -1;
      f = f_74_;
    }
    g729a_common.g729a_generation_adaptive_vector(fs[2], fs_62_, i_67_ + 1,
            -2);
    f_74_ = 0.0F;
    for (int i_76_ = 0; i_76_ < 40; i_76_++) {
      f_74_ += fs_64_[i_76_] * fs[2][i_76_];
    }
    if (f_74_ > f + 3.0517578E-5F) {
      i_72_ = 1;
    }
    g729acode.setP2((short) (3 * (i_67_ - i_65_) + i_72_ + 2));
    var_g729a_encode_internal.T2 = i_67_;
    var_g729a_encode_internal.frac_T2 = i_72_;
    var_g729a_encode_internal.v = fs[i_72_ + 1];
  }

  static void g729a_backward(float[] fs, float[] fs_77_, float[] fs_78_) {
    float[] fs_79_ = new float[40];
    float[] fs_80_ = new float[40];
    g729a_sinthesis_filter var_g729a_sinthesis_filter = new g729a_sinthesis_filter();
    for (int i = 0; i < 40; i++) {
      fs_79_[i] = fs_77_[39 - i];
    }
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter.delay, 10);
    g729a_common.g729a_IIR(fs_80_, fs_79_, fs_78_,
            var_g729a_sinthesis_filter);
    for (int i = 0; i < 40; i++) {
      fs[i] = fs_80_[39 - i];
    }
  }

  static void g729a_adcb_gain(g729a_encode_internal var_g729a_encode_internal) {
    float[] fs = var_g729a_encode_internal.y;
    float[] fs_81_ = var_g729a_encode_internal.x;
    float[] fs_82_ = var_g729a_encode_internal.v;
    g729a_sinthesis_filter var_g729a_sinthesis_filter = new g729a_sinthesis_filter();
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter.delay, 10);
    g729a_common.g729a_IIR(fs, fs_82_, var_g729a_encode_internal.a_gamma,
            var_g729a_sinthesis_filter);
    float f;
    var_g729a_encode_internal.xy = f = g729a_common.g729a_correl(fs_81_, 0, fs, 0);
    float f_83_;
    var_g729a_encode_internal.yy = f_83_ = g729a_common.g729a_norma(fs);
    float f_84_;
    if (f_83_ == 0.0F) {
      f_84_ = 0.0F;
    } else {
      f_84_ = f / f_83_;
    }
    if (f_84_ < 0.0F) {
      f_84_ = 0.0F;
    }
    if (f_84_ > 1.2000122F) {
      f_84_ = 1.2000122F;
    }
    var_g729a_encode_internal.gp = f_84_;
  }

  static int g729a_test_err(float[] fs, int i, int i_85_) {
    if (i_85_ == 1) {
      i++;
    }
    int i_86_ = i - 50;
    if (i_86_ < 0) {
      i_86_ = 0;
    }
    int i_87_ = g729a_tables.g729a_tab_zone[i_86_];
    int i_88_ = g729a_tables.g729a_tab_zone[i + 10 - 2];
    float f = -1.0F;
    int i_89_ = 0;
    for (i_86_ = i_88_; i_86_ >= i_87_; i_86_--) {
      if (fs[i_86_] > f) {
        f = fs[i_86_];
      }
    }
    if (f > 60000.0F) {
      i_89_ = 1;
    }
    return i_89_;
  }

  static void g729a_update_exc_err(float[] fs, float f, int i) {
    float f_90_ = -1.0F;
    if (i < 40) {
      float f_91_ = f * fs[0] + 1.0F;
      if (f_91_ > f_90_) {
        f_90_ = f_91_;
      }
      f_91_ = f * f_91_ + 1.0F;
      if (f_91_ > f_90_) {
        f_90_ = f_91_;
      }
    } else {
      int i_92_ = g729a_tables.g729a_tab_zone[i - 40];
      int i_93_ = g729a_tables.g729a_tab_zone[i - 1];
      for (int i_94_ = i_92_; i_94_ <= i_93_; i_94_++) {
        float f_95_ = f * fs[i_94_] + 1.0F;
        if (f_95_ > f_90_) {
          f_90_ = f_95_;
        }
      }
    }
    for (int i_96_ = 3; i_96_ >= 1; i_96_--) {
      fs[i_96_] = fs[i_96_ - 1];
    }
    fs[0] = f_90_;
  }
}