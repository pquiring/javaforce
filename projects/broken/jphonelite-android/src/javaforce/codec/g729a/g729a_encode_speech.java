/* g729a_encode_speech - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_encode_speech implements g729a_constants
{
    float[] past_subframe_3;
    float[] past_subframe_2;
    float[] past_subframe_1;
    float[] present_subframe_1;
    float[] present_subframe_2;
    float[] future_subframe;
    float[] weighted_speech;
    float[] excitation;
    int[] U = new int[4];
    float past_quan_gp;
    
    void print() {
	for (int i = 0; i < 40; i++)
	    System.err.println("speech.past_subframe_3[" + i + "]="
			       + g729a_utils.HF(past_subframe_3[i]));
	for (int i = 0; i < 40; i++)
	    System.err.println("speech.past_subframe_2[" + i + "]="
			       + g729a_utils.HF(past_subframe_2[i]));
	for (int i = 0; i < 40; i++)
	    System.err.println("speech.past_subframe_1[" + i + "]="
			       + g729a_utils.HF(past_subframe_1[i]));
	for (int i = 0; i < 40; i++)
	    System.err.println("speech.present_subframe_1[" + i + "]="
			       + g729a_utils.HF(present_subframe_1[i]));
	for (int i = 0; i < 40; i++)
	    System.err.println("speech.present_subframe_2[" + i + "]="
			       + g729a_utils.HF(present_subframe_2[i]));
	for (int i = 0; i < 40; i++)
	    System.err.println("speech.future_subframe[" + i + "]="
			       + g729a_utils.HF(future_subframe[i]));
	for (int i = 0; i < 184; i++)
	    System.err.println("speech.weighted_speech[" + i + "]="
			       + g729a_utils.HF(weighted_speech[i]));
	for (int i = 0; i < 194; i++)
	    System.err.println("speech.excitation[" + i + "]="
			       + g729a_utils.HF(excitation[i]));
	for (int i = 0; i < 4; i++)
	    System.err.println("speech.U[" + i + "]=" + U[i]);
	System.err
	    .println("speech.past_quan_gp=" + g729a_utils.HF(past_quan_gp));
    }
}