/* g729a_decode_lp - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_decode_lp implements g729a_constants
{
    float[] q_quant_past_subframe_2;
    float[] q_quant_present_subframe_1 = new float[10];
    float[] q_quant_present_subframe_2;
    float[] l_quant_past_frame_4;
    float[] l_quant_past_frame_3;
    float[] l_quant_past_frame_2;
    float[] l_quant_past_frame_1;
    float[] past_omega = new float[10];
    int past_MA;
    float[] fg_l_quant_sum = new float[20];
    
    void print() {
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.q_quant_past_subframe_2[" + i + "]="
			       + g729a_utils.HF(q_quant_past_subframe_2[i]));
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.q_quant_present_subframe_1[" + i + "]="
			       + g729a_utils
				     .HF(q_quant_present_subframe_1[i]));
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.q_quant_present_subframe_2[" + i + "]="
			       + g729a_utils
				     .HF(q_quant_present_subframe_2[i]));
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.l_quant_past_frame_4[" + i + "]="
			       + g729a_utils.HF(l_quant_past_frame_4[i]));
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.l_quant_past_frame_3[" + i + "]="
			       + g729a_utils.HF(l_quant_past_frame_3[i]));
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.l_quant_past_frame_2[" + i + "]="
			       + g729a_utils.HF(l_quant_past_frame_2[i]));
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.l_quant_past_frame_1[" + i + "]="
			       + g729a_utils.HF(l_quant_past_frame_1[i]));
	for (int i = 0; i < 10; i++)
	    System.err.println("lp.past_omega[" + i + "]="
			       + g729a_utils.HF(past_omega[i]));
	System.err.println("past_MA=" + past_MA);
	for (int i = 0; i < 20; i++)
	    System.err.println("lp.fg_l_quant_sum[" + i + "]="
			       + g729a_utils.HF(fg_l_quant_sum[i]));
    }
}