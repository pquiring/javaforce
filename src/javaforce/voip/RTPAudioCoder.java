package javaforce.voip;

/**
 * Base interface for all audio codec encoders/decoders.
 */
public interface RTPAudioCoder {
  public byte[] encode(short[] src16, int id);
  public short[] decode(byte[] src8, int off);
  public int getSampleRate();
}
