package javaforce.controls.s7;

/**
 *
 * @author pquiring
 */

public class S7Data {
  public byte block_type;
  public byte block_number;
  public byte data_type;
  public short offset;
  public short length;
  public byte data[];
}
