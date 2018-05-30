package javaforce.controls;

/** Tag Types (0x00 - 0xff)
 *
 * @author pquiring
 */

public class TagType {
  public static final int unknown = 0;
  public static final int bit = 1;
  public static final int int8 = 2;
  public static final int int16 = 3;
  public static final int int32 = 4;
  public static final int int64 = 5;

  public static final int float32 = 8;
  public static final int float64 = 9;

  public static final int char8 = 16;
  public static final int char16 = 17;
  public static final int string = 18;  //UTF-8

  public static final int function = 32;

  public static final int unsigned_mask = 32;
  public static final int uint8 = 34;
  public static final int uint16 = 35;
  public static final int uint32 = 36;
  public static final int uint64 = 37;

  public static final int any = 64;
  public static final int anyint = 65;
  public static final int anyfloat = 66;
  public static final int anyarray = 67;
}
