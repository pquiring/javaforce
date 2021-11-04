package javaforce.controls.ab;

/**
 *
 * @author pquiring
 */

public class ABTypes {
  public static final byte BOOL = (byte)0xc1;
  public static final byte SINT = (byte)0xc2;  //byte
  public static final byte INT = (byte)0xc3;  //short
  public static final byte DINT = (byte)0xc4;  //int
  public static final byte LINT = (byte)0xc5;  //long
  public static final byte USINT = (byte)0xc6;  //unsigned byte
  public static final byte UINT = (byte)0xc7;  //unsigned short
  public static final byte UDINT = (byte)0xc8;  //unsigned int
  public static final byte LWORD = (byte)0xc9;  //long
  public static final byte REAL = (byte)0xca;  //float
  public static final byte LREAL = (byte)0xcb;  //double
  public static final byte DWORD = (byte)0xd3;  //32bit boolean array
  public static final byte STRING = (byte)0xda;
}
