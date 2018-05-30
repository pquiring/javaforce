package javaforce.controls.s7;

/**
 *
 * @author pquiring
 */

public class S7Types {

  //block_type
  public static final byte I = (byte) 0x81;
  public static final byte Q = (byte) 0x82;
  public static final byte M = (byte) 0x83;
  public static final byte DB = (byte) 0x84;
  //etc...

  //data_type
  public static final byte BIT = 1; //1bit
  public static final byte BYTE = 2; //8bit
  public static final byte CHAR = 3; //8bit
  public static final byte WORD = 4; //unsigned 16bit
  public static final byte INT = 5; //signed 16bit
  public static final byte DWORD = 6; //unsigned 32bit
  public static final byte DINT = 7; //signed 32bit
  public static final byte REAL = 8; //float (32bit)
  //etc...

  public static short getTypeSize(byte data_type, short len) {
    switch (data_type) {
      case S7Types.BIT:
        return (short)((len + 7) / 8);
      case S7Types.BYTE:
      case S7Types.CHAR:
        return len;
      case S7Types.WORD:
      case S7Types.INT:
        return (short)(len * 2);
      case S7Types.DWORD:
      case S7Types.DINT:
      case S7Types.REAL:
        return (short)(len * 4);
      default:
        return 0;
    }
  }

  public static byte getType(char type) {
    switch (type) {
      case 'X': return BIT;
      case 'B': return BYTE;
      case 'D': return DWORD;
      case 'W': return WORD;
      case 'I': return INT;
      case 'C': return CHAR;
      case 'R': return REAL;
    }
    return 0;
  }

}
