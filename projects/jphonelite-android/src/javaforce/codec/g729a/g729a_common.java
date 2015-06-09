/* g729a_common - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_common implements g729a_constants
{
    static void g729a_FIR(float[] fs, int i, float[] fs_0_, float[] fs_1_,
			  int i_2_,
			  g729a_sinthesis_filter var_g729a_sinthesis_filter) {
	float f = var_g729a_sinthesis_filter.delay[0];
	float f_3_ = var_g729a_sinthesis_filter.delay[1];
	float f_4_ = var_g729a_sinthesis_filter.delay[2];
	float f_5_ = var_g729a_sinthesis_filter.delay[3];
	float f_6_ = var_g729a_sinthesis_filter.delay[4];
	float f_7_ = var_g729a_sinthesis_filter.delay[5];
	float f_8_ = var_g729a_sinthesis_filter.delay[6];
	float f_9_ = var_g729a_sinthesis_filter.delay[7];
	float f_10_ = var_g729a_sinthesis_filter.delay[8];
	float f_11_ = var_g729a_sinthesis_filter.delay[9];
	float f_12_ = fs_1_[i_2_];
	float f_13_ = fs_1_[1 + i_2_];
	float f_14_ = fs_1_[2 + i_2_];
	float f_15_ = fs_1_[3 + i_2_];
	float f_16_ = fs_1_[4 + i_2_];
	float f_17_ = fs_1_[5 + i_2_];
	float f_18_ = fs_1_[6 + i_2_];
	float f_19_ = fs_1_[7 + i_2_];
	float f_20_ = fs_1_[8 + i_2_];
	float f_21_ = fs_1_[9 + i_2_];
	for (int i_22_ = 0; i_22_ < 40; i_22_++) {
	    float f_23_ = fs_0_[i_22_];
	    float f_24_
		= (f_23_ + f_21_ * f_11_ + f_20_ * f_10_ + f_19_ * f_9_
		   + f_18_ * f_8_ + f_17_ * f_7_ + f_16_ * f_6_ + f_15_ * f_5_
		   + f_14_ * f_4_ + f_13_ * f_3_ + f_12_ * f);
	    f_11_ = f_10_;
	    f_10_ = f_9_;
	    f_9_ = f_8_;
	    f_8_ = f_7_;
	    f_7_ = f_6_;
	    f_6_ = f_5_;
	    f_5_ = f_4_;
	    f_4_ = f_3_;
	    f_3_ = f;
	    f = f_23_;
	    fs[i_22_ + i] = f_24_;
	}
	var_g729a_sinthesis_filter.delay[0] = f;
	var_g729a_sinthesis_filter.delay[1] = f_3_;
	var_g729a_sinthesis_filter.delay[2] = f_4_;
	var_g729a_sinthesis_filter.delay[3] = f_5_;
	var_g729a_sinthesis_filter.delay[4] = f_6_;
	var_g729a_sinthesis_filter.delay[5] = f_7_;
	var_g729a_sinthesis_filter.delay[6] = f_8_;
	var_g729a_sinthesis_filter.delay[7] = f_9_;
	var_g729a_sinthesis_filter.delay[8] = f_10_;
	var_g729a_sinthesis_filter.delay[9] = f_11_;
    }
    
    static void g729a_IIR(float[] fs, float[] fs_25_, int i, float[] fs_26_,
			  g729a_sinthesis_filter var_g729a_sinthesis_filter) {
	float f = var_g729a_sinthesis_filter.delay[0];
	float f_27_ = var_g729a_sinthesis_filter.delay[1];
	float f_28_ = var_g729a_sinthesis_filter.delay[2];
	float f_29_ = var_g729a_sinthesis_filter.delay[3];
	float f_30_ = var_g729a_sinthesis_filter.delay[4];
	float f_31_ = var_g729a_sinthesis_filter.delay[5];
	float f_32_ = var_g729a_sinthesis_filter.delay[6];
	float f_33_ = var_g729a_sinthesis_filter.delay[7];
	float f_34_ = var_g729a_sinthesis_filter.delay[8];
	float f_35_ = var_g729a_sinthesis_filter.delay[9];
	float f_36_ = fs_26_[0];
	float f_37_ = fs_26_[1];
	float f_38_ = fs_26_[2];
	float f_39_ = fs_26_[3];
	float f_40_ = fs_26_[4];
	float f_41_ = fs_26_[5];
	float f_42_ = fs_26_[6];
	float f_43_ = fs_26_[7];
	float f_44_ = fs_26_[8];
	float f_45_ = fs_26_[9];
	for (int i_46_ = 0; i_46_ < 40; i_46_++) {
	    float f_47_ = fs_25_[i_46_ + i];
	    float f_48_ = (f_47_ - f_45_ * f_35_ - f_44_ * f_34_
			   - f_43_ * f_33_ - f_42_ * f_32_ - f_41_ * f_31_
			   - f_40_ * f_30_ - f_39_ * f_29_ - f_38_ * f_28_
			   - f_37_ * f_27_ - f_36_ * f);
	    f_35_ = f_34_;
	    f_34_ = f_33_;
	    f_33_ = f_32_;
	    f_32_ = f_31_;
	    f_31_ = f_30_;
	    f_30_ = f_29_;
	    f_29_ = f_28_;
	    f_28_ = f_27_;
	    f_27_ = f;
	    f = f_48_;
	    fs[i_46_] = f_48_;
	}
	var_g729a_sinthesis_filter.delay[0] = f;
	var_g729a_sinthesis_filter.delay[1] = f_27_;
	var_g729a_sinthesis_filter.delay[2] = f_28_;
	var_g729a_sinthesis_filter.delay[3] = f_29_;
	var_g729a_sinthesis_filter.delay[4] = f_30_;
	var_g729a_sinthesis_filter.delay[5] = f_31_;
	var_g729a_sinthesis_filter.delay[6] = f_32_;
	var_g729a_sinthesis_filter.delay[7] = f_33_;
	var_g729a_sinthesis_filter.delay[8] = f_34_;
	var_g729a_sinthesis_filter.delay[9] = f_35_;
    }
    
    static void g729a_IIR(float[] fs, float[] fs_49_, float[] fs_50_,
			  g729a_sinthesis_filter var_g729a_sinthesis_filter) {
	g729a_IIR(fs, fs_49_, 0, fs_50_, var_g729a_sinthesis_filter);
    }
    
    static float g729a_correl(float[] fs, int i, float[] fs_51_, int i_52_) {
	float f = 0.0F;
	for (int i_53_ = 0; i_53_ < 40; i_53_++)
	    f += fs[i_53_ + i] * fs_51_[i_53_ + i_52_];
	return f;
    }
    
    static float g729a_norma(float[] fs) {
	float f = 0.0F;
	for (int i = 0; i < 40; i++)
	    f += fs[i] * fs[i];
	return f;
    }
    
    static float g729a_norma(float[] fs, int i) {
	float f = 0.0F;
	for (int i_54_ = i; i_54_ < 40 + i; i_54_++)
	    f += fs[i_54_] * fs[i_54_];
	return f;
    }
    
    static void g729a_generation_adaptive_vector(float[] fs, float[] fs_55_,
						 int i, int i_56_) {
	int i_57_ = i < 40 ? i : 40;
	float f = fs_55_[154 - i - 1];
	float f_58_ = fs_55_[154 - i - 2];
	float f_59_ = fs_55_[154 - i - 3];
	float f_60_ = fs_55_[154 - i - 4];
	float f_61_ = fs_55_[154 - i - 5];
	float f_62_ = fs_55_[154 - i - 6];
	float f_63_ = fs_55_[154 - i - 7];
	float f_64_ = fs_55_[154 - i - 8];
	float f_65_ = fs_55_[154 - i - 9];
	float f_66_ = g729a_tables.g729a_b30[-i_56_];
	float f_67_ = g729a_tables.g729a_b30[3 - i_56_];
	float f_68_ = g729a_tables.g729a_b30[6 - i_56_];
	float f_69_ = g729a_tables.g729a_b30[9 - i_56_];
	float f_70_ = g729a_tables.g729a_b30[12 - i_56_];
	float f_71_ = g729a_tables.g729a_b30[15 - i_56_];
	float f_72_ = g729a_tables.g729a_b30[18 - i_56_];
	float f_73_ = g729a_tables.g729a_b30[21 - i_56_];
	float f_74_ = g729a_tables.g729a_b30[24 - i_56_];
	float f_75_ = g729a_tables.g729a_b30[27 - i_56_];
	for (int i_76_ = 0; i_76_ < i_57_; i_76_++) {
	    float f_77_ = fs_55_[154 + i_76_ - i];
	    float f_78_ = (f_66_ * f_77_ + f_75_ * f_65_ + f_74_ * f_64_
			   + f_73_ * f_63_ + f_72_ * f_62_ + f_71_ * f_61_
			   + f_70_ * f_60_ + f_69_ * f_59_ + f_68_ * f_58_
			   + f_67_ * f);
	    f_65_ = f_64_;
	    f_64_ = f_63_;
	    f_63_ = f_62_;
	    f_62_ = f_61_;
	    f_61_ = f_60_;
	    f_60_ = f_59_;
	    f_59_ = f_58_;
	    f_58_ = f;
	    f = f_77_;
	    fs[i_76_] = f_78_;
	}
	f = fs_55_[154 - i + 9];
	f_58_ = fs_55_[154 - i + 8];
	f_59_ = fs_55_[154 - i + 7];
	f_60_ = fs_55_[154 - i + 6];
	f_61_ = fs_55_[154 - i + 5];
	f_62_ = fs_55_[154 - i + 4];
	f_63_ = fs_55_[154 - i + 3];
	f_64_ = fs_55_[154 - i + 2];
	f_65_ = fs_55_[154 - i + 1];
	f_66_ = g729a_tables.g729a_b30[30 + i_56_];
	f_67_ = g729a_tables.g729a_b30[27 + i_56_];
	f_68_ = g729a_tables.g729a_b30[24 + i_56_];
	f_69_ = g729a_tables.g729a_b30[21 + i_56_];
	f_70_ = g729a_tables.g729a_b30[18 + i_56_];
	f_71_ = g729a_tables.g729a_b30[15 + i_56_];
	f_72_ = g729a_tables.g729a_b30[12 + i_56_];
	f_73_ = g729a_tables.g729a_b30[9 + i_56_];
	f_74_ = g729a_tables.g729a_b30[6 + i_56_];
	f_75_ = g729a_tables.g729a_b30[3 + i_56_];
	for (int i_79_ = 0; i_79_ < i_57_; i_79_++) {
	    float f_80_ = fs_55_[154 + i_79_ - i + 10];
	    float f_81_ = (fs[i_79_] + f_66_ * f_80_ + f_75_ * f_65_
			   + f_74_ * f_64_ + f_73_ * f_63_ + f_72_ * f_62_
			   + f_71_ * f_61_ + f_70_ * f_60_ + f_69_ * f_59_
			   + f_68_ * f_58_ + f_67_ * f);
	    f_65_ = f_64_;
	    f_64_ = f_63_;
	    f_63_ = f_62_;
	    f_62_ = f_61_;
	    f_61_ = f_60_;
	    f_60_ = f_59_;
	    f_59_ = f_58_;
	    f_58_ = f;
	    f = f_80_;
	    fs[i_79_] = f_81_;
	    fs_55_[154 + i_79_] = f_81_;
	}
	for (int i_82_ = i_57_; i_82_ < 40; i_82_++) {
	    float f_83_ = 0.0F;
	    for (int i_84_ = 0; i_84_ < 10; i_84_++)
		f_83_ += ((fs_55_[154 + i_82_ - i - i_84_]
			   * g729a_tables.g729a_b30[3 * i_84_ - i_56_])
			  + (fs_55_[154 + i_82_ - i + 1 + i_84_]
			     * g729a_tables.g729a_b30[3 + i_56_ + 3 * i_84_]));
	    fs[i_82_] = f_83_;
	    fs_55_[154 + i_82_] = f_83_;
	}
    }
    
    static void g729a_rearrange(float[] fs, float f, int i) {
	for (int i_85_ = 1; i_85_ < i; i_85_++) {
	    if (fs[i_85_ - 1] > fs[i_85_] - f) {
		float f_86_ = fs[i_85_ - 1];
		float f_87_ = fs[i_85_];
		fs[i_85_ - 1] = (f_87_ + f_86_ - f) * 0.5F;
		fs[i_85_] = (f_87_ + f_86_ + f) * 0.5F;
	    }
	}
    }
    
    static void g729a_reconstracte(float[] fs, float[] fs_88_, float[] fs_89_,
				   int i, int i_90_, int i_91_) {
	for (int i_92_ = 0; i_92_ < i_91_; i_92_++)
	    fs[i_92_]
		= (g729a_tables.g729a_fg_sum[i_92_ + i_90_] * fs_88_[i_92_]
		   + fs_89_[i_92_ + i]);
    }
    
    static void g729a_check_stability(float[] fs) {
	float f = fs[0];
	float f_93_ = fs[1];
	float f_94_ = fs[2];
	float f_95_ = fs[3];
	float f_96_ = fs[4];
	float f_97_ = fs[5];
	float f_98_ = fs[6];
	float f_99_ = fs[7];
	float f_100_ = fs[8];
	float f_101_ = fs[9];
	if (f > f_93_) {
	    float f_102_ = f;
	    f = f_93_;
	    f_93_ = f_102_;
	}
	if (f_94_ > f_95_) {
	    float f_103_ = f_94_;
	    f_94_ = f_95_;
	    f_95_ = f_103_;
	}
	if (f_96_ > f_97_) {
	    float f_104_ = f_96_;
	    f_96_ = f_97_;
	    f_97_ = f_104_;
	}
	if (f_98_ > f_99_) {
	    float f_105_ = f_98_;
	    f_98_ = f_99_;
	    f_99_ = f_105_;
	}
	if (f_100_ > f_101_) {
	    float f_106_ = f_100_;
	    f_100_ = f_101_;
	    f_101_ = f_106_;
	}
	if (f > f_94_) {
	    float f_107_ = f;
	    f = f_94_;
	    f_94_ = f_107_;
	}
	if (f > f_96_) {
	    float f_108_ = f;
	    f = f_96_;
	    f_96_ = f_108_;
	}
	if (f > f_98_) {
	    float f_109_ = f;
	    f = f_98_;
	    f_98_ = f_109_;
	}
	if (f > f_100_) {
	    float f_110_ = f;
	    f = f_100_;
	    f_100_ = f_110_;
	}
	if (f_93_ > f_101_) {
	    float f_111_ = f_93_;
	    f_93_ = f_101_;
	    f_101_ = f_111_;
	}
	if (f_95_ > f_101_) {
	    float f_112_ = f_95_;
	    f_95_ = f_101_;
	    f_101_ = f_112_;
	}
	if (f_97_ > f_101_) {
	    float f_113_ = f_97_;
	    f_97_ = f_101_;
	    f_101_ = f_113_;
	}
	if (f_99_ > f_101_) {
	    float f_114_ = f_99_;
	    f_99_ = f_101_;
	    f_101_ = f_114_;
	}
	if (f_93_ > f_94_) {
	    float f_115_ = f_93_;
	    f_93_ = f_94_;
	    f_94_ = f_115_;
	}
	if (f_95_ > f_96_) {
	    float f_116_ = f_95_;
	    f_95_ = f_96_;
	    f_96_ = f_116_;
	}
	if (f_97_ > f_98_) {
	    float f_117_ = f_97_;
	    f_97_ = f_98_;
	    f_98_ = f_117_;
	}
	if (f_99_ > f_100_) {
	    float f_118_ = f_99_;
	    f_99_ = f_100_;
	    f_100_ = f_118_;
	}
	if (f_93_ > f_95_) {
	    float f_119_ = f_93_;
	    f_93_ = f_95_;
	    f_95_ = f_119_;
	}
	if (f_93_ > f_97_) {
	    float f_120_ = f_93_;
	    f_93_ = f_97_;
	    f_97_ = f_120_;
	}
	if (f_93_ > f_99_) {
	    float f_121_ = f_93_;
	    f_93_ = f_99_;
	    f_99_ = f_121_;
	}
	if (f_94_ > f_100_) {
	    float f_122_ = f_94_;
	    f_94_ = f_100_;
	    f_100_ = f_122_;
	}
	if (f_96_ > f_100_) {
	    float f_123_ = f_96_;
	    f_96_ = f_100_;
	    f_100_ = f_123_;
	}
	if (f_98_ > f_100_) {
	    float f_124_ = f_98_;
	    f_98_ = f_100_;
	    f_100_ = f_124_;
	}
	if (f_94_ > f_95_) {
	    float f_125_ = f_94_;
	    f_94_ = f_95_;
	    f_95_ = f_125_;
	}
	if (f_96_ > f_97_) {
	    float f_126_ = f_96_;
	    f_96_ = f_97_;
	    f_97_ = f_126_;
	}
	if (f_98_ > f_99_) {
	    float f_127_ = f_98_;
	    f_98_ = f_99_;
	    f_99_ = f_127_;
	}
	if (f_94_ > f_96_) {
	    float f_128_ = f_94_;
	    f_94_ = f_96_;
	    f_96_ = f_128_;
	}
	if (f_94_ > f_98_) {
	    float f_129_ = f_94_;
	    f_94_ = f_98_;
	    f_98_ = f_129_;
	}
	if (f_95_ > f_99_) {
	    float f_130_ = f_95_;
	    f_95_ = f_99_;
	    f_99_ = f_130_;
	}
	if (f_97_ > f_99_) {
	    float f_131_ = f_97_;
	    f_97_ = f_99_;
	    f_99_ = f_131_;
	}
	if (f_95_ > f_96_) {
	    float f_132_ = f_95_;
	    f_95_ = f_96_;
	    f_96_ = f_132_;
	}
	if (f_97_ > f_98_) {
	    float f_133_ = f_97_;
	    f_97_ = f_98_;
	    f_98_ = f_133_;
	}
	if (f_95_ > f_97_) {
	    float f_134_ = f_95_;
	    f_95_ = f_97_;
	    f_97_ = f_134_;
	}
	if (f_96_ > f_98_) {
	    float f_135_ = f_96_;
	    f_96_ = f_98_;
	    f_98_ = f_135_;
	}
	if (f_96_ > f_97_) {
	    float f_136_ = f_96_;
	    f_96_ = f_97_;
	    f_97_ = f_136_;
	}
	if (f < 0.0050F)
	    f = 0.0050F;
	if (f_93_ - f < 0.0391F)
	    f_93_ = f + 0.0391F;
	if (f_94_ - f_93_ < 0.0391F)
	    f_94_ = f_93_ + 0.0391F;
	if (f_95_ - f_94_ < 0.0391F)
	    f_95_ = f_94_ + 0.0391F;
	if (f_96_ - f_95_ < 0.0391F)
	    f_96_ = f_95_ + 0.0391F;
	if (f_97_ - f_96_ < 0.0391F)
	    f_97_ = f_96_ + 0.0391F;
	if (f_98_ - f_97_ < 0.0391F)
	    f_98_ = f_97_ + 0.0391F;
	if (f_99_ - f_98_ < 0.0391F)
	    f_99_ = f_98_ + 0.0391F;
	if (f_100_ - f_99_ < 0.0391F)
	    f_100_ = f_99_ + 0.0391F;
	if (f_101_ - f_100_ < 0.0391F)
	    f_101_ = f_100_ + 0.0391F;
	if (f_101_ > 3.135F)
	    f_101_ = 3.135F;
	fs[0] = f;
	fs[1] = f_93_;
	fs[2] = f_94_;
	fs[3] = f_95_;
	fs[4] = f_96_;
	fs[5] = f_97_;
	fs[6] = f_98_;
	fs[7] = f_99_;
	fs[8] = f_100_;
	fs[9] = f_101_;
    }
    
    static void g729a_lsp_interpolation(float[] fs, float[] fs_137_,
					float[] fs_138_) {
	for (int i = 0; i < 10; i++)
	    fs[i] = (fs_137_[i] + fs_138_[i]) * 0.5F;
    }
    
    static void g729a_LSP2LP(float[] fs, float[] fs_139_) {
	float f = -2.0F * fs_139_[0];
	float f_140_ = -2.0F * (fs_139_[2] * f - 1.0F);
	f -= 2.0F * fs_139_[2];
	float f_141_ = -2.0F * (fs_139_[4] * f_140_ - f);
	f_140_ = f_140_ - 2.0F * fs_139_[4] * f + 1.0F;
	f -= 2.0F * fs_139_[4];
	float f_142_ = -2.0F * (fs_139_[6] * f_141_ - f_140_);
	f_141_ = f_141_ - 2.0F * fs_139_[6] * f_140_ + f;
	f_140_ = f_140_ - 2.0F * fs_139_[6] * f + 1.0F;
	f -= 2.0F * fs_139_[6];
	float f_143_ = -2.0F * (fs_139_[8] * f_142_ - f_141_);
	f_142_ = f_142_ - 2.0F * fs_139_[8] * f_141_ + f_140_;
	f_141_ = f_141_ - 2.0F * fs_139_[8] * f_140_ + f;
	f_140_ = f_140_ - 2.0F * fs_139_[8] * f + 1.0F;
	f -= 2.0F * fs_139_[8];
	float f_144_ = -2.0F * fs_139_[1];
	float f_145_ = -2.0F * (fs_139_[3] * f_144_ - 1.0F);
	f_144_ -= 2.0F * fs_139_[3];
	float f_146_ = -2.0F * (fs_139_[5] * f_145_ - f_144_);
	f_145_ = f_145_ - 2.0F * fs_139_[5] * f_144_ + 1.0F;
	f_144_ -= 2.0F * fs_139_[5];
	float f_147_ = -2.0F * (fs_139_[7] * f_146_ - f_145_);
	f_146_ = f_146_ - 2.0F * fs_139_[7] * f_145_ + f_144_;
	f_145_ = f_145_ - 2.0F * fs_139_[7] * f_144_ + 1.0F;
	f_144_ -= 2.0F * fs_139_[7];
	float f_148_ = -2.0F * (fs_139_[9] * f_147_ - f_146_);
	f_147_ = f_147_ - 2.0F * fs_139_[9] * f_146_ + f_145_;
	f_146_ = f_146_ - 2.0F * fs_139_[9] * f_145_ + f_144_;
	f_145_ = f_145_ - 2.0F * fs_139_[9] * f_144_ + 1.0F;
	f_144_ -= 2.0F * fs_139_[9];
	float f_149_ = f + 1.0F;
	float f_150_ = f_140_ + f;
	float f_151_ = f_141_ + f_140_;
	float f_152_ = f_142_ + f_141_;
	float f_153_ = f_143_ + f_142_;
	float f_154_ = f_144_ - 1.0F;
	float f_155_ = f_145_ - f_144_;
	float f_156_ = f_146_ - f_145_;
	float f_157_ = f_147_ - f_146_;
	float f_158_ = f_148_ - f_147_;
	fs[0] = 0.5F * (f_149_ + f_154_);
	fs[1] = 0.5F * (f_150_ + f_155_);
	fs[2] = 0.5F * (f_151_ + f_156_);
	fs[3] = 0.5F * (f_152_ + f_157_);
	fs[4] = 0.5F * (f_153_ + f_158_);
	fs[5] = 0.5F * (f_153_ - f_158_);
	fs[6] = 0.5F * (f_152_ - f_157_);
	fs[7] = 0.5F * (f_151_ - f_156_);
	fs[8] = 0.5F * (f_150_ - f_155_);
	fs[9] = 0.5F * (f_149_ - f_154_);
    }
}