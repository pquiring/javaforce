package jfnetboot;

/** Arch conversion functions.
 *
 * @author pquiring
 */

public class Arch {
  public static int toInt(String arch) {
    switch (arch) {
      case "arm": return 1;
      case "x86": return 2;
    }
    return 0;
  }

  public static String toString(int arch) {
    switch (arch) {
      case 1: return "arm";
      case 2: return "x86";
    }
    return null;
  }
}
