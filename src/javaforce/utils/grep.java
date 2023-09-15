package javaforce.utils;

/** grep CLI
 *
 * @author pquiring
 */

import javaforce.*;

public class grep {
  public static void main(String[] args) {
    if (args.length != 3) {
      JFLog.log("Usage: grep filein fileout regex");
      return;
    }
    JF.grep(args[0], args[1], args[2]);
  }
}
