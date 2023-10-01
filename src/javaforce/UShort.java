package javaforce;

/** Unsigned Short
 *
 * @author pquiring
 */

public class UShort {
  public static final int MIN_VALUE = 0;
  public static final int MAX_VALUE = 0xffff;

  public static final int MASK = 0xffff;

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

  public static void main(String[] args) {
    short val = 0x00;
    for(int a=0;a<16;a++) {
      String str = toString(val, 16);
      short v2 = valueOf(str, 16);
      JFLog.log("short:" + (val & MASK) + " > " + str + " > " + (v2 & MASK));
      val += 0x1111;
    }
  }
}
