/* g729a_pst - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_pst implements g729a_constants
{
    static void g729a_pst(g729a_decode_speech var_g729a_decode_speech,
			  g729a_decode_internal var_g729a_decode_internal,
			  g729a_decode_filters var_g729a_decode_filters,
			  float[] fs, int i) {
	g729a_sinthesis_filter var_g729a_sinthesis_filter
	    = var_g729a_decode_filters.residual;
	g729a_sinthesis_filter var_g729a_sinthesis_filter_0_
	    = var_g729a_decode_filters.shortterm;
	g729a_tilt_filter var_g729a_tilt_filter
	    = var_g729a_decode_filters.tilt;
	float[] fs_1_ = var_g729a_decode_internal.s;
	float[] fs_2_ = var_g729a_decode_internal.sf;
	float[] fs_3_ = var_g729a_decode_speech.residual;
	float f = var_g729a_decode_speech.past_g;
	float[] fs_4_ = new float[40];
	float[] fs_5_ = new float[11];
	float[] fs_6_ = new float[40];
	float[] fs_7_ = new float[40];
	g729a_sinthesis_filter var_g729a_sinthesis_filter_8_
	    = new g729a_sinthesis_filter();
	if (i > 140)
	    i = 140;
	fs_4_[0] = 1.0F;
	fs_4_[1] = 0.55F * fs[0];
	fs_4_[2] = 0.3025F * fs[1];
	fs_4_[3] = 0.16637501F * fs[2];
	fs_4_[4] = 0.09150626F * fs[3];
	fs_4_[5] = 0.05032844F * fs[4];
	fs_4_[6] = 0.027680643F * fs[5];
	fs_4_[7] = 0.015224354F * fs[6];
	fs_4_[8] = 0.008373395F * fs[7];
	fs_4_[9] = 0.0046053673F * fs[8];
	fs_4_[10] = 0.002532952F * fs[9];
	fs_5_[0] = 0.7F * fs[0];
	fs_5_[1] = 0.48999998F * fs[1];
	fs_5_[2] = 0.343F * fs[2];
	fs_5_[3] = 0.2401F * fs[3];
	fs_5_[4] = 0.16806999F * fs[4];
	fs_5_[5] = 0.11764899F * fs[5];
	fs_5_[6] = 0.08235429F * fs[6];
	fs_5_[7] = 0.057648003F * fs[7];
	fs_5_[8] = 0.0403536F * fs[8];
	fs_5_[9] = 0.02824752F * fs[9];
	g729a_common.g729a_FIR(fs_3_, 144, fs_1_, fs_4_, 1,
			       var_g729a_sinthesis_filter);
	int i_9_ = i - 3;
	float f_10_ = g729a_common.g729a_correl(fs_3_, 144, fs_3_, 144 - i_9_);
	for (int i_11_ = i - 2; i_11_ < i + 4; i_11_++) {
	    float f_12_
		= g729a_common.g729a_correl(fs_3_, 144, fs_3_, 144 - i_11_);
	    if (f_12_ > f_10_) {
		f_10_ = f_12_;
		i_9_ = i_11_;
	    }
	}
	float f_13_ = g729a_common.g729a_norma(fs_3_, 144);
	float f_14_ = g729a_common.g729a_norma(fs_3_, 144 - i_9_);
	float f_15_;
	if (f_13_ == 0.0F || f_14_ == 0.0F)
	    f_15_ = 0.0F;
	else
	    f_15_ = f_10_ * f_10_ / (f_14_ * f_13_);
	float f_16_;
	if (f_15_ < 0.5F)
	    f_16_ = 0.0F;
	else {
	    f_16_ = f_10_ / f_13_;
	    var_g729a_decode_speech.periodic |= 0x1;
	}
	if (f_16_ < 0.0F)
	    f_16_ = 0.0F;
	if (f_16_ > 1.0F)
	    f_16_ = 1.0F;
	float f_17_ = f_16_ * 0.5F;
	float f_18_ = 1.0F / (1.0F + f_17_);
	float f_19_ = f_17_ / (1.0F + f_17_);
	for (int i_20_ = 0; i_20_ < 40; i_20_++)
	    fs_7_[i_20_] = (f_18_ * fs_3_[i_20_ + 144]
			    + f_19_ * fs_3_[i_20_ + 144 - i_9_]);
	g729a_utils.g729a_set_0(fs_4_, 11, 29);
	g729a_utils.g729a_set_0(var_g729a_sinthesis_filter_8_.delay, 10);
	g729a_common.g729a_IIR(fs_6_, fs_4_, fs_5_,
			       var_g729a_sinthesis_filter_8_);
	float f_21_ = 0.0F;
	for (int i_22_ = 0; i_22_ < 22; i_22_++)
	    f_21_ += fs_6_[i_22_] * fs_6_[i_22_];
	float f_23_ = 0.0F;
	for (int i_24_ = 0; i_24_ < 21; i_24_++)
	    f_23_ += fs_6_[i_24_] * fs_6_[i_24_ + 1];
	float f_25_ = -f_23_ / f_21_;
	float f_26_;
	if (f_25_ < 0.0F)
	    f_26_ = 0.8F;
	else
	    f_26_ = 0.0F;
	f_26_ *= f_25_;
	float f_27_ = var_g729a_tilt_filter.x1;
	for (int i_28_ = 0; i_28_ < 40; i_28_++) {
	    float f_29_ = fs_7_[i_28_];
	    fs_7_[i_28_] = f_29_ + f_26_ * f_27_;
	    f_27_ = f_29_;
	}
	var_g729a_tilt_filter.x1 = f_27_;
	g729a_common.g729a_IIR(fs_2_, fs_7_, fs_5_,
			       var_g729a_sinthesis_filter_0_);
	f_13_ = g729a_common.g729a_norma(fs_1_);
	f_10_ = g729a_common.g729a_norma(fs_2_);
	float f_30_;
	if (f_10_ == 0.0F)
	    f_30_ = 0.0F;
	else
	    f_30_ = (float) (Math.sqrt((double) (f_13_ / f_10_))
			     * 0.10000000149011612);
	for (int i_31_ = 0; i_31_ < 40; i_31_++) {
	    fs_2_[i_31_] *= f;
	    f = 0.9F * f + f_30_;
	}
	var_g729a_decode_speech.past_g = f;
	for (int i_32_ = 0; i_32_ < 144; i_32_++)
	    fs_3_[i_32_] = fs_3_[i_32_ + 40];
    }
}