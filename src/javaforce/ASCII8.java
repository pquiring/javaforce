package javaforce;

/** Convert ASCII-8bit to Unicode
 *
 * Converts codes 128-255
 * Used by jfTerm and Hex editors (hex , hexbig)
 *
 * @author pquiring
 *
 * Created : Jan 15, 2014
 */

public class ASCII8 {
  //java does not have any 8bit ascii, so I had to create a lookup table
  //used http://www.rapidmonkey.com/unicodeconverter/ to convert codes
  private static char[] table = (
    "\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7\u00EA\u00EB\u00E8\u00EF\u00EE\u00EC\u00C4\u00C5" +
    "\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2\u00FB\u00F9\u00FF\u00D6\u00DC\u00A2\u00A3\u00A5\u20A7\u0192" +
    "\u00E1\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA\u00BF\u2310\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB" +
    "\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255D\u255C\u255B\u2510" +
    "\u2514\u2534\u252C\u251C\u2500\u253C\u255E\u255F\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u2567" +
    "\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256B\u256A\u2518\u250C\u2588\u2584\u258C\u2590\u2580" +
    "\u03B1\u00DF\u0393\u03C0\u03A3\u03C3\u00B5\u03C4\u03A6\u0398\u03A9\u03B4\u221E\u03C6\u03B5\u2229" +
    "\u2261\u00B1\u2265\u2264\u2320\u2321\u00F7\u2248\u00B0\u2219\u00B7\u221A\u207F\u00B2\u25A0\u00A0"
  ).toCharArray();

  /** Convert ASCII8 to UTF16 */
  public static char convert(char ascii) {
    if (ascii < 128) return ascii;
    if (ascii > 255) return ascii;
    return table[ascii-128];
  }
  /** Convert ASCII8 to UTF16 */
  public static char convert(int ascii) {
    return convert((char)ascii);
  }
  /** Convert UTF16 to ASCII8 */
  public static char convertUTF16(int utf16) {
    if (utf16 < 128) return (char)utf16;
    for(int a=0;a<table.length;a++) {
      if (table[a] == utf16) {
        return (char)(a + 128);
      }
    }
    return (char)-1;
  }
  public static void main(String args[]) {
    System.out.println("ASCII=" + convert(176) + convert(177) + convert(178) + convert(219));
  }
}
