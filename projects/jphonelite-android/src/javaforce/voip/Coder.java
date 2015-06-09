package javaforce.voip;

/**
 * Base interface for all codec encoders/decoders.
 */
public interface Coder {
  public byte[] encode(short src16[]);
  public short[] decode(byte src8[]);
}
