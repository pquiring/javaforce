/* g729a_pwf - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_pwf implements g729a_constants
{
    static void g729a_pwf(float[] fs, int i, float[] fs_0_, int i_1_,
			  float[] fs_2_, float[] fs_3_,
			  g729a_encode_filters var_g729a_encode_filters) {
	g729a_sinthesis_filter var_g729a_sinthesis_filter
	    = var_g729a_encode_filters.sinthesis;
	g729a_sinthesis_filter var_g729a_sinthesis_filter_4_
	    = var_g729a_encode_filters.weghted;
	g729a_common.g729a_FIR(fs_0_, i_1_, fs_2_, fs_3_, 0,
			       var_g729a_sinthesis_filter);
	i += 144;
	float f = fs_3_[0];
	float f_5_ = fs_3_[1];
	float f_6_ = fs_3_[2];
	float f_7_ = fs_3_[3];
	float f_8_ = fs_3_[4];
	float f_9_ = fs_3_[5];
	float f_10_ = fs_3_[6];
	float f_11_ = fs_3_[7];
	float f_12_ = fs_3_[8];
	float f_13_ = fs_3_[9];
	f_13_ = 0.07508469F * (0.75F * f_13_ - 0.7F * f_12_);
	f_12_ = 0.100112915F * (0.75F * f_12_ - 0.7F * f_11_);
	f_11_ = 0.13348389F * (0.75F * f_11_ - 0.7F * f_10_);
	f_10_ = 0.17797852F * (0.75F * f_10_ - 0.7F * f_9_);
	f_9_ = 0.23730469F * (0.75F * f_9_ - 0.7F * f_8_);
	f_8_ = 0.31640625F * (0.75F * f_8_ - 0.7F * f_7_);
	f_7_ = 0.421875F * (0.75F * f_7_ - 0.7F * f_6_);
	f_6_ = 0.5625F * (0.75F * f_6_ - 0.7F * f_5_);
	f_5_ = 0.75F * (0.75F * f_5_ - 0.7F * f);
	f = 0.75F * f - 0.7F;
	float f_14_ = var_g729a_sinthesis_filter_4_.delay[0];
	float f_15_ = var_g729a_sinthesis_filter_4_.delay[1];
	float f_16_ = var_g729a_sinthesis_filter_4_.delay[2];
	float f_17_ = var_g729a_sinthesis_filter_4_.delay[3];
	float f_18_ = var_g729a_sinthesis_filter_4_.delay[4];
	float f_19_ = var_g729a_sinthesis_filter_4_.delay[5];
	float f_20_ = var_g729a_sinthesis_filter_4_.delay[6];
	float f_21_ = var_g729a_sinthesis_filter_4_.delay[7];
	float f_22_ = var_g729a_sinthesis_filter_4_.delay[8];
	float f_23_ = var_g729a_sinthesis_filter_4_.delay[9];
	for (int i_24_ = 0; i_24_ < 40; i_24_++) {
	    float f_25_ = fs_0_[i_24_ + i_1_];
	    float f_26_
		= (f_25_ - f_13_ * f_23_ - f_12_ * f_22_ - f_11_ * f_21_
		   - f_10_ * f_20_ - f_9_ * f_19_ - f_8_ * f_18_ - f_7_ * f_17_
		   - f_6_ * f_16_ - f_5_ * f_15_ - f * f_14_);
	    float f_27_ = f_23_;
	    f_23_ = f_22_;
	    f_22_ = f_21_;
	    f_21_ = f_20_;
	    f_20_ = f_19_;
	    f_19_ = f_18_;
	    f_18_ = f_17_;
	    f_17_ = f_16_;
	    f_16_ = f_15_;
	    f_15_ = f_14_;
	    f_14_ = f_26_;
	    fs[i + i_24_] = f_26_;
	}
	var_g729a_sinthesis_filter_4_.delay[0] = f_14_;
	var_g729a_sinthesis_filter_4_.delay[1] = f_15_;
	var_g729a_sinthesis_filter_4_.delay[2] = f_16_;
	var_g729a_sinthesis_filter_4_.delay[3] = f_17_;
	var_g729a_sinthesis_filter_4_.delay[4] = f_18_;
	var_g729a_sinthesis_filter_4_.delay[5] = f_19_;
	var_g729a_sinthesis_filter_4_.delay[6] = f_20_;
	var_g729a_sinthesis_filter_4_.delay[7] = f_21_;
	var_g729a_sinthesis_filter_4_.delay[8] = f_22_;
	var_g729a_sinthesis_filter_4_.delay[9] = f_23_;
    }
}