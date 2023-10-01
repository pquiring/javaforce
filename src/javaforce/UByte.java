package javaforce;

/** Unsigned Byte
 *
 * @author pquiring
 */

public class UByte {
  public static int MIN_VALUE = 0;
  public static int MAX_VALUE = 0xff;

  private byte value;

  public UByte(byte value) {
    this.value = value;
  }

  public byte getValue() {
    return value;
  }

  public static byte valueOf(String s, int radix) throws NumberFormatException {
    int value = Integer.parseUnsignedInt(s, radix);
    if (value < MIN_VALUE || value > MAX_VALUE) {
      throw new NumberFormatException("UByte:value out of range");
    }
    return (byte)value;
  }

  public static byte valueOf(String s) {
    return valueOf(s, 10);
  }

  public static String toString(byte value, int radix) {
    return Integer.toString(value & 0xff, radix);
  }

  public static String toString(byte value) {
    return toString(value, 10);
  }
}
