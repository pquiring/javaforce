package javaforce.controls.s7;

/**
 *
 * @author pquiring
 */

public class S7Data {
  public byte block_type;
  public short block_number;
  public byte data_type;
  public int offset;  //24bit
  public short length;
  public byte data[];
  public int getLength() {
    return S7Types.getTypeSize(data_type, length);
  }
}
