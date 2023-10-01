package javaforce;

/** Unsigned Short
 *
 * @author pquiring
 */

public class UShort {
  public static int MIN_VALUE = 0;
  public static int MAX_VALUE = 0xffff;

  private short value;

  public UShort(short value) {
    this.value = value;
  }

  public short getValue() {
    return value;
  }

  public static short valueOf(String s, int radix) throws NumberFormatException {
    int value = Integer.parseUnsignedInt(s, radix);
    if (value < MIN_VALUE || value > MAX_VALUE) {
      throw new NumberFormatException("UShort:value out of range");
    }
    return (short)value;
  }

  public static short valueOf(String s) {
    return valueOf(s, 10);
  }

  public static String toString(short value, int radix) {
    return Integer.toString(value & 0xffff, radix);
  }

  public static String toString(short value) {
    return toString(value, 10);
  }
}
