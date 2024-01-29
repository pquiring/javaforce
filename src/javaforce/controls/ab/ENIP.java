package javaforce.controls.ab;

import javaforce.LE;

/** EtherNet/IP (Industrial Protocol)
 *
 * @author pquiring
 */

public class ENIP {
  //header (24 bytes)
  public short cmd;  //cmd type
  public short len;  //following command data below
  public int session;  //random ID for connection
  public int status;  //0 = success
  public long context;  //increments per packet
  public int options;  //0
  //CMD_RR_DATA (16 bytes)
  public int ihandle;  //0
  public short timeout;  //0
  public short count = 2;
  public short type_1 = 0x0000;
  public short len_1 = 0x0000;
  public short type_2 = 0x00b2;
  public short len_2 = 0x0000;  //length of CIP packet in bytes
  //CMD_GET_SESSION (4 bytes)
  public short protocol = 0x0001;
  public short flags = 0x0000;

  public static final short CMD_RR_DATA = 0x6f;
  public static final short CMD_GET_SESSION = 0x65;

  public ENIP() {}

  public ENIP(short _cmd) {
    cmd = _cmd;
  }

  public int size() {
    switch (cmd) {
      case CMD_RR_DATA: return 24 + 16;  //40
      case CMD_GET_SESSION: return 24 + 4;  //28
    }
    return -1;
  }

  public void read(byte[] data, int offset) throws Exception {
    cmd = (short)LE.getuint16(data, offset); offset += 2;
    len = (short)LE.getuint16(data, offset); offset += 2;
    session = LE.getuint32(data, offset); offset += 4;
    status = LE.getuint32(data, offset); offset += 4;
    context = LE.getuint64(data, offset); offset += 8;
    options = LE.getuint32(data, offset); offset += 4;
    switch (cmd) {
      case CMD_RR_DATA:
        ihandle = LE.getuint32(data, offset); offset += 4;
        timeout = (short)LE.getuint16(data, offset); offset += 2;
        count = (short)LE.getuint16(data, offset); offset += 2;
        if (count != 2) throw new Exception("ab:bad ip packet");
        type_1 = (short)LE.getuint16(data, offset); offset += 2;
        len_1 = (short)LE.getuint16(data, offset); offset += 2;
        type_2 = (short)LE.getuint16(data, offset); offset += 2;
        len_2 = (short)LE.getuint16(data, offset); offset += 2;
        break;
      case CMD_GET_SESSION:
        protocol = (short)LE.getuint16(data, offset); offset += 2;
        flags = (short)LE.getuint16(data, offset); offset += 2;
        break;
    }
  }

  public void setSizes(int size) {
    switch (cmd) {
      case CMD_RR_DATA:
        len = (short)(16 + size);
        len_2 = (short)size;
        break;
      case CMD_GET_SESSION:
        len = 4;
        break;
    }
  }

  public void write(byte[] data, int offset, ABContext abcontext) {
    session = abcontext.session;
    context = abcontext.context;
    abcontext.increment();
    LE.setuint16(data, offset, cmd); offset += 2;
    LE.setuint16(data, offset, len); offset += 2;
    LE.setuint32(data, offset, session); offset += 4;
    LE.setuint32(data, offset, status); offset += 4;
    LE.setuint64(data, offset, context); offset += 8;
    LE.setuint32(data, offset, options); offset += 4;
    switch (cmd) {
      case CMD_RR_DATA:
        LE.setuint32(data, offset, ihandle); offset += 4;
        LE.setuint16(data, offset, timeout); offset += 2;
        LE.setuint16(data, offset, count); offset += 2;
        LE.setuint16(data, offset, type_1); offset += 2;
        LE.setuint16(data, offset, len_1); offset += 2;
        LE.setuint16(data, offset, type_2); offset += 2;
        LE.setuint16(data, offset, len_2); offset += 2;
        break;
      case CMD_GET_SESSION:
        LE.setuint16(data, offset, protocol); offset += 2;
        LE.setuint16(data, offset, flags); offset += 2;
        break;
    }
  }
}
