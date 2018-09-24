/* g729a_lpc - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_lpc implements g729a_constants
{
    static void g729a_lp_analysis
	(float[] fs, g729a_encode_speech var_g729a_encode_speech) {
	float[] fs_0_ = new float[240];
	float[] fs_1_ = new float[11];
	float[] fs_2_ = new float[10];
	float[] fs_3_ = new float[5];
	float[] fs_4_ = new float[5];
	g729a_window(fs_0_, 0, g729a_tables.g729a_wlp, 0,
		     var_g729a_encode_speech.past_subframe_3);
	g729a_window(fs_0_, 40, g729a_tables.g729a_wlp, 40,
		     var_g729a_encode_speech.past_subframe_2);
	g729a_window(fs_0_, 80, g729a_tables.g729a_wlp, 80,
		     var_g729a_encode_speech.past_subframe_1);
	g729a_window(fs_0_, 120, g729a_tables.g729a_wlp, 120,
		     var_g729a_encode_speech.present_subframe_1);
	g729a_window(fs_0_, 160, g729a_tables.g729a_wlp, 160,
		     var_g729a_encode_speech.present_subframe_2);
	g729a_window(fs_0_, 200, g729a_tables.g729a_wlp, 200,
		     var_g729a_encode_speech.future_subframe);
	g729a_utils.g729a_corel_11(fs_1_, fs_0_);
	if (fs_1_[0] < 1.0F)
	    fs_1_[0] = 1.0F;
	for (int i = 0; i <= 10; i++)
	    fs_1_[i] *= g729a_tables.g729a_wlag[i];
	float f = -fs_1_[1] / fs_1_[0];
	float f_5_ = f;
	float f_6_ = fs_1_[0] * (1.0F - f * f);
	if (!(f_6_ <= 0.0F)) {
	    f = -(fs_1_[2] + f_5_ * fs_1_[1]) / f_6_;
	    f_5_ += f * f_5_;
	    float f_7_ = f;
	    f_6_ *= 1.0F - f * f;
	    if (!(f_6_ <= 0.0F)) {
		f = -(fs_1_[3] + f_5_ * fs_1_[2] + f_7_ * fs_1_[1]) / f_6_;
		float f_8_ = f_5_;
		f_5_ += f * f_7_;
		f_7_ += f * f_8_;
		float f_9_ = f;
		f_6_ *= 1.0F - f * f;
		if (!(f_6_ <= 0.0F)) {
		    f = -(fs_1_[4] + f_5_ * fs_1_[3] + f_7_ * fs_1_[2]
			  + f_9_ * fs_1_[1]) / f_6_;
		    f_8_ = f_5_;
		    f_5_ += f * f_9_;
		    f_7_ += f * f_7_;
		    f_9_ += f * f_8_;
		    float f_10_ = f;
		    f_6_ *= 1.0F - f * f;
		    if (!(f_6_ <= 0.0F)) {
			f = -(fs_1_[5] + f_5_ * fs_1_[4] + f_7_ * fs_1_[3]
			      + f_9_ * fs_1_[2] + f_10_ * fs_1_[1]) / f_6_;
			f_8_ = f_5_;
			f_5_ += f * f_10_;
			f_10_ += f * f_8_;
			f_8_ = f_7_;
			f_7_ += f * f_9_;
			f_9_ += f * f_8_;
			float f_11_ = f;
			f_6_ *= 1.0F - f * f;
			if (!(f_6_ <= 0.0F)) {
			    f = -(fs_1_[6] + f_5_ * fs_1_[5] + f_7_ * fs_1_[4]
				  + f_9_ * fs_1_[3] + f_10_ * fs_1_[2]
				  + f_11_ * fs_1_[1]) / f_6_;
			    f_8_ = f_5_;
			    f_5_ += f * f_11_;
			    f_11_ += f * f_8_;
			    f_8_ = f_7_;
			    f_7_ += f * f_10_;
			    f_10_ += f * f_8_;
			    f_9_ += f * f_9_;
			    float f_12_ = f;
			    f_6_ *= 1.0F - f * f;
			    if (!(f_6_ <= 0.0F)) {
				f = -(fs_1_[7] + f_5_ * fs_1_[6]
				      + f_7_ * fs_1_[5] + f_9_ * fs_1_[4]
				      + f_10_ * fs_1_[3] + f_11_ * fs_1_[2]
				      + f_12_ * fs_1_[1]) / f_6_;
				f_8_ = f_5_;
				f_5_ += f * f_12_;
				f_12_ += f * f_8_;
				f_8_ = f_7_;
				f_7_ += f * f_11_;
				f_11_ += f * f_8_;
				f_8_ = f_9_;
				f_9_ += f * f_10_;
				f_10_ += f * f_8_;
				float f_13_ = f;
				f_6_ *= 1.0F - f * f;
				if (!(f_6_ <= 0.0F)) {
				    f = -(fs_1_[8] + f_5_ * fs_1_[7]
					  + f_7_ * fs_1_[6] + f_9_ * fs_1_[5]
					  + f_10_ * fs_1_[4] + f_11_ * fs_1_[3]
					  + f_12_ * fs_1_[2]
					  + f_13_ * fs_1_[1]) / f_6_;
				    f_8_ = f_5_;
				    f_5_ += f * f_13_;
				    f_13_ += f * f_8_;
				    f_8_ = f_7_;
				    f_7_ += f * f_12_;
				    f_12_ += f * f_8_;
				    f_8_ = f_9_;
				    f_9_ += f * f_11_;
				    f_11_ += f * f_8_;
				    f_10_ += f * f_10_;
				    float f_14_ = f;
				    f_6_ *= 1.0F - f * f;
				    if (!(f_6_ <= 0.0F)) {
					f = -(fs_1_[9] + f_5_ * fs_1_[8]
					      + f_7_ * fs_1_[7]
					      + f_9_ * fs_1_[6]
					      + f_10_ * fs_1_[5]
					      + f_11_ * fs_1_[4]
					      + f_12_ * fs_1_[3]
					      + f_13_ * fs_1_[2]
					      + f_14_ * fs_1_[1]) / f_6_;
					f_8_ = f_5_;
					f_5_ += f * f_14_;
					f_14_ += f * f_8_;
					f_8_ = f_7_;
					f_7_ += f * f_13_;
					f_13_ += f * f_8_;
					f_8_ = f_9_;
					f_9_ += f * f_12_;
					f_12_ += f * f_8_;
					f_8_ = f_10_;
					f_10_ += f * f_11_;
					f_11_ += f * f_8_;
					float f_15_ = f;
					f_6_ *= 1.0F - f * f;
					if (!(f_6_ <= 0.0F)) {
					    f = -(fs_1_[10] + f_5_ * fs_1_[9]
						  + f_7_ * fs_1_[8]
						  + f_9_ * fs_1_[7]
						  + f_10_ * fs_1_[6]
						  + f_11_ * fs_1_[5]
						  + f_12_ * fs_1_[4]
						  + f_13_ * fs_1_[3]
						  + f_14_ * fs_1_[2]
						  + f_15_ * fs_1_[1]) / f_6_;
					    f_8_ = f_5_;
					    f_5_ += f * f_15_;
					    f_15_ += f * f_8_;
					    f_8_ = f_7_;
					    f_7_ += f * f_14_;
					    f_14_ += f * f_8_;
					    f_8_ = f_9_;
					    f_9_ += f * f_13_;
					    f_13_ += f * f_8_;
					    f_8_ = f_10_;
					    f_10_ += f * f_12_;
					    f_12_ += f * f_8_;
					    f_11_ += f * f_11_;
					    float f_16_ = f;
					    f_6_ *= 1.0F - f * f;
					    if (!(f_6_ <= 0.0F)) {
						float f_17_
						    = f_5_ + f_16_ - 1.0F;
						float f_18_
						    = f_7_ + f_15_ - f_17_;
						float f_19_
						    = f_9_ + f_14_ - f_18_;
						float f_20_
						    = f_10_ + f_13_ - f_19_;
						float f_21_
						    = f_11_ + f_12_ - f_20_;
						float f_22_ = 0.5F * f_21_;
						fs_3_[0] = f_17_;
						fs_3_[1] = f_18_;
						fs_3_[2] = f_19_;
						fs_3_[3] = f_20_;
						fs_3_[4] = f_22_;
						float f_23_
						    = f_5_ - f_16_ + 1.0F;
						float f_24_
						    = f_7_ - f_15_ + f_23_;
						float f_25_
						    = f_9_ - f_14_ + f_24_;
						float f_26_
						    = f_10_ - f_13_ + f_25_;
						float f_27_
						    = f_11_ - f_12_ + f_26_;
						float f_28_ = 0.5F * f_27_;
						fs_4_[0] = f_23_;
						fs_4_[1] = f_24_;
						fs_4_[2] = f_25_;
						fs_4_[3] = f_26_;
						fs_4_[4] = f_28_;
						boolean bool = false;
						float[] fs_29_ = fs_3_;
						int i = 0;
						float f_30_ = 1.0F;
						f_30_ = (g729a_tables.g729a_tab
							 [0]);
						float f_31_
						    = g729a_chebps(f_30_,
								   fs_29_);
						for (int i_32_ = 1; i_32_ < 51;
						     i_32_++) {
						    float f_33_ = f_30_;
						    float f_34_ = f_31_;
						    f_30_
							= (g729a_tables
							   .g729a_tab[i_32_]);
						    f_31_
							= g729a_chebps(f_30_,
								       fs_29_);
						    if (f_34_ * f_31_
							<= 0.0F) {
							for (int i_35_ = 0;
							     i_35_ < 2;
							     i_35_++) {
							    float f_36_
								= ((f_33_
								    + f_30_)
								   / 2.0F);
							    float f_37_
								= (g729a_chebps
								   (f_36_,
								    fs_29_));
							    if (f_31_ * f_37_
								<= 0.0F) {
								f_34_ = f_37_;
								f_33_ = f_36_;
							    } else {
								f_31_ = f_37_;
								f_30_ = f_36_;
							    }
							}
							float f_38_;
							if (f_34_ == f_31_)
							    f_38_ = f_30_;
							else
							    f_38_
								= (f_30_
								   - (f_31_
								      * (f_33_
									 - f_30_)
								      / (f_34_
									 - f_31_)));
							fs_2_[i] = f_38_;
							if (++i >= 10)
							    break;
							f_30_ = f_38_;
							if (!bool) {
							    bool = true;
							    fs_29_ = fs_4_;
							} else {
							    bool = false;
							    fs_29_ = fs_3_;
							}
							f_31_ = (g729a_chebps
								 (f_30_,
								  fs_29_));
						    }
						}
						if (i >= 10) {
						    for (int i_39_ = 0;
							 i_39_ < 10; i_39_++)
							fs[i_39_]
							    = fs_2_[i_39_];
						}
					    }
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	}
    }

    static void g729a_window(float[] fs, int i, float[] fs_40_, int i_41_,
			     float[] fs_42_) {
	for (int i_43_ = 0; i_43_ < 40; i_43_++)
	    fs[i_43_ + i] = fs_42_[i_43_] * fs_40_[i_43_ + i_41_];
    }

    static float g729a_chebps(float f, float[] fs) {
	float f_44_ = 2.0F * f;
	float f_45_ = f_44_ + fs[0];
	float f_46_ = f_44_ * f_45_ - 1.0F + fs[1];
	float f_47_ = f_44_ * f_46_ - f_45_ + fs[2];
	float f_48_ = f_44_ * f_47_ - f_46_ + fs[3];
	float f_49_ = f * f_48_ - f_47_ + fs[4];
	return f_49_;
    }
}
