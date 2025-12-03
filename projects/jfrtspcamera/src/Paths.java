/** Paths
 *
 * @author pquiring
 */

import javaforce.*;

public class Paths {

  public static String config;

  public static void init() {
    if (JF.isWindows()) {
      config = System.getenv("APPDATA");
    } else {
      config = "/etc";
    }
  }
}
