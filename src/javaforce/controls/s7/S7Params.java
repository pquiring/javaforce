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

  /** Create a packet to read single tag. */
  public void makeRead(S7Data s7) {
    func = READ;
    funcData = new byte[13];
    funcData[0] = 1;  //count
    funcData[1] = 0x12;  //var def
    funcData[2] = 10;  //length of def
    funcData[3] = 0x10;  //S7ANY
    funcData[4] = s7.data_type;  //INT, BYTE, etc.
    BE.setuint16(funcData, 5, 1);  //length (# of elements)
    BE.setuint16(funcData, 7, s7.block_number);  //DBxx
    funcData[9] = s7.block_type;  //DB, I, Q, etc.
    //BE.setuint24(data, 9, off);
    funcData[10] = (byte)((s7.offset & 0xff0000) >> 16);
    funcData[11] = (byte)((s7.offset & 0xff00) >> 8);
    funcData[12] = (byte)(s7.offset & 0xff);
  }

  /** Create a packet to read multiple tags. */
  public void makeRead(S7Data s7s[]) {
    func = READ;
    byte cnt = (byte)s7s.length;
    funcData = new byte[1 + cnt * 12];
    funcData[0] = cnt;  //count
    int offset = 1;
    for(byte a=0;a<cnt;a++) {
      S7Data s7 = s7s[a];
      funcData[offset++] = 0x12;  //var def
      funcData[offset++] = 10;  //length of def
      funcData[offset++] = 0x10;  //S7ANY
      funcData[offset++] = s7.data_type;  //INT, BYTE, etc.
      BE.setuint16(funcData, offset, 1);  //length (# of elements)
      offset += 2;
      BE.setuint16(funcData, offset, s7.block_number);  //DBxx
      offset += 2;
      funcData[offset++] = s7.block_type;  //DB, I, Q, etc.
      //BE.setuint24(data, 9, off);
      funcData[offset++] = (byte)((s7.offset & 0xff0000) >> 16);
      funcData[offset++] = (byte)((s7.offset & 0xff00) >> 8);
      funcData[offset++] = (byte)(s7.offset & 0xff);
    }
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
    BE.setuint16(funcData, 5, 1);  //length (# of elements)
    BE.setuint16(funcData, 7, block_number);  //DBxx
    funcData[9] = block_type;  //DB, I, Q, etc.
    //BE.setuint24(data, 9, off);
    funcData[10] = (byte)((off & 0xff0000) >> 16);
    funcData[11] = (byte)((off & 0xff00) >> 8);
    funcData[12] = (byte)(off & 0xff);

    funcData[13] = 0x00;  //res
    funcData[14] = getTransportType(data_type);  //transport type
    if (len > 1) len <<= 3;  //length in bits
    BE.setuint16(funcData, 15, len);
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

  private boolean isBits(byte transport_type) {
    switch (transport_type) {
      case TT_BIT: return true;
      case TT_UINT: return true;
      case TT_SINT: return true;
      case TT_REAL: return false;
      case TT_CHAR: return false;
      default: return false;
    }
  }

  /** Reads params from packet and fills in S7Data. */
  public boolean read(byte data[], int offset, S7Data out) throws Exception {
    func = data[offset++];
    byte count = data[offset++];
    for(int a=0;a<count;a++) {
      byte success = data[offset++];
      if (success != (byte)0xff) {
        JFLog.log("Error:success=" + success);
        return false;
      }
      if (func == READ) {
        byte transport_type = data[offset++];
        int len = BE.getuint16(data, offset);
        if (isBits(transport_type)) {
          len = (len + 7) >> 3; //divide by 8
        }
        offset += 2;
        if (a == 0) {
          out.data = new byte[len];
          System.arraycopy(data,offset,out.data,0,len);
        }
        offset += len;
        if (len % 2 == 1) {
          offset++;  //fill byte
        }
      }
    }
    return true;
  }

  /** Reads params from packet and fills in S7Data. */
  public boolean read(byte data[], int offset, S7Data outs[]) throws Exception {
    func = data[offset++];
    byte count = data[offset++];
    for(int a=0;a<count;a++) {
      S7Data out = outs[a];
      byte success = data[offset++];
      if (success != (byte)0xff) {
        JFLog.log("Error:success=" + success);
        return false;
      }
      if (func == READ) {
        byte transport_type = data[offset++];
        int len = BE.getuint16(data, offset);
        if (isBits(transport_type)) {
          len = (len + 7) >> 3; //divide by 8
        }
        offset += 2;
        out.data = new byte[len];
        System.arraycopy(data,offset,out.data,0,len);
        offset += len;
        if (len % 2 == 1) {
          offset++;  //fill byte
        }
      }
    }
    return true;
  }
}
