package javaforce;

/** Unsigned Byte
 *
 * @author pquiring
 */

public class UByte {
  public static final int MIN_VALUE = 0;
  public static final int MAX_VALUE = 0xff;

  public static final int MASK = 0xff;

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

  public static void main(String[] args) {
    byte val = 0x00;
    for(int a=0;a<16;a++) {
      String str = toString(val, 16);
      byte v2 = valueOf(str, 16);
      JFLog.log("byte:" + (val & MASK) + " > " + str + " > " + (v2 & MASK));
      val += 0x11;
    }
  }
}
