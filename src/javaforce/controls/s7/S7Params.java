package javaforce.controls.s7;

/**
 *
 * @author pquiring
 */

import javaforce.*;

public class S7Params {
  public byte func;
  public byte[] funcData;  //varies based on func

  //funcs
  public static final byte READ = 0x04;
  public static final byte WRITE = 0x05;
  public static final byte CONNECT = (byte)0xf0;

  /** Create a packet to setup communications (connect) . */
  public void makeConnect() {
    func = CONNECT;
    funcData = new byte[7];
    funcData[0] = 1;  //res
    funcData[1] = 0; funcData[2] = 1;  //max AmQ calling
    funcData[3] = 1; funcData[4] = 1;  //max AmQ called
    funcData[5] = 1; funcData[6] = (byte)0xe0;  //PDU length
  }

  /** Create a packet to read data. */
  public void makeRead(byte block_type, short block_number, byte data_type, int off/*24bit*/, short len) {
    func = READ;
    funcData = new byte[13];
    funcData[0] = 1;  //count
    funcData[1] = 0x12;  //var def
    funcData[2] = 10;  //length of def
    funcData[3] = 0x10;  //S7ANY
    funcData[4] = data_type;  //INT, BYTE, etc.
    BE.setuint16(funcData, 5, len);  //length (# of elements)
    BE.setuint16(funcData, 7, block_number);  //DBxx
    funcData[9] = block_type;  //DB, I, Q, etc.
    //BE.setuint24(data, 9, off);
    funcData[10] = (byte)((off & 0xff0000) >> 16);
    funcData[11] = (byte)((off & 0xff00) >> 8);
    funcData[12] = (byte)(off & 0xff);
  }

  //transport types
  private static final byte TT_UNKNOWN = 0;
  private static final byte TT_BIT = 3;
  private static final byte TT_UINT = 4;
  private static final byte TT_SINT = 5;
  //6
  private static final byte TT_REAL = 7;
  //8
  private static final byte TT_CHAR = 9;

  private byte getTransportType(byte data_type) {
    switch (data_type) {
      case S7Types.BIT:
        return TT_BIT;
      case S7Types.BYTE:
      case S7Types.WORD:
      case S7Types.DWORD:
        return TT_UINT;
      case S7Types.CHAR:
        return TT_CHAR;
      case S7Types.INT:
      case S7Types.DINT:
        return TT_SINT;
      case S7Types.REAL:
        return TT_REAL;
      default:
        return TT_UNKNOWN;
    }
  }

  /** Create a packet to write data. */
  public void makeWrite(byte block_type, short block_number, byte data_type, int off/*24bit*/, short len, byte data[]) {
    func = WRITE;
    funcData = new byte[13 + 4 + data.length];
    funcData[0] = 1;  //count
    funcData[1] = 0x12;  //var def
    funcData[2] = 10;  //length of def
    funcData[3] = 0x10;  //S7ANY
    funcData[4] = data_type;  //INT, BYTE, etc.
    BE.setuint16(funcData, 5, len);  //length (# of elements)
    BE.setuint16(funcData, 7, block_number);  //DBxx
    funcData[9] = block_type;  //DB, I, Q, etc.
    //BE.setuint24(data, 9, off);
    funcData[10] = (byte)((off & 0xff0000) >> 16);
    funcData[11] = (byte)((off & 0xff00) >> 8);
    funcData[12] = (byte)(off & 0xff);

    funcData[13] = 0x00;  //res
    funcData[14] = getTransportType(data_type);  //transport type
    BE.setuint16(funcData, 15, len << 3);  //length (# of bytes)
    System.arraycopy(data, 0, funcData, 17, data.length);
  }

  /** Returns size of params. */
  public int size() {
    return 1 + funcData.length;
  }

  /** Write params to packet. */
  public void write(byte data[], int offset) {
    data[offset++] = func;
    System.arraycopy(funcData, 0, data, offset, funcData.length);
  }

  /** Reads params from packet and fills in S7Data. */
  public boolean read(byte data[], int offset, S7Data out) throws Exception {
    func = data[offset++];
    byte count = data[offset++];
    if (count > 1) throw new Exception("S7:only support 1 data per packet:" + count);
    for(int a=0;a<count;a++) {
      byte success = data[offset++];
      if (success != (byte)0xff) {
        JFLog.log("Error:" + success);
        return false;
      }
      if (func == READ) {
        byte transport_type = data[offset++];
        short len = (short)BE.getuint16(data, offset);
        offset += 2;
        out.data = new byte[len];
        System.arraycopy(data,offset,out.data,0,len);
        offset += len;
      }
    }
    return true;
  }
}
