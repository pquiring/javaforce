package javaforce;

/** Encoding 4 bits per digit (0-9).
 *
 * Hex values A-F are not used.
 *
 * Old IBM encoding used by Siemens PLCs.
 *
 * @author pquiring
 */

public class EBCDIC {

  /** Encodes byte -> byte[1]
   *
   * @param val = 0-99
   */
  public static byte[] encode(byte val, byte[] out, int offset) {
    if (val < 0 || val > 99) return null;
    char[] ca = String.format("%02d", val).toCharArray();
    byte b;
    b = (byte)((ca[0] - '0') << 4);
    b += (byte)(ca[1] - '0');
    out[offset++] = b;
    return out;
  }

  /** Encodes short -> byte[2]
   *
   * @param val = 0-9999
   */
  public static byte[] encode(short val, byte[] out, int offset) {
    if (val < 0 || val > 9999) return null;
    char[] ca = String.format("%04d", val).toCharArray();
    byte b;
    b = (byte)((ca[0] - '0') << 4);
    b += (byte)(ca[1] - '0');
    out[offset++] = b;
    b = (byte)((ca[2] - '0') << 4);
    b += (byte)(ca[3] - '0');
    out[offset++] = b;
    return out;
  }

  /** Encodes int -> byte[4];
   *
   * @param val = 0-99999999
   */
  public static byte[] encode(int val, byte[] out, int offset) {
    if (val < 0 || val > 99999999) return null;
    char[] ca = String.format("%08d", val).toCharArray();
    byte b;
    b = (byte)((ca[0] - '0') << 4);
    b += (byte)(ca[1] - '0');
    out[offset++] = b;
    b = (byte)((ca[2] - '0') << 4);
    b += (byte)(ca[3] - '0');
    out[offset++] = b;
    b = (byte)((ca[4] - '0') << 4);
    b += (byte)(ca[5] - '0');
    out[offset++] = b;
    b = (byte)((ca[6] - '0') << 4);
    b += (byte)(ca[7] - '0');
    out[offset++] = b;
    return out;
  }

  /** Decodes byte, short or int.
   */
  public static int decode(byte[] in, int offset, int length) {
    if (length < 1 || length > 4) return -1;
    int val = 0;
    int mult = 1;
    int i = offset + length;
    while (i > offset) {
      i--;
      int b = ((in[i] & 0xf0) >> 4) * 10;
      b += in[i] & 0x0f;
      val += b * mult;
      mult *= 100;
    }
    return val;
  }
}
