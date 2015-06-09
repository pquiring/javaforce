/* Decoder - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

public final class Decoder implements Constants {

  private g729a_decoder decoder = new g729a_decoder();

  public void decode(short[] is, int i, byte[] is_0_, int i_1_, int i_2_) {
    this.decoder.decode(is, i, is_0_, i_1_, i_2_);
  }

  public void decode(short[] is, int i, byte[] is_3_, int i_4_, boolean bool,
          boolean bool_5_) {
    this.decoder.decode(is, i, is_3_, i_4_, bool, bool_5_);
  }

  public final void reset() {
    this.decoder.reset();
  }
}