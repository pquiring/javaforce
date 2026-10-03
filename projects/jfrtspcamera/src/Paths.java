/** Paths
 *
 * @author pquiring
 */

import javaforce.*;

public class Paths {

  public static String config;

  public static void init() {
    config = JF.getConfigPath();
  }
}
