/* g729a_decode_filters - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_decode_filters
{
    g729a_preproc_filter postproc;
    g729a_sinthesis_filter sinthesis;
    g729a_sinthesis_filter residual;
    g729a_sinthesis_filter shortterm;
    g729a_tilt_filter tilt;
    
    void print() {
	postproc.print();
	sinthesis.print("filters.sinthesis");
	residual.print("filters.residual");
	shortterm.print("filters.shortterm");
	tilt.print("filters.tilt");
    }
}