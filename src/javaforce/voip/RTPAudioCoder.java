package javaforce.voip;

/**
 * Base interface for all audio codec encoders/decoders.
 */
public interface RTPAudioCoder {
  public void setid(int id);
  public byte[] encode(short[] src16);
  public short[] decode(byte[] src8, int off);
  public int getSampleRate();
}
