package javaforce.controls.ab;

/** CIP : Connection Manager : Reply
 *
 * @author pquiring
 */

public class CIP_Reply_Read {
  public byte cmd = (byte)0xcc;
  public byte reserved1;
  public byte reserved2;
  public byte reserved3;
  public byte type = 0;  //ABTypes
  public byte reserved;
  public byte[] tagdata;  //BE format

  public int size() {
    return tagdata.length + 6;
  }
  public void read(byte data[], int offset) throws Exception {
    cmd = data[offset++];
    reserved1 = data[offset++];
    reserved2 = data[offset++];
    reserved3 = data[offset++];
    type = data[offset++];
    reserved = data[offset++];
    int size = 0;
    switch (type) {
      case ABTypes.INT:
        size = 2;
        break;
      case ABTypes.DINT:
      case ABTypes.REAL:
        size = 4;
        break;
      case ABTypes.BOOL:
        size = 1;
        break;
    }
    tagdata = new byte[size];
    System.arraycopy(data, offset, tagdata, 0, size);
  }
}
