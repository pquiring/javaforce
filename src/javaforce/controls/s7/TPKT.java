package javaforce.controls.s7;

import javaforce.BE;

/** TPKT Header
 *
 * @author pquiring
 */

public class TPKT {
  public byte version = 3;  //always 3
  public byte res;  //always 0
  public short length;  //length of data including this header
  public int size() {
    return 4;
  }
  public void write(byte[] data, int offset, short totalsize) {
    length = totalsize;
    data[offset++] = version;
    data[offset++] = res;
    BE.setuint16(data, offset, length); //offset += 2;
  }
  public void read(byte[] data, int offset) throws Exception {
    version = data[offset++];
    if (version != 3) throw new Exception("TPKT : unknown version");
    res = data[offset++];
    if (res != 0) throw new Exception("TPKT : unknown res");
    length = (short)BE.getuint16(data, offset);
    offset += 2;
  }
}
