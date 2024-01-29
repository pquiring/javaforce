package javaforce.controls.s7;

/**
 *
 * @author pquiring
 */

public class S7Data {
  public byte block_type;
  public int block_number;
  public byte data_type;
  public int offset;  //24bit
  public int length;  //# elements
  public byte[] data;
  public int getLength() {  //returns bytes
    return S7Types.getTypeSize(data_type, length);
  }
}
