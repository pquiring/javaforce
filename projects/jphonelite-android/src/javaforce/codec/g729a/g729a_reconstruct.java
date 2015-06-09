/* g729a_reconstruct - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_reconstruct implements g729a_constants
{
    static void g729a_lspdec(g729a_decode_lp var_g729a_decode_lp,
			     G729aCode g729acode) {
	int i = g729acode.getL0();
	int i_0_ = g729acode.getL1();
	int i_1_ = g729acode.getL2();
	int i_2_ = g729acode.getL3();
	float[] fs = new float[10];
	float[] fs_3_ = new float[10];
	var_g729a_decode_lp.past_MA = i;
	float[] fs_4_ = var_g729a_decode_lp.fg_l_quant_sum;
	int i_5_ = 10 * i;
	float[] fs_6_ = g729a_tables.g729a_fg_sum;
	int i_7_ = 10 * i;
	for (int i_8_ = 0; i_8_ < 5; i_8_++)
	    fs[i_8_] = (g729a_tables.g729a_lspcb1[10 * i_0_ + i_8_]
			+ g729a_tables.g729a_lspcb2[10 * i_1_ + i_8_]);
	for (int i_9_ = 5; i_9_ < 10; i_9_++)
	    fs[i_9_] = (g729a_tables.g729a_lspcb1[10 * i_0_ + i_9_]
			+ g729a_tables.g729a_lspcb2[10 * i_2_ + i_9_]);
	g729a_common.g729a_rearrange(fs, 0.0012207031F, 10);
	g729a_common.g729a_rearrange(fs, 6.1035156E-4F, 10);
	g729a_common.g729a_reconstracte(fs_3_, fs, fs_4_, i_5_, i_7_, 10);
	g729a_common.g729a_check_stability(fs_3_);
	float[] fs_10_ = var_g729a_decode_lp.l_quant_past_frame_4;
	var_g729a_decode_lp.l_quant_past_frame_4
	    = var_g729a_decode_lp.l_quant_past_frame_3;
	var_g729a_decode_lp.l_quant_past_frame_3
	    = var_g729a_decode_lp.l_quant_past_frame_2;
	var_g729a_decode_lp.l_quant_past_frame_2
	    = var_g729a_decode_lp.l_quant_past_frame_1;
	var_g729a_decode_lp.l_quant_past_frame_1 = fs_10_;
	for (int i_11_ = 0; i_11_ < 10; i_11_++)
	    fs_10_[i_11_] = fs[i_11_];
	float[] fs_12_ = var_g729a_decode_lp.l_quant_past_frame_1;
	float[] fs_13_ = var_g729a_decode_lp.l_quant_past_frame_2;
	float[] fs_14_ = var_g729a_decode_lp.l_quant_past_frame_3;
	float[] fs_15_ = var_g729a_decode_lp.l_quant_past_frame_4;
	for (int i_16_ = 0; i_16_ < 2; i_16_++) {
	    fs_4_ = var_g729a_decode_lp.fg_l_quant_sum;
	    i_5_ = 10 * i_16_;
	    float[] fs_17_ = g729a_tables.g729a_fg;
	    int i_18_ = 40 * i_16_;
	    for (int i_19_ = 0; i_19_ < 10; i_19_++)
		fs_4_[i_5_ + i_19_]
		    = (fs_17_[i_18_ + i_19_] * fs_12_[i_19_]
		       + fs_17_[i_18_ + 10 + i_19_] * fs_13_[i_19_]
		       + fs_17_[i_18_ + 20 + i_19_] * fs_14_[i_19_]
		       + fs_17_[i_18_ + 30 + i_19_] * fs_15_[i_19_]);
	}
	for (int i_20_ = 0; i_20_ < 10; i_20_++)
	    var_g729a_decode_lp.past_omega[i_20_] = fs_3_[i_20_];
	for (int i_21_ = 0; i_21_ < 10; i_21_++)
	    var_g729a_decode_lp.q_quant_present_subframe_2[i_21_]
		= (float) Math.cos((double) fs_3_[i_21_]);
    }

    static void g729a_lspdec_erased(g729a_decode_lp var_g729a_decode_lp) {
	float[] fs = new float[10];
	float[] fs_22_ = new float[10];
	float[] fs_23_ = var_g729a_decode_lp.fg_l_quant_sum;
	int i = 10 * var_g729a_decode_lp.past_MA;
	float[] fs_24_ = g729a_tables.g729a_fg_sum;
	int i_25_ = 10 * var_g729a_decode_lp.past_MA;
	float[] fs_26_ = g729a_tables.g729a_fg_sum_inv;
	int i_27_ = 10 * var_g729a_decode_lp.past_MA;
	float[] fs_28_ = var_g729a_decode_lp.past_omega;
	for (int i_29_ = 0; i_29_ < 10; i_29_++)
	    fs[i_29_]
		= (fs_28_[i_29_] - fs_23_[i + i_29_]) * fs_26_[i_27_ + i_29_];
	g729a_common.g729a_rearrange(fs, 0.0012207031F, 10);
	g729a_common.g729a_rearrange(fs, 6.1035156E-4F, 10);
	g729a_common.g729a_reconstracte(fs_22_, fs, fs_23_, i, i_25_, 10);
	g729a_common.g729a_check_stability(fs_22_);
	for (int i_30_ = 0; i_30_ < 10; i_30_++)
	    var_g729a_decode_lp.q_quant_present_subframe_2[i_30_]
		= (float) Math.cos((double) fs_22_[i_30_]);
    }

    static void g729a_dec_lag3_1
	(G729aCode g729acode, g729a_decode_internal var_g729a_decode_internal,
	 g729a_decode_speech var_g729a_decode_speech) {
	float[] fs = var_g729a_decode_internal.v;
	float[] fs_31_ = var_g729a_decode_speech.excitation;
	int i = g729acode.getP0();
	int i_32_ = g729acode.getP1();
	int i_33_
	    = (i_32_ >> 2 & 0x1 ^ i_32_ >> 3 & 0x1 ^ i_32_ >> 4 & 0x1
	       ^ i_32_ >> 5 & 0x1 ^ i_32_ >> 6 & 0x1 ^ i_32_ >> 7 & 0x1 ^ 0x1);
	int i_34_;
	int i_35_;
	if (i_33_ != i) {
	    i_34_ = var_g729a_decode_speech.T2;
	    i_35_ = 0;
	} else if (i_32_ < 197) {
	    i_34_ = (i_32_ + 2) / 3 + 19;
	    i_35_ = i_32_ - 3 * i_34_ + 58;
	} else {
	    i_34_ = i_32_ - 112;
	    i_35_ = 0;
	}
	var_g729a_decode_internal.T1 = i_34_;
	if (i_35_ == 1) {
	    i_35_ = -2;
	    i_34_++;
	}
	g729a_common.g729a_generation_adaptive_vector(fs, fs_31_, i_34_,
						      i_35_);
    }

    static void g729a_dec_lag3_2
	(G729aCode g729acode, g729a_decode_internal var_g729a_decode_internal,
	 g729a_decode_speech var_g729a_decode_speech) {
	float[] fs = var_g729a_decode_internal.v;
	float[] fs_36_ = var_g729a_decode_speech.excitation;
	int i = g729acode.getP2();
	int i_37_ = var_g729a_decode_internal.T1;
	int i_38_ = i_37_ - 5;
	if (i_38_ < 20)
	    i_38_ = 20;
	int i_39_ = i_38_ + 9;
	if (i_39_ > 143) {
	    i_39_ = 143;
	    i_38_ = i_39_ - 9;
	}
	int i_40_ = (i + 2) / 3 - 1 + i_38_;
	int i_41_ = i - 2 - 3 * (i_40_ - i_38_);
	var_g729a_decode_speech.T2 = i_40_;
	if (i_41_ == 1) {
	    i_41_ = -2;
	    i_40_++;
	}
	g729a_common.g729a_generation_adaptive_vector(fs, fs_36_, i_40_,
						      i_41_);
    }

    static void g729a_de_acelp(g729a_decode_internal var_g729a_decode_internal,
			       int i, float f) {
	float[] fs = var_g729a_decode_internal.c;
	int i_42_ = var_g729a_decode_internal.S;
	int i_43_ = var_g729a_decode_internal.C;
	int i_44_ = i_42_ & 0x1;
	i_44_ = i_44_ - 1 + (i_44_ & 0x1);
	int i_45_ = i_42_ >> 1 & 0x1;
	i_45_ = i_45_ - 1 + (i_45_ & 0x1);
	int i_46_ = i_42_ >> 2 & 0x1;
	i_46_ = i_46_ - 1 + (i_46_ & 0x1);
	int i_47_ = i_42_ >> 3 & 0x1;
	i_47_ = i_47_ - 1 + (i_47_ & 0x1);
	g729a_utils.g729a_set_0(fs, 40);
	int i_48_ = (i_43_ & 0x7) * 5;
	int i_49_ = (i_43_ >> 3 & 0x7) * 5 + 1;
	int i_50_ = (i_43_ >> 6 & 0x7) * 5 + 2;
	int i_51_ = (i_43_ >> 9 & 0x1) + 3;
	int i_52_ = (i_43_ >> 10 & 0x7) * 5 + i_51_;
	fs[i_48_] = (float) i_44_;
	fs[i_49_] = (float) i_45_;
	fs[i_50_] = (float) i_46_;
	fs[i_52_] = (float) i_47_;
	if (f < 0.2000122F)
	    f = 0.2000122F;
	if (f > 0.7944946F)
	    f = 0.7944946F;
	for (i_48_ = i; i_48_ < 40; i_48_++)
	    fs[i_48_] += f * fs[i_48_ - i];
    }

    static void g729a_dec_gain(g729a_decode_internal var_g729a_decode_internal,
			       g729a_decode_speech var_g729a_decode_speech) {
	float[] fs = var_g729a_decode_internal.c;
	float[] fs_53_ = var_g729a_decode_speech.U;
	byte i = g729a_tables.g729a_imap1[var_g729a_decode_internal.GA];
	byte i_54_ = g729a_tables.g729a_imap2[var_g729a_decode_internal.GB];
	float f = g729a_common.g729a_norma(fs);
	f = 1.0F / (float) Math.sqrt((double) (f / 40.0F));
	float f_55_ = ((0.68F * fs_53_[0] + 0.58F * fs_53_[1]
			+ 0.34F * fs_53_[2] + 0.19F * fs_53_[3] + 30.0F)
		       / 20.0F);
	float f_56_
	    = (float) ((double) f * Math.exp((double) (2.3025851F * f_55_)));
	var_g729a_decode_speech.past_quan_gp
	    = var_g729a_decode_internal.gp_quan
	    = (g729a_tables.g729a_gbk1[i][0]
	       + g729a_tables.g729a_gbk2[i_54_][0]);
	float f_57_ = (g729a_tables.g729a_gbk1[i][1]
		       + g729a_tables.g729a_gbk2[i_54_][1]);
	var_g729a_decode_speech.past_quan_gc
	    = var_g729a_decode_internal.gc_quan = f_56_ * f_57_;
	fs_53_[3] = fs_53_[2];
	fs_53_[2] = fs_53_[1];
	fs_53_[1] = fs_53_[0];
	fs_53_[0]
	    = (float) (20.0 * Math.log((double) f_57_) * 0.4342944819032518);
    }

    static void g729a_reconstruct_speech
	(g729a_decode_internal var_g729a_decode_internal,
	 g729a_decode_speech var_g729a_decode_speech,
	 g729a_decode_filters var_g729a_decode_filters, float[] fs) {
	float[] fs_58_ = var_g729a_decode_speech.excitation;
	float[] fs_59_ = var_g729a_decode_internal.v;
	float[] fs_60_ = var_g729a_decode_internal.c;
	float[] fs_61_ = var_g729a_decode_internal.s;
	float f = var_g729a_decode_internal.gc_quan;
	float f_62_ = var_g729a_decode_internal.gp_quan;
	g729a_sinthesis_filter var_g729a_sinthesis_filter
	    = var_g729a_decode_filters.sinthesis;
	for (int i = 0; i < 40; i++)
	    fs_58_[i + 154] = f_62_ * fs_59_[i] + f * fs_60_[i];
	g729a_common.g729a_IIR(fs_61_, fs_58_, 154, fs,
			       var_g729a_sinthesis_filter);
	for (int i = -114; i < 40; i++)
	    fs_58_[154 + i - 40] = fs_58_[154 + i];
    }
}
