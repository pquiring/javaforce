package javaforce.controls.ab;

/** Allen Bradley Data Types
 *
 * @author pquiring
 */

public class ABTypes {
  public static final byte DT = (byte)0xc0;  //datetime : 64bit
  public static final byte BOOL = (byte)0xc1;
  public static final byte SINT = (byte)0xc2;  //byte
  public static final byte INT = (byte)0xc3;  //short
  public static final byte DINT = (byte)0xc4;  //int
  public static final byte LINT = (byte)0xc5;  //long
  public static final byte USINT = (byte)0xc6;  //unsigned byte
  public static final byte UINT = (byte)0xc7;  //unsigned short
  public static final byte UDINT = (byte)0xc8;  //unsigned int
  public static final byte ULINT = (byte)0xc9;  //unsigned long
  public static final byte REAL = (byte)0xca;  //float
  public static final byte LREAL = (byte)0xcb;  //double
  public static final byte STIME = (byte)0xcc;  //synchronous time
  public static final byte DATE = (byte)0xcd;  //date
  public static final byte TIME_DAY = (byte)0xce;  //time of day
  public static final byte DATE_TIME_DAY = (byte)0xcf;  //date and time of day
  public static final byte CHAR_STRING = (byte)0xd0;  //char string (bytes)
  public static final byte BYTE = (byte)0xd1;  //8bit boolean array
  public static final byte WORD = (byte)0xd2;  //16bit boolean array
  public static final byte DWORD = (byte)0xd3;  //32bit boolean array
  public static final byte LWORD = (byte)0xd4;  //64bit boolean array
  public static final byte STRING2 = (byte)0xd5;  //utf16 string
  public static final byte FTIME = (byte)0xd6;  //High resolution duration value
  public static final byte LTIME = (byte)0xd7;  //Medium resolution duration value
  public static final byte ITIME = (byte)0xd8;  //Low resolution duration value
  public static final byte STRINGN = (byte)0xd9;  //N-byte per char string
  public static final byte SHORT_STRING = (byte)0xda;  //1 byte length : 1 byte per char
  public static final byte TIME = (byte)0xdb;  //Time in ms
  public static final byte EPATH = (byte)0xdc;  //CIP path seg
  public static final byte ENGUNITS = (byte)0xdd;  //eng units
  public static final byte STRINGI = (byte)0xde;  //intnational char string
}
