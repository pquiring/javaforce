package jfnetboot;

/** Arch conversion functions.
 *
 * @author pquiring
 */

public class Arch {
  public static short toShort(String arch) {
    switch (arch) {
      case "bios": return 0;
      case "x86": return 7;
      case "arm": return 11;
    }
    return -1;
  }

  public static String toString(short arch) {
    switch (arch) {
      case 0: return "bios";
      case 7: return "x86";
      case 11: return "arm";
    }
    return null;
  }
}
