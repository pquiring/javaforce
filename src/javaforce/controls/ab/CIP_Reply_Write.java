package javaforce.controls.ab;

/** CIP : Connection Manager : Reply
 *
 * @author pquiring
 */

public class CIP_Reply_Write {
  public byte cmd = (byte)0xcd;
  public byte reserved1;
  public byte reserved2;
  public byte reserved3;

  public int size() {
    return 4;
  }
  public void read(byte[] data, int offset) throws Exception {
    cmd = data[offset++];
    reserved1 = data[offset++];
    reserved2 = data[offset++];
    reserved3 = data[offset++];
  }
}
