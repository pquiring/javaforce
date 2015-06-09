/* g729a_qua_lsp - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_qua_lsp implements g729a_constants {

  static void g729a_qua_lsp_encode(g729a_encode_lp var_g729a_encode_lp,
          G729aCode g729acode) {
    float[] fs = new float[10];
    float[] fs_0_ = new float[10];
    float[][] fs_1_ = new float[2][10];
    float[][] fs_2_ = new float[2][10];
    float[] fs_3_ = new float[2];
    float[] fs_4_ = new float[128];
    int[] is = new int[2];
    int[] is_5_ = new int[2];
    int[] is_6_ = new int[2];
    float[] fs_7_ = new float[10];
    int i = 63;
    for (int i_8_ = 9; i_8_ >= 0; i_8_--) {
      float f;
      for (f = var_g729a_encode_lp.q[i_8_];
              g729a_tables.g729a_cos[i] < f; i--) {
        /* empty */
      }
      fs[i_8_] = ((float) i * 3.1415927F / 64.0F
              + ((g729a_tables.g729a_cos[i] - f)
              * g729a_tables.g729a_slope_acos[i]));
    }
    float f = fs[1] - 0.12566371F - 1.0F;
    if (f > 0.0F) {
      fs_0_[0] = 1.0F;
    } else {
      fs_0_[0] = 10.0F * f * f + 1.0F;
    }
    for (int i_9_ = 1; i_9_ < 9; i_9_++) {
      f = fs[i_9_ + 1] - fs[i_9_ - 1] - 1.0F;
      if (f > 0.0F) {
        fs_0_[i_9_] = 1.0F;
      } else {
        fs_0_[i_9_] = 10.0F * f * f + 1.0F;
      }
    }
    f = 2.8902655F - fs[8] - 1.0F;
    if (f > 0.0F) {
      fs_0_[9] = 1.0F;
    } else {
      fs_0_[9] = 10.0F * f * f + 1.0F;
    }
    fs_0_[4] *= 1.2F;
    fs_0_[5] *= 1.2F;
    for (i = 0; i < 2; i++) {
      float[] fs_10_ = var_g729a_encode_lp.fg_l_quant_sum;
      int i_11_ = 10 * i;
      float[] fs_12_ = g729a_tables.g729a_fg_sum;
      int i_13_ = 10 * i;
      float[] fs_14_ = g729a_tables.g729a_fg_sum_inv;
      int i_15_ = 10 * i;
      float f_16_ = (fs[0] - fs_10_[i_11_]) * fs_14_[i_15_];
      float f_17_ = (fs[1] - fs_10_[i_11_ + 1]) * fs_14_[i_15_ + 1];
      float f_18_ = (fs[2] - fs_10_[i_11_ + 2]) * fs_14_[i_15_ + 2];
      float f_19_ = (fs[3] - fs_10_[i_11_ + 3]) * fs_14_[i_15_ + 3];
      float f_20_ = (fs[4] - fs_10_[i_11_ + 4]) * fs_14_[i_15_ + 4];
      float f_21_ = (fs[5] - fs_10_[i_11_ + 5]) * fs_14_[i_15_ + 5];
      float f_22_ = (fs[6] - fs_10_[i_11_ + 6]) * fs_14_[i_15_ + 6];
      float f_23_ = (fs[7] - fs_10_[i_11_ + 7]) * fs_14_[i_15_ + 7];
      float f_24_ = (fs[8] - fs_10_[i_11_ + 8]) * fs_14_[i_15_ + 8];
      float f_25_ = (fs[9] - fs_10_[i_11_ + 9]) * fs_14_[i_15_ + 9];
      float[] fs_26_ = g729a_tables.g729a_lspcb1;
      int i_27_ = 10;
      for (int i_28_ = 1; i_28_ < 128; i_28_++) {
        float f_29_ = f_16_ - fs_26_[i_27_];
        i_27_++;
        float f_30_ = f_17_ - fs_26_[i_27_];
        i_27_++;
        float f_31_ = f_18_ - fs_26_[i_27_];
        i_27_++;
        float f_32_ = f_19_ - fs_26_[i_27_];
        i_27_++;
        float f_33_ = f_20_ - fs_26_[i_27_];
        i_27_++;
        float f_34_ = f_21_ - fs_26_[i_27_];
        i_27_++;
        float f_35_ = f_22_ - fs_26_[i_27_];
        i_27_++;
        float f_36_ = f_23_ - fs_26_[i_27_];
        i_27_++;
        float f_37_ = f_24_ - fs_26_[i_27_];
        i_27_++;
        float f_38_ = f_25_ - fs_26_[i_27_];
        i_27_++;
        fs_4_[i_28_] = (f_29_ * f_29_ + f_30_ * f_30_ + f_31_ * f_31_
                + f_32_ * f_32_ + f_33_ * f_33_ + f_34_ * f_34_
                + f_35_ * f_35_ + f_36_ * f_36_ + f_37_ * f_37_
                + f_38_ * f_38_);
      }
      int i_39_ = 0;
      fs_26_ = g729a_tables.g729a_lspcb1;
      i_27_ = 0;
      float f_40_ = f_16_ - fs_26_[i_27_];
      i_27_++;
      float f_41_ = f_17_ - fs_26_[i_27_];
      i_27_++;
      float f_42_ = f_18_ - fs_26_[i_27_];
      i_27_++;
      float f_43_ = f_19_ - fs_26_[i_27_];
      i_27_++;
      float f_44_ = f_20_ - fs_26_[i_27_];
      i_27_++;
      float f_45_ = f_21_ - fs_26_[i_27_];
      i_27_++;
      float f_46_ = f_22_ - fs_26_[i_27_];
      i_27_++;
      float f_47_ = f_23_ - fs_26_[i_27_];
      i_27_++;
      float f_48_ = f_24_ - fs_26_[i_27_];
      i_27_++;
      float f_49_ = f_25_ - fs_26_[i_27_];
      float f_50_ = (f_40_ * f_40_ + f_41_ * f_41_ + f_42_ * f_42_
              + f_43_ * f_43_ + f_44_ * f_44_ + f_45_ * f_45_
              + f_46_ * f_46_ + f_47_ * f_47_ + f_48_ * f_48_
              + f_49_ * f_49_);
      for (int i_51_ = 1; i_51_ < 128; i_51_++) {
        if (fs_4_[i_51_] < f_50_) {
          f_50_ = fs_4_[i_51_];
          i_39_ = i_51_;
        }
      }
      fs_7_[0] = f_16_;
      fs_7_[1] = f_17_;
      fs_7_[2] = f_18_;
      fs_7_[3] = f_19_;
      fs_7_[4] = f_20_;
      fs_7_[5] = f_21_;
      fs_7_[6] = f_22_;
      fs_7_[7] = f_23_;
      fs_7_[8] = f_24_;
      fs_7_[9] = f_25_;
      float f_52_ = fs_7_[0] - g729a_tables.g729a_lspcb1[10 * i_39_];
      float f_53_ = fs_7_[1] - g729a_tables.g729a_lspcb1[10 * i_39_ + 1];
      float f_54_ = fs_7_[2] - g729a_tables.g729a_lspcb1[10 * i_39_ + 2];
      float f_55_ = fs_7_[3] - g729a_tables.g729a_lspcb1[10 * i_39_ + 3];
      float f_56_ = fs_7_[4] - g729a_tables.g729a_lspcb1[10 * i_39_ + 4];
      float f_57_ = fs_0_[0];
      float f_58_ = fs_0_[1];
      float f_59_ = fs_0_[2];
      float f_60_ = fs_0_[3];
      float f_61_ = fs_0_[4];
      for (int i_62_ = 1; i_62_ < 32; i_62_++) {
        f_40_ = f_52_ - g729a_tables.g729a_lspcb2[10 * i_62_];
        f_41_ = f_53_ - g729a_tables.g729a_lspcb2[10 * i_62_ + 1];
        f_42_ = f_54_ - g729a_tables.g729a_lspcb2[10 * i_62_ + 2];
        f_43_ = f_55_ - g729a_tables.g729a_lspcb2[10 * i_62_ + 3];
        f_44_ = f_56_ - g729a_tables.g729a_lspcb2[10 * i_62_ + 4];
        fs_4_[i_62_] = (f_40_ * f_40_ * f_57_ + f_41_ * f_41_ * f_58_
                + f_42_ * f_42_ * f_59_ + f_43_ * f_43_ * f_60_
                + f_44_ * f_44_ * f_61_);
      }
      int i_63_ = 0;
      f_40_ = f_52_ - g729a_tables.g729a_lspcb2[0];
      f_41_ = f_53_ - g729a_tables.g729a_lspcb2[1];
      f_42_ = f_54_ - g729a_tables.g729a_lspcb2[2];
      f_43_ = f_55_ - g729a_tables.g729a_lspcb2[3];
      f_44_ = f_56_ - g729a_tables.g729a_lspcb2[4];
      f_50_ = (f_40_ * f_40_ * f_57_ + f_41_ * f_41_ * f_58_
              + f_42_ * f_42_ * f_59_ + f_43_ * f_43_ * f_60_
              + f_44_ * f_44_ * f_61_);
      for (int i_64_ = 1; i_64_ < 32; i_64_++) {
        if (fs_4_[i_64_] < f_50_) {
          f_50_ = fs_4_[i_64_];
          i_63_ = i_64_;
        }
      }
      f_52_ = fs_7_[5] - g729a_tables.g729a_lspcb1[10 * i_39_ + 5];
      f_53_ = fs_7_[6] - g729a_tables.g729a_lspcb1[10 * i_39_ + 6];
      f_54_ = fs_7_[7] - g729a_tables.g729a_lspcb1[10 * i_39_ + 7];
      f_55_ = fs_7_[8] - g729a_tables.g729a_lspcb1[10 * i_39_ + 8];
      f_56_ = fs_7_[9] - g729a_tables.g729a_lspcb1[10 * i_39_ + 9];
      f_57_ = fs_0_[5];
      f_58_ = fs_0_[6];
      f_59_ = fs_0_[7];
      f_60_ = fs_0_[8];
      f_61_ = fs_0_[9];
      for (int i_65_ = 1; i_65_ < 32; i_65_++) {
        f_45_ = f_52_ - g729a_tables.g729a_lspcb2[10 * i_65_ + 5];
        f_46_ = f_53_ - g729a_tables.g729a_lspcb2[10 * i_65_ + 6];
        f_47_ = f_54_ - g729a_tables.g729a_lspcb2[10 * i_65_ + 7];
        f_48_ = f_55_ - g729a_tables.g729a_lspcb2[10 * i_65_ + 8];
        f_49_ = f_56_ - g729a_tables.g729a_lspcb2[10 * i_65_ + 9];
        fs_4_[i_65_] = (f_45_ * f_45_ * f_57_ + f_46_ * f_46_ * f_58_
                + f_47_ * f_47_ * f_59_ + f_48_ * f_48_ * f_60_
                + f_49_ * f_49_ * f_61_);
      }
      int i_66_ = 0;
      f_45_ = f_52_ - g729a_tables.g729a_lspcb2[5];
      f_46_ = f_53_ - g729a_tables.g729a_lspcb2[6];
      f_47_ = f_54_ - g729a_tables.g729a_lspcb2[7];
      f_48_ = f_55_ - g729a_tables.g729a_lspcb2[8];
      f_49_ = f_56_ - g729a_tables.g729a_lspcb2[9];
      f_50_ = (f_45_ * f_45_ * f_57_ + f_46_ * f_46_ * f_58_
              + f_47_ * f_47_ * f_59_ + f_48_ * f_48_ * f_60_
              + f_49_ * f_49_ * f_61_);
      for (int i_67_ = 1; i_67_ < 32; i_67_++) {
        if (fs_4_[i_67_] < f_50_) {
          f_50_ = fs_4_[i_67_];
          i_66_ = i_67_;
        }
      }
      for (int i_68_ = 0; i_68_ < 5; i_68_++) {
        fs_1_[i][i_68_] = (g729a_tables.g729a_lspcb1[10 * i_39_ + i_68_]
                + g729a_tables.g729a_lspcb2[10 * i_63_ + i_68_]);
      }
      for (int i_69_ = 5; i_69_ < 10; i_69_++) {
        fs_1_[i][i_69_] = (g729a_tables.g729a_lspcb1[10 * i_39_ + i_69_]
                + g729a_tables.g729a_lspcb2[10 * i_66_ + i_69_]);
      }
      g729a_common.g729a_rearrange(fs_1_[i], 0.0012207031F, 10);
      g729a_common.g729a_rearrange(fs_1_[i], 6.1035156E-4F, 10);
      float f_70_ = 0.0F;
      for (int i_71_ = 0; i_71_ < 10; i_71_++) {
        f = (fs_1_[i][i_71_] - fs_7_[i_71_]) * fs_12_[i_13_ + i_71_];
        f_70_ += f * f * fs_0_[i_71_];
      }
      fs_3_[i] = f_70_;
      is[i] = i_39_;
      is_5_[i] = i_63_;
      is_6_[i] = i_66_;
    }
    int i_72_ = 0;
    if (fs_3_[0] > fs_3_[1]) {
      i_72_ = 1;
    }
    g729acode.setL0((short) i_72_);
    g729acode.setL1((short) is[i_72_]);
    g729acode.setL2((short) is_5_[i_72_]);
    g729acode.setL3((short) is_6_[i_72_]);
    float[] fs_73_ = var_g729a_encode_lp.fg_l_quant_sum;
    int i_74_ = 10 * i_72_;
    float[] fs_75_ = g729a_tables.g729a_fg_sum;
    int i_76_ = 10 * i_72_;
    g729a_common.g729a_reconstracte(fs_2_[i_72_], fs_1_[i_72_], fs_73_,
            i_74_, i_76_, 10);
    g729a_common.g729a_check_stability(fs_2_[i_72_]);
    float[] fs_77_ = var_g729a_encode_lp.l_quant_past_frame_4;
    var_g729a_encode_lp.l_quant_past_frame_4 = var_g729a_encode_lp.l_quant_past_frame_3;
    var_g729a_encode_lp.l_quant_past_frame_3 = var_g729a_encode_lp.l_quant_past_frame_2;
    var_g729a_encode_lp.l_quant_past_frame_2 = var_g729a_encode_lp.l_quant_past_frame_1;
    var_g729a_encode_lp.l_quant_past_frame_1 = fs_77_;
    for (int i_78_ = 0; i_78_ < 10; i_78_++) {
      fs_77_[i_78_] = fs_1_[i_72_][i_78_];
    }
    float[] fs_79_ = var_g729a_encode_lp.l_quant_past_frame_1;
    float[] fs_80_ = var_g729a_encode_lp.l_quant_past_frame_2;
    float[] fs_81_ = var_g729a_encode_lp.l_quant_past_frame_3;
    float[] fs_82_ = var_g729a_encode_lp.l_quant_past_frame_4;
    for (i = 0; i < 2; i++) {
      fs_73_ = var_g729a_encode_lp.fg_l_quant_sum;
      i_74_ = 10 * i;
      float[] fs_83_ = g729a_tables.g729a_fg;
      int i_84_ = 40 * i;
      for (int i_85_ = 0; i_85_ < 10; i_85_++) {
        fs_73_[i_74_ + i_85_] = (fs_83_[i_84_ + i_85_] * fs_79_[i_85_]
                + fs_83_[i_84_ + 10 + i_85_] * fs_80_[i_85_]
                + fs_83_[i_84_ + 20 + i_85_] * fs_81_[i_85_]
                + fs_83_[i_84_ + 30 + i_85_] * fs_82_[i_85_]);
      }
    }
    for (int i_86_ = 0; i_86_ < 10; i_86_++) {
      i = (int) ((double) fs_2_[i_72_][i_86_] / 0.04908738657832146);
      f = (float) ((double) ((float) i * 3.1415927F) / 64.0);
      var_g729a_encode_lp.q_quant_present_subframe_2[i_86_] = (float) ((double) g729a_tables.g729a_cos[i]
              + ((double) (g729a_tables.g729a_cos[i + 1]
              - g729a_tables.g729a_cos[i])
              * 64.0 / 3.1415927410125732
              * (double) (fs_2_[i_72_][i_86_] - f)));
    }
  }
}
