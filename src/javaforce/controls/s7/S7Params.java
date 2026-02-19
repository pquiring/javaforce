package javaforce.controls.s7;

/**
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class S7Params {
  public byte func;
  public byte[] funcData;  //varies based on func

  //funcs
  public static final byte CPU = 0x00;
  public static final byte READ = 0x04;
  public static final byte WRITE = 0x05;
  public static final byte CONNECT = (byte)0xf0;

  //cpu sub-funcs
  public static final byte REQUEST_TIME = 0x47;
  public static final byte RESPONSE_TIME = (byte)0x87;

  //time sub-funcs
  public static final byte READ_CLOCK = 0x1;
  public static final byte WRITE_CLOCK = 0x2;


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
    BE.setuint16(funcData, 5, s7.length);  //length (# of elements)
    BE.setuint16(funcData, 7, s7.block_number);  //DBxx
    funcData[9] = s7.block_type;  //DB, I, Q, etc.
    //BE.setuint24(data, 9, off);
    funcData[10] = (byte)((s7.offset & 0xff0000) >> 16);
    funcData[11] = (byte)((s7.offset & 0xff00) >> 8);
    funcData[12] = (byte)(s7.offset & 0xff);
  }

  /** Create a packet to read multiple tags. */
  public void makeRead(S7Data[] s7s) {
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
  public void makeWrite(byte block_type, int block_number, byte data_type, int off/*24bit*/, int len, byte[] data) {
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
    if (data_type == S7Types.BIT) {
      BE.setuint16(funcData, 15, len);  //# of bits
    } else {
      BE.setuint16(funcData, 15, data.length << 3);  //# of bits
    }
    System.arraycopy(data, 0, funcData, 17, data.length);
  }

  /** Create a packet to read PLC time. */
  public void makeReadTime() {
    func = CPU;
    funcData = new byte[7 + 4];
    funcData[0] = 1;  //count
    funcData[1] = 0x12;  //var def
    funcData[2] = 4;  //length of def
    funcData[3] = 0x11;  //syntax-id
    funcData[4] = REQUEST_TIME;
    funcData[5] = READ_CLOCK;
    funcData[6] = 0;  //seq
    //data
    funcData[7] = 0x0a;  //obj does not exist
    funcData[8] = 0x00;  //NULL
    BE.setuint16(funcData, 9, 0);  //length
  }

  /** Create a packet to write PLC time. */
  public void makeWriteTime(Calendar dt) {
    int year = dt.get(Calendar.YEAR);
    int month = dt.get(Calendar.MONTH) + 1;
    int day = dt.get(Calendar.DAY_OF_MONTH);
    int hour = dt.get(Calendar.HOUR_OF_DAY);
    int min = dt.get(Calendar.MINUTE);
    int sec = dt.get(Calendar.SECOND);
    int ms = dt.get(Calendar.MILLISECOND);
    func = CPU;
    funcData = new byte[7 + 14];
    funcData[0] = 1;  //count
    funcData[1] = 0x12;  //var def
    funcData[2] = 4;  //length of def
    funcData[3] = 0x11;  //syntax-id
    funcData[4] = REQUEST_TIME;
    funcData[5] = WRITE_CLOCK;
    funcData[6] = 0;  //seq
    //data
    funcData[7] = (byte)0xff;  //success
    funcData[8] = 0x09;  //octet string
    BE.setuint16(funcData, 9, 0x0a);  //length
    funcData[11] = 0;  //reserved
    year -= 100;  //???
    EBCDIC.encode((short)year, funcData, 12);
    EBCDIC.encode((byte)month, funcData, 14);
    EBCDIC.encode((byte)day, funcData, 15);
    EBCDIC.encode((byte)hour, funcData, 16);
    EBCDIC.encode((byte)min, funcData, 17);
    EBCDIC.encode((byte)sec, funcData, 18);
    EBCDIC.encode((short)ms, funcData, 19);
  }

  /** Returns size of params. */
  public int size() {
    return 1 + funcData.length;
  }

  /** Write params to packet. */
  public void write(byte[] data, int offset) {
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
  public boolean read(byte[] data, int offset, S7Data out) throws Exception {
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
  public boolean read(byte[] data, int offset, S7Data[] outs) throws Exception {
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

  /** Reads params from packet and fills in Calendar. */
  public boolean read(byte[] data, int offset, Calendar out) throws Exception {
    func = data[offset++];
    byte count = data[offset++];
    for(int a=0;a<count;a++) {
      if (func == CPU) {
        byte var_spec = data[offset++];  //0x12
        byte var_spec_len = data[offset++];  //0x08
        byte syntax = data[offset++];  //0x12
        byte func_group = data[offset++];  //0x87
        byte sub_func = data[offset++];  //READ_CLOCK
        if (sub_func == READ_CLOCK) {
          byte seq = data[offset++];
          byte data_ref = data[offset++];
          byte last_data = data[offset++];
          short error = (short)BE.getuint16(data, offset); offset += 2;
          //read data
          byte result = data[offset++];
          if (result == (byte)0xff) {
            byte trans_size = data[offset++];  //0x09 (OCTET)
            if (trans_size != 0x09) {
              JFLog.log("Warning:OCTET = " + trans_size);
            }
            short len = (short)BE.getuint16(data, offset); offset += 2;  //0x000a
            if (len != 0x0a) {
              JFLog.log("Warning:length = " + trans_size);
            }
            byte res = data[offset++];
            int year = EBCDIC.decode(data, offset, 2); offset += 2;
            year += 100;  //???
            int month = EBCDIC.decode(data, offset++, 1);
            int day = EBCDIC.decode(data, offset++, 1);
            int hour = EBCDIC.decode(data, offset++, 1);
            int min = EBCDIC.decode(data, offset++, 1);
            int sec = EBCDIC.decode(data, offset++, 1);
            int ms = EBCDIC.decode(data, offset, 2); offset += 2;
            out.set(Calendar.YEAR, year);
            out.set(Calendar.MONTH, month - 1);
            out.set(Calendar.DAY_OF_MONTH, day);
            out.set(Calendar.HOUR_OF_DAY, hour);
            out.set(Calendar.MINUTE, min);
            out.set(Calendar.SECOND, sec);
            out.set(Calendar.MILLISECOND, ms);
          } else {
            JFLog.log("Read time failed! result=" + result);
          }
        }
      }
    }
    return true;
  }
}
