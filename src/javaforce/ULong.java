package javaforce;

/** Unsigned Long
 *
 * @author pquiring
 */

public class ULong {
  public static final long MIN_VALUE = 0;
  public static final long MAX_VALUE = 0xffffffffffffffffL;

  public static final long MASK = 0xffffffff;

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

  public static void main(String[] args) {
    long val = 0x00;
    for(int a=0;a<16;a++) {
      String str = toString(val, 16);
      long v2 = valueOf(str, 16);
      JFLog.log("int:" + (val & MASK) + " > " + str + " > " + (v2 & MASK));
      val += 0x1111111111111111L;
    }
  }
}
