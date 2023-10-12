package javaforce;

/** Unsigned Integer
 *
 * @author pquiring
 */

public class UInteger {
  public static final int MIN_VALUE = 0;
  public static final int MAX_VALUE = 0xffffffff;

  public static final int MASK = 0xffffffff;

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

  public static int compare(int v1, int v2) {
    return Integer.compareUnsigned(v1, v2);
  }

  public static int divide(int v1, int v2) {
    return Integer.divideUnsigned(v1, v2);
  }

  public static int remainder(int v1, int v2) {
    return Integer.remainderUnsigned(v1, v2);
  }

  public static void main(String[] args) {
    int val = 0x00;
    for(int a=0;a<16;a++) {
      String str = toString(val, 16);
      int v2 = valueOf(str, 16);
      JFLog.log("int:" + (val & MASK) + " > " + str + " > " + (v2 & MASK));
      val += 0x11111111;
    }
  }
}
