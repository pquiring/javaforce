package javaforce;

/** Unsigned Integer
 *
 * @author pquiring
 */

public class UInteger {
  public static int MIN_VALUE = 0;
  public static int MAX_VALUE = 0xffffffff;

  private int value;

  public UInteger(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static int valueOf(String s, int radix) throws NumberFormatException {
    return Integer.parseUnsignedInt(s, radix);
  }

  public static int valueOf(String s) {
    return valueOf(s, 10);
  }

  public static String toString(int value, int radix) {
    return Integer.toUnsignedString(value, radix);
  }

  public static String toString(int value) {
    return toString(value, 10);
  }
}
