package javaforce;

/** Base16 (hex) encoder / decoder
 *
 * Hex Chars : 0-9,a-f  (lower case)
 *
 * @author pquiring
 */

public class Base16 {
  /** Encode base16.
  * in = byte array
  * out = char array
  */
  public static byte[] encode(byte[] in) {
    int len = in.length;
    byte[] out = new byte[len * 2];
    int out_pos = 0;
    for(int in_pos=0;in_pos<len;in_pos++) {
      byte hi = (byte)((in[in_pos] & 0xf0) >> 4);
      if (hi > 9) hi += 'a'; else hi += '0';
      byte lo = (byte)(in[in_pos] & 0xf);
      if (lo > 9) lo += 'a'; else lo += '0';
      out[out_pos++] = hi;
      out[out_pos++] = lo;
    }
    return out;
  }

  //0-9 = 0x30-0x39
  //A-F = 0x41-0x46
  //a-f = 0x61-0x66
  private static final byte to_lower_case = ('a' - 'A');  //0x20

  /** Decode base16.
  * in = char array
  * out = byte array
  */
  public static byte[] decode(byte[] in) {
    int len = in.length / 2;
    byte[] out = new byte[len];
    int in_pos = 0;
    for(int out_pos=0;out_pos<len;out_pos++) {
      byte hi = (byte)in[in_pos++];
      if (hi >= 'A' && hi <= 'F') hi += to_lower_case;
      if (hi >= 'a') hi -= 'a'; else hi -= '0';
      byte lo = (byte)in[in_pos++];
      if (lo >= 'A' && lo <= 'F') lo += to_lower_case;
      if (lo >= 'a') lo -= 'a'; else lo -= '0';
      out[out_pos] = (byte)((hi << 4) + lo);
    }
    return out;
  }
}
