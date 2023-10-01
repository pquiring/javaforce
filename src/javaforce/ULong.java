package javaforce;

/** Unsigned Long
 *
 * @author pquiring
 */

public class ULong {
  public static long MIN_VALUE = 0;
  public static long MAX_VALUE = 0xffffffffffffffffL;

  private long value;

  public ULong(long value) {
    this.value = value;
  }

  public long getValue() {
    return value;
  }

  public static long valueOf(String s, int radix) throws NumberFormatException {
    return Long.parseUnsignedLong(s, radix);
  }

  public static long valueOf(String s) {
    return valueOf(s, 10);
  }

  public static String toString(long value, int radix) {
    return Long.toUnsignedString(value, radix);
  }

  public static String toString(long value) {
    return toString(value, 10);
  }
}
