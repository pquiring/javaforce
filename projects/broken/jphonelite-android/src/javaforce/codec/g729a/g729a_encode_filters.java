/* g729a_encode_filters - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_encode_filters
{
    g729a_preproc_filter preproc;
    g729a_sinthesis_filter sinthesis;
    g729a_sinthesis_filter weghted_sinthesis;
    g729a_sinthesis_filter weghted;
    
    void print() {
	preproc.print();
	sinthesis.print("filters.sinthesis");
	weghted_sinthesis.print("filters.weghted_sinthesis");
	weghted.print("filters.weghted");
    }
}