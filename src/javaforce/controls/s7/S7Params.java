package javaforce.controls.s7;

import java.util.*;

import javaforce.*;
import javaforce.net.*;

/** S7Params Packet.
 *
 * @author pquiring
 */

public class S7Params implements SubPacket {
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
    BCD.encode((short)year, funcData, 12);
    BCD.encode((byte)month, funcData, 14);
    BCD.encode((byte)day, funcData, 15);
    BCD.encode((byte)hour, funcData, 16);
    BCD.encode((byte)min, funcData, 17);
    BCD.encode((byte)sec, funcData, 18);
    BCD.encode((short)ms, funcData, 19);
  }

  /** Returns size of params. */
  public int getSize() {
    return 1 + funcData.length;
  }

  public int getDataSize() {
    return -1;
  }

  /** Write params to packet. */
  public void write(Packet packet) throws Exception {
    packet.writeByte(func);
    packet.write(funcData);
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

  public void read(Packet packet) throws Exception {
    read(packet, (S7Data)null);
  }

  /** Reads params from packet and fills in S7Data. */
  public void read(Packet packet, S7Data out) throws Exception {
    func = packet.readByte();
    byte count = packet.readByte();
    boolean padding = false;
    for(int a=0;a<count;a++) {
      byte success = packet.readByte();
      if (success != (byte)0xff) {
        throw new Exception("Error:success=" + success);
      }
      if (func == READ) {
        if (padding) {
          packet.readByte();  //fill byte
          padding = false;
        }
        byte transport_type = packet.readByte();
        int len = packet.readShort();
        if (isBits(transport_type)) {
          len = (len + 7) >> 3; //divide by 8
        }
        byte[] data = new byte[len];
        packet.read(data);
        if (a == 0 && out != null) {
          out.data = new byte[len];
          System.arraycopy(data,0, out.data,0, len);
        }
        if (len % 2 == 1) {
          padding = true;
        }
      }
    }
  }

  /** Reads params from packet and fills in S7Data. */
  public void read(Packet packet, S7Data[] outs) throws Exception {
    func = packet.readByte();
    byte count = packet.readByte();
    boolean padding = false;
    for(int a=0;a<count;a++) {
      S7Data out = outs[a];
      byte success = packet.readByte();
      if (success != (byte)0xff) {
        throw new Exception("Error:success=" + success);
      }
      if (func == READ) {
        if (padding) {
          packet.readByte();  //fill byte
          padding = false;
        }
        byte transport_type = packet.readByte();
        int len = packet.readShort();
        if (isBits(transport_type)) {
          len = (len + 7) >> 3; //divide by 8
        }
        out.data = new byte[len];
        packet.read(out.data);
        if (len % 2 == 1) {
          padding = true;
        }
      }
    }
  }

  /** Reads params from packet and fills in Calendar. */
  public void read(Packet packet, Calendar out) throws Exception {
    func = packet.readByte();
    byte count = packet.readByte();
    for(int a=0;a<count;a++) {
      if (func == CPU) {
        byte var_spec = packet.readByte();  //0x12
        byte var_spec_len = packet.readByte();  //0x08
        byte syntax = packet.readByte();  //0x12
        byte func_group = packet.readByte();  //0x87
        byte sub_func = packet.readByte();  //READ_CLOCK
        if (sub_func == READ_CLOCK) {
          byte seq = packet.readByte();
          byte data_ref = packet.readByte();
          byte last_data = packet.readByte();
          short error = (short)packet.readShort();
          //read data
          byte result = packet.readByte();
          if (result == (byte)0xff) {
            byte trans_size = packet.readByte();  //0x09 (OCTET)
            if (trans_size != 0x09) {
              throw new Exception("Warning:OCTET = " + trans_size);
            }
            short len = (short)packet.readShort();  //0x000a
            if (len != 0x0a) {
              throw new Exception("Warning:length = " + trans_size);
            }
            byte res = packet.readByte();
            int year = BCD.decode(packet.data, packet.offset, 2); packet.offset += 2;
            year += 100;  //???
            int month = BCD.decode(packet.data, packet.offset++, 1);
            int day = BCD.decode(packet.data, packet.offset++, 1);
            int hour = BCD.decode(packet.data, packet.offset++, 1);
            int min = BCD.decode(packet.data, packet.offset++, 1);
            int sec = BCD.decode(packet.data, packet.offset++, 1);
            int ms = BCD.decode(packet.data, packet.offset, 2); packet.offset += 2;
            out.set(Calendar.YEAR, year);
            out.set(Calendar.MONTH, month - 1);
            out.set(Calendar.DAY_OF_MONTH, day);
            out.set(Calendar.HOUR_OF_DAY, hour);
            out.set(Calendar.MINUTE, min);
            out.set(Calendar.SECOND, sec);
            out.set(Calendar.MILLISECOND, ms);
          } else {
            throw new Exception("Read time failed! result=" + result);
          }
        }
      }
    }
  }
}
