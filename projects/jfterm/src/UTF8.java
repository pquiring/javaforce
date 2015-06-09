/*
 * UTF8.java
 *
 * Created : Apr 1, 2012
 *
 * @author pquiring
 *
 */

public class UTF8 {

  public UTF8() {}

  public char char16;

  public boolean decode(char code[], int codelen, Buffer buffer) {
    if ((code[0] & 0xe0) == 0xc0) {
      if (codelen != 2) return false;
      int utf16 = code[0] & 0x01f;
      utf16 <<= 6;
      utf16 += code[1] & 0x3f;
      char16 = (char)utf16;
      return true;
    }
    if ((code[0] & 0xf0) == 0xe0) {
      if (codelen != 3) return false;
      int utf16 = code[0] & 0x0f;
      utf16 <<= 6;
      utf16 += code[1] & 0x3f;
      utf16 <<= 6;
      utf16 += code[2] & 0x3f;
      char16 = (char)utf16;
      return true;
    }
    return false;  //should not happen
  }

  public boolean isUTF8(char code) {
    if ((code & 0xe0) == 0xc0) return true;  //110xxxxx = 11 bits
    if ((code & 0xf0) == 0xe0) return true;  //1110xxxx = 16 bits
    return false;
  }
}
