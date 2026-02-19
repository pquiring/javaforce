package javaforce.controls.s7;

/**
 * S7 Header
 *
 * @author pquiring
 */

import javaforce.*;

public class S7Header {
  public byte id = 0x32;  //protocol id (always 0x32)
  public byte rosctr = ROSCTR_JOB;
  public short res;  //reserved
  public short pdu_ref;  //protocol data unit reference
  public short param_length;
  public short data_length;

  public static final byte ROSCTR_JOB = 1;
  public static final byte ROSCTR_ACK = 3;
  public static final byte ROSCTR_USERDATA = 7;

  public int size() {
    switch (rosctr) {
      case ROSCTR_JOB: return 10;
      case ROSCTR_ACK: return 12;
      case ROSCTR_USERDATA: return 10;
    }
    return 0;
  }
  public void write(byte[] data, int offset, short _param_length, short _data_length) {
    pdu_ref = 0x500;
    param_length = _param_length;
    data_length = _data_length;
    data[offset++] = id;
    data[offset++] = rosctr;
    BE.setuint16(data, offset, res); offset += 2;
    BE.setuint16(data, offset, pdu_ref); offset += 2;
    BE.setuint16(data, offset, param_length); offset += 2;
    BE.setuint16(data, offset, data_length); offset += 2;
  }
  public void read(byte[] data, int offset) throws Exception {
    id = data[offset++];
    rosctr = data[offset++];
    res = (short)BE.getuint16(data, offset); offset += 2;
    pdu_ref = (short)BE.getuint16(data, offset); offset += 2;
    param_length = (short)BE.getuint16(data, offset); offset += 2;
    data_length = (short)BE.getuint16(data, offset); offset += 2;
    if (rosctr == ROSCTR_ACK) {
      //error_cls = data[offset++]
      //error_code = data[offset++]
    }
  }
}
