/* g729a_preproc_filter - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_preproc_filter
{
    float a0;
    float a1;
    float a2;
    float b1;
    float b2;
    float x1;
    float x2;
    float y1;
    float y2;
    
    void print() {
	System.err.println("postroc.a0=" + g729a_utils.HF(a0));
	System.err.println("postroc.a1=" + g729a_utils.HF(a1));
	System.err.println("postroc.a2=" + g729a_utils.HF(a2));
	System.err.println("postroc.b1=" + g729a_utils.HF(b1));
	System.err.println("postroc.b2=" + g729a_utils.HF(b2));
	System.err.println("postroc.x1=" + g729a_utils.HF(x1));
	System.err.println("postroc.x2=" + g729a_utils.HF(x2));
	System.err.println("postroc.y1=" + g729a_utils.HF(y1));
	System.err.println("postroc.y2=" + g729a_utils.HF(y2));
    }
}