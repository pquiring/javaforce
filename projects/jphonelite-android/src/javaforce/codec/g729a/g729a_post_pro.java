/* g729a_post_pro - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_post_pro implements g729a_constants
{
    static void g729a_postproc_filter
	(short[] is, int i, float[] fs,
	 g729a_decode_filters var_g729a_decode_filters) {
	g729a_preproc_filter var_g729a_preproc_filter
	    = var_g729a_decode_filters.postproc;
	int[] is_0_ = new int[40];
	float f = var_g729a_preproc_filter.a0;
	float f_1_ = var_g729a_preproc_filter.a1;
	float f_2_ = var_g729a_preproc_filter.a2;
	float f_3_ = var_g729a_preproc_filter.b1;
	float f_4_ = var_g729a_preproc_filter.b2;
	float f_5_ = var_g729a_preproc_filter.x1;
	float f_6_ = var_g729a_preproc_filter.x2;
	float f_7_ = var_g729a_preproc_filter.y1;
	float f_8_ = var_g729a_preproc_filter.y2;
	for (int i_9_ = 0; i_9_ < 40; i_9_++) {
	    float f_10_ = fs[i_9_];
	    float f_11_ = (f * f_10_ + f_1_ * f_5_ + f_2_ * f_6_ + f_4_ * f_8_
			   + f_3_ * f_7_);
	    f_6_ = f_5_;
	    f_5_ = f_10_;
	    f_8_ = f_7_;
	    f_7_ = f_11_;
	    is_0_[i_9_] = (int) (f_11_ + 32768.0F);
	    is[i_9_ + i] = (short) (is_0_[i_9_] >> 16);
	}
	var_g729a_preproc_filter.x1 = f_5_;
	var_g729a_preproc_filter.x2 = f_6_;
	var_g729a_preproc_filter.y1 = f_7_;
	var_g729a_preproc_filter.y2 = f_8_;
    }
}