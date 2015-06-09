/* g729a_utils - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_utils implements g729a_constants
{
    static void g729a_set_0(float[] fs, int i, int i_0_) {
	for (int i_1_ = 0; i_1_ < i_0_; i_1_++)
	    fs[i_1_ + i] = 0.0F;
    }
    
    static void g729a_set_0(float[] fs, int i) {
	for (int i_2_ = 0; i_2_ < i; i_2_++)
	    fs[i_2_] = 0.0F;
    }
    
    static void g729a_corel_11(float[] fs, float[] fs_3_) {
	float f = fs_3_[0];
	float f_4_ = fs_3_[1];
	float f_5_ = fs_3_[2];
	float f_6_ = fs_3_[3];
	float f_7_ = fs_3_[4];
	float f_8_ = fs_3_[5];
	float f_9_ = fs_3_[6];
	float f_10_ = fs_3_[7];
	float f_11_ = fs_3_[8];
	float f_12_ = fs_3_[9];
	float f_13_ = (f * f + f_4_ * f_4_ + f_5_ * f_5_ + f_6_ * f_6_
		       + f_7_ * f_7_ + f_8_ * f_8_ + f_9_ * f_9_
		       + f_10_ * f_10_ + f_11_ * f_11_ + f_12_ * f_12_);
	float f_14_
	    = (f_4_ * f + f_5_ * f_4_ + f_6_ * f_5_ + f_7_ * f_6_ + f_8_ * f_7_
	       + f_9_ * f_8_ + f_10_ * f_9_ + f_11_ * f_10_ + f_12_ * f_11_);
	float f_15_
	    = (f_5_ * f + f_6_ * f_4_ + f_7_ * f_5_ + f_8_ * f_6_ + f_9_ * f_7_
	       + f_10_ * f_8_ + f_11_ * f_9_ + f_12_ * f_10_);
	float f_16_ = (f_6_ * f + f_7_ * f_4_ + f_8_ * f_5_ + f_9_ * f_6_
		       + f_10_ * f_7_ + f_11_ * f_8_ + f_12_ * f_9_);
	float f_17_ = (f_7_ * f + f_8_ * f_4_ + f_9_ * f_5_ + f_10_ * f_6_
		       + f_11_ * f_7_ + f_12_ * f_8_);
	float f_18_ = (f_8_ * f + f_9_ * f_4_ + f_10_ * f_5_ + f_11_ * f_6_
		       + f_12_ * f_7_);
	float f_19_ = f_9_ * f + f_10_ * f_4_ + f_11_ * f_5_ + f_12_ * f_6_;
	float f_20_ = f_10_ * f + f_11_ * f_4_ + f_12_ * f_5_;
	float f_21_ = f_11_ * f + f_12_ * f_4_;
	float f_22_ = f_12_ * f;
	float f_23_ = 0.0F;
	for (int i = 10; i < 240; i++) {
	    float f_24_ = fs_3_[i];
	    f_13_ += f_24_ * f_24_;
	    f_14_ += f_24_ * f_12_;
	    f_15_ += f_24_ * f_11_;
	    f_16_ += f_24_ * f_10_;
	    f_17_ += f_24_ * f_9_;
	    f_18_ += f_24_ * f_8_;
	    f_19_ += f_24_ * f_7_;
	    f_20_ += f_24_ * f_6_;
	    f_21_ += f_24_ * f_5_;
	    f_22_ += f_24_ * f_4_;
	    f_23_ += f_24_ * f;
	    f = f_4_;
	    f_4_ = f_5_;
	    f_5_ = f_6_;
	    f_6_ = f_7_;
	    f_7_ = f_8_;
	    f_8_ = f_9_;
	    f_9_ = f_10_;
	    f_10_ = f_11_;
	    f_11_ = f_12_;
	    f_12_ = f_24_;
	}
	fs[0] = f_13_;
	fs[1] = f_14_;
	fs[2] = f_15_;
	fs[3] = f_16_;
	fs[4] = f_17_;
	fs[5] = f_18_;
	fs[6] = f_19_;
	fs[7] = f_20_;
	fs[8] = f_21_;
	fs[9] = f_22_;
	fs[10] = f_23_;
    }
    
    static void g729a_corel_10(float[] fs, int i, float[] fs_25_, int i_26_) {
	float f = 0.0F;
	float f_27_ = 0.0F;
	float f_28_ = 0.0F;
	float f_29_ = 0.0F;
	float f_30_ = 0.0F;
	float f_31_ = 0.0F;
	float f_32_ = 0.0F;
	float f_33_ = 0.0F;
	float f_34_ = 0.0F;
	float f_35_ = 0.0F;
	float f_36_ = fs_25_[-i_26_];
	float f_37_ = fs_25_[-i_26_ + 2];
	float f_38_ = fs_25_[-i_26_ + 4];
	float f_39_ = fs_25_[-i_26_ + 6];
	float f_40_ = fs_25_[-i_26_ + 8];
	float f_41_ = fs_25_[-i_26_ + 10];
	float f_42_ = fs_25_[-i_26_ + 12];
	float f_43_ = fs_25_[-i_26_ + 14];
	float f_44_ = fs_25_[-i_26_ + 16];
	float[] fs_45_ = fs_25_;
	int i_46_ = -i_26_ + 18;
	int i_47_ = 0;
	while (i_47_ < 80) {
	    float f_48_ = fs_25_[i_46_];
	    float f_49_ = fs_25_[i_47_ + 144];
	    f += f_49_ * f_36_;
	    f_27_ += f_49_ * f_37_;
	    f_28_ += f_49_ * f_38_;
	    f_29_ += f_49_ * f_39_;
	    f_30_ += f_49_ * f_40_;
	    f_31_ += f_49_ * f_41_;
	    f_32_ += f_49_ * f_42_;
	    f_33_ += f_49_ * f_43_;
	    f_34_ += f_49_ * f_44_;
	    f_35_ += f_49_ * f_48_;
	    f_36_ = f_37_;
	    f_37_ = f_38_;
	    f_38_ = f_39_;
	    f_39_ = f_40_;
	    f_40_ = f_41_;
	    f_41_ = f_42_;
	    f_42_ = f_43_;
	    f_43_ = f_44_;
	    f_44_ = f_48_;
	    i_47_ += 2;
	    i_46_ += 2;
	}
	fs[i] = f;
	fs[1 + i] = f_27_;
	fs[2 + i] = f_28_;
	fs[3 + i] = f_29_;
	fs[4 + i] = f_30_;
	fs[5 + i] = f_31_;
	fs[6 + i] = f_32_;
	fs[7 + i] = f_33_;
	fs[8 + i] = f_34_;
	fs[9 + i] = f_35_;
    }
    
    static void validateCode(byte[] is, int i) {
	if (i < 0 || i > is.length)
	    throw new G729aException(2);
	if (is.length - i < 10)
	    throw new G729aException(1);
    }
    
    static void validateData(short[] is, int i) {
	if (i < 0 || i > is.length)
	    throw new G729aException(2);
	if (is.length - i < 80)
	    throw new G729aException(1);
    }
    
    static String HF(float f) {
	return Integer.toHexString(Float.floatToIntBits(f));
    }
}