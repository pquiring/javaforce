/* Encoder - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

public final class Encoder implements Constants
{
    private g729a_encoder encoder = new g729a_encoder();

    public void encode(byte[] is, int i, short[] is_0_, int i_1_, int i_2_) {
	this.encoder.encode(is, i, is_0_, i_1_, i_2_);
    }

    public void encode(byte[] is, int i, short[] is_3_, int i_4_) {
	this.encoder.encode(is, i, is_3_, i_4_);
    }

    public final void reset() {
	this.encoder.reset();
    }
}