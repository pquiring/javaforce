/* g729a_sinthesis_filter - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_sinthesis_filter implements g729a_constants
{
    float[] delay = new float[10];
    
    void print(String string) {
	for (int i = 0; i < 10; i++)
	    System.err
		.println(string + "[" + i + "]=" + g729a_utils.HF(delay[i]));
    }
}